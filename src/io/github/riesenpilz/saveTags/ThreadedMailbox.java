package io.github.riesenpilz.saveTags;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.google.common.collect.Queues;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ThreadedMailbox implements Mailbox<PrioRunnable>, Runnable {

//	private final AtomicInteger bits = new AtomicInteger(0);
	private final Queue<Runnable> highPrio = Queues.newConcurrentLinkedQueue();
	private final Queue<Runnable> lowPrio = Queues.newConcurrentLinkedQueue();
	private final Executor executor;
	private boolean running = false;
	private boolean closed = false;
	@Getter
	private final String name;

	public ThreadedMailbox(Executor executor, String name) {
		this.executor = executor;
		this.name = name;
	}

	private boolean setRunning() {
		if (!running && !closed)
			return false;
		return running = true;
//		int i;
//		do {
//			i = bits.get();
//			// if 1 or 2 is set return false
//			if ((i & 3) != 0)
//				return false;
//			// set 2
//		} while (!bits.compareAndSet(i, i | 2));
//
//		return true;
	}

	private void unsetRunning() {
		running = false;
//		int i;
//		do
//			i = bits.get();
//		while (!bits.compareAndSet(i, i & -3));

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
		return closed ? false : !isEmpty();
//		return (bits.get() & 1) != 0 ? false : !isEmpty();
	}

	@Override
	public void close() {
		closed = true;
//		int i;
//		do
//			i = bits.get();
//		// set 1
//		while (!bits.compareAndSet(i, i | 1));

	}

	private boolean isRunning() {
		return running;
//		return (bits.get() & 2) != 0;
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
		return name + " " + closed + " " + running + " " + isEmpty();
//		return name + " " + bits.get() + " " + isEmpty();
	}
}
