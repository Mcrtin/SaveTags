package io.github.martin.saveTags.storage;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class IOWorker implements AutoCloseable {
	private final AtomicBoolean closed = new AtomicBoolean();
	private final ThreadedMailbox mailbox;
	private final RegionFileCache cache;
	private final Map<ChunkCoords, CompletableJson> chunkData = Maps.newLinkedHashMap();

	public IOWorker(File dir, boolean sync, String name) {
		this.cache = new RegionFileCache(dir, sync);
		this.mailbox = new ThreadedMailbox(name);
	}

	public CompletableFuture<?> write(ChunkCoords chunk, JsonObject json) {
		return internal(() -> {
			CompletableJson cJson = chunkData.computeIfAbsent(chunk, unused -> new CompletableJson(json));
			cJson.json = json;
			return Either.left(cJson.completableFuture);
		}).thenCompose(Function.identity());
	}

	@Nullable
	public JsonObject read(ChunkCoords chunk) throws IOException {
		CompletableFuture<?> cFuture = internal(() -> {
			CompletableJson cJson = chunkData.get(chunk);
			if (cJson != null)
				return Either.left(cJson.json);
			try {
				JsonObject json = cache.read(chunk);

				return Either.left(json);
			} catch (Exception ex) {
				IOWorker.log.warn("Failed to read chunk {}", chunk, ex);
				return Either.right(ex);
			}
		});
		return (JsonObject) join(cFuture);
	}

	public CompletableFuture<?> syncChunks() {
		CompletableFuture<?> cFuture = internal(() -> {
			return Either.left(CompletableFuture.allOf(chunkData.values().stream()
					.map((cJson) -> cJson.completableFuture).toArray(CompletableFuture[]::new)));
		}).thenCompose(Function.identity());

		return cFuture.thenCompose(unnused -> internal(() -> {
			try {
				cache.close2();
				return Either.left(null);
			} catch (Exception exception) {
				IOWorker.log.warn("Failed to synchronized chunks", exception);
				return Either.right(exception);
			}
		}));
	}

	private <T> CompletableFuture<T> internal(Supplier<Either<T, Exception>> supplier) {
		return mailbox.sendEither(mailbox -> new PrioRunnable(true, () -> {
			if (!closed.get())
				mailbox.setMessage(supplier.get());
			IOWorker.this.mailbox.setMessage(new PrioRunnable(false, this::internalWrite2));
		}));
	}

	private void internalWrite2() {
		Iterator<Entry<ChunkCoords, CompletableJson>> iterator = chunkData.entrySet().iterator();

		if (iterator.hasNext()) {
			Entry<ChunkCoords, CompletableJson> entry = iterator.next();

			iterator.remove();
			internalWrite1(entry.getKey(), entry.getValue());
			mailbox.setMessage(new PrioRunnable(false, this::internalWrite2));
		}
	}

	private void internalWrite1(ChunkCoords chunk, CompletableJson cJson) {
		try {
			cache.write(chunk, cJson.json);
			cJson.completableFuture.complete(null);
		} catch (Exception exception) {
			log.error("Failed to store chunk {}", chunk, exception);
			cJson.completableFuture.completeExceptionally(exception);
		}

	}

	public void close() throws IOException {
		if (closed.compareAndSet(false, true)) {

			join(mailbox.send(mailbox -> new PrioRunnable(true, () -> mailbox.setMessage(Unit.INSTANCE))));

			mailbox.close();
			chunkData.forEach(this::internalWrite1);
			chunkData.clear();

			try {
				cache.close();
			} catch (Exception ex) {
				IOWorker.log.error("Failed to close storage", ex);
			}

		}
	}

	private static <T> T join(CompletableFuture<T> c) throws IOException {
		try {
			return c.join();
		} catch (CompletionException cex) {
			if (cex.getCause() instanceof IOException)
				throw (IOException) cex.getCause();
			throw cex;
		}
	}

	private static enum Unit {
		INSTANCE
	}
}
