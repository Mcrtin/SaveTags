package io.github.riesenpilz.saveTags;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;

public class IOWorker implements AutoCloseable {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final AtomicInteger ioWorkerCounter = new AtomicInteger(1);
	private static final Executor executor = Executors.newCachedThreadPool((runnable) -> {
		Thread thread = new Thread(runnable);
		thread.setName("IO-Worker-" + ioWorkerCounter.getAndIncrement());
		thread.setUncaughtExceptionHandler((thread1, throwable) -> {
			if (throwable instanceof CompletionException)
				throwable = throwable.getCause();
			LOGGER.error(String.format("Caught exception in thread %s", thread1), throwable);
		});
		return thread;
	});
	private final AtomicBoolean b = new AtomicBoolean();
	private final ThreadedMailbox c;
	private final RegionFileCache d;
	private final Map<ChunkCoords, CompletableNBTTag> e = Maps.newLinkedHashMap();

	public IOWorker(File file, boolean flag, String s) {
		this.d = new RegionFileCache(file, flag);
		this.c = new ThreadedMailbox(executor, "IOWorker-" + s);
	}

	public CompletableFuture<?> write(ChunkCoords chunkcoordintpair, JsonObject nbttagcompound) {
		return this.internalWrite4(() -> {
			CompletableNBTTag ioworker_a = this.e.computeIfAbsent(chunkcoordintpair,
					(chunkcoordintpair1) -> new CompletableNBTTag(nbttagcompound));

			ioworker_a.json = nbttagcompound;
			return Either.left(ioworker_a.completableFuture);
		}).thenCompose(Function.identity());
	}

	@Nullable
	public JsonObject read(ChunkCoords chunkcoordintpair) throws IOException {
		CompletableFuture<?> completablefuture = this.internalWrite4(() -> {
			CompletableNBTTag ioworker_a = this.e.get(chunkcoordintpair);

			if (ioworker_a != null) {
				return Either.left(ioworker_a.json);
			}
			try {
				JsonObject nbttagcompound = this.d.read(chunkcoordintpair);

				return Either.left(nbttagcompound);
			} catch (Exception exception) {
				IOWorker.LOGGER.warn("Failed to read chunk {}", chunkcoordintpair, exception);
				return Either.right(exception);
			}
		});

		try {
			return (JsonObject) completablefuture.join();
		} catch (CompletionException completionexception) {
			if (completionexception.getCause() instanceof IOException) {
				throw (IOException) completionexception.getCause();
			}
			throw completionexception;
		}
	}

	public CompletableFuture<Void> syncChunks() {
		CompletableFuture<Void> completablefuture = this.internalWrite4(() -> {
			return Either.left(CompletableFuture.allOf(this.e.values().stream().map((ioworker_a) -> {
				return ioworker_a.completableFuture;
			}).toArray((i) -> {
				return new CompletableFuture[i];
			})));
		}).thenCompose(Function.identity());

		return completablefuture.thenCompose((ovoid) -> {
			return this.internalWrite4(() -> {
				try {
					this.d.a();
					return Either.left(null);
				} catch (Exception exception) {
					IOWorker.LOGGER.warn("Failed to synchronized chunks", exception);
					return Either.right(exception);
				}
			});
		});
	}

	private <T> CompletableFuture<T> internalWrite4(Supplier<Either<T, Exception>> supplier) {
		return this.c.c((mailbox) -> {
			return new PrioRunnable(true, () -> {
				if (!this.b.get()) {
					mailbox.setMessage(supplier.get());
				}

				this.internalWrite3();
			});
		});
	}

	private void internalWrite2() {
		Iterator<Entry<ChunkCoords, CompletableNBTTag>> iterator = this.e.entrySet().iterator();

		if (iterator.hasNext()) {
			Entry<ChunkCoords, CompletableNBTTag> entry = iterator.next();

			iterator.remove();
			this.internalWrite1(entry.getKey(), entry.getValue());
			this.internalWrite3();
		}
	}

	private void internalWrite3() {
		this.c.setMessage(new PrioRunnable(false, this::internalWrite2));
	}

	private void internalWrite1(ChunkCoords chunkcoordintpair, CompletableNBTTag ioworker_a) {
		try {
			this.d.write(chunkcoordintpair, ioworker_a.json);
			ioworker_a.completableFuture.complete(null);
		} catch (Exception exception) {
			IOWorker.LOGGER.error("Failed to store chunk {}", chunkcoordintpair, exception);
			ioworker_a.completableFuture.completeExceptionally(exception);
		}

	}

	public void close() throws IOException {
		if (this.b.compareAndSet(false, true)) {

			CompletableFuture<?> completablefuture = this.c.b((mailbox) -> {
				return new PrioRunnable(true, () -> {
					mailbox.setMessage(Unit.INSTANCE);
				});
			});
			try {
				completablefuture.join();
			} catch (CompletionException completionexception) {
				if (completionexception.getCause() instanceof IOException) {
					throw (IOException) completionexception.getCause();
				}

				throw completionexception;
			}

			this.c.close();
			this.e.forEach(this::internalWrite1);
			this.e.clear();

			try {
				this.d.close();
			} catch (Exception exception) {
				IOWorker.LOGGER.error("Failed to close storage", exception);
			}

		}
	}

	static enum Unit {

		INSTANCE;

	}
}
