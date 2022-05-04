package io.github.riesenpilz.saveTags.storage;

import java.util.Queue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.google.common.collect.Queues;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ThreadedMailbox implements Mailbox<PrioRunnable>, Runnable {
	private static final AtomicInteger ioWorkerCounter = new AtomicInteger(1);
	private static final Executor executor = Executors.newCachedThreadPool((runnable) -> {
		Thread thread = new Thread(runnable);
		thread.setName("IO-Worker-" + ioWorkerCounter.getAndIncrement());
		thread.setUncaughtExceptionHandler((thread1, throwable) -> {
			if (throwable instanceof CompletionException)
				throwable = throwable.getCause();
			log.error(String.format("Caught exception in thread %s", thread1), throwable);
		});
		return thread;
	});
	private final AtomicInteger bits = new AtomicInteger(0);
	private final Queue<Runnable> highPrio = Queues.newConcurrentLinkedQueue();
	private final Queue<Runnable> lowPrio = Queues.newConcurrentLinkedQueue();
	@Getter
	private final String name;

	public ThreadedMailbox(String name) {
		this.name = "IOWorker-" + name;
	}

	private boolean setRunning() {
		int i;
		do {
			i = bits.get();
			if ((i & 3) != 0)
				return false;
		} while (!bits.compareAndSet(i, i | 2));

		return true;
	}

	private void unsetRunning() {
		int i;
		do
			i = bits.get();
		while (!bits.compareAndSet(i, i & -3));

	}

	private boolean isEmpty() {
		return highPrio.isEmpty() && lowPrio.isEmpty();
	}

	@Nullable
	private Runnable poll() {
		Runnable runnable = highPrio.poll();
		if (runnable == null)
			runnable = lowPrio.poll();
		return runnable;
	}

	private boolean isClosedAndNotEmpty() {
		return (bits.get() & 1) != 0 ? false : !isEmpty();
	}

	@Override
	public void close() {
		int i;
		do
			i = bits.get();
		while (!bits.compareAndSet(i, i | 1));

	}

	private boolean isRunning() {
		return (bits.get() & 2) != 0;
	}

	private boolean execute() {
		if (!isRunning())
			return false;
		Runnable runnable = poll();

		if (runnable == null)
			return false;
		Thread thread = Thread.currentThread();
		String threadName = thread.getName();
		thread.setName(name);
		runnable.run();
		thread.setName(threadName);

		return true;
	}

	public void run() {
		try {
			tryExecute();
		} finally {
			unsetRunning();
			schedule();
		}

	}

	@Override
	public void setMessage(PrioRunnable runnable) {
		if (runnable.high)
			highPrio.add(runnable);
		else
			lowPrio.add(runnable);
		schedule();
	}

	private void schedule() {
		if (isClosedAndNotEmpty() && setRunning())
			try {
				executor.execute(this);
			} catch (RejectedExecutionException reex) {
				try {
					executor.execute(this);
				} catch (RejectedExecutionException reex1) {
					ThreadedMailbox.log.error("Cound not schedule mailbox", reex1);
				}
			}

	}

	private void tryExecute() {
		for (int i = 0; i == 0 && execute(); ++i)
			;
	}

	public String toString() {
		return name + " " + bits.get() + " " + isEmpty();
	}
}
