package io.github.riesenpilz.saveTags;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2BooleanFunction;

import com.google.common.collect.Queues;

public class ThreadedMailbox implements Mailbox<PrioRunnable>, Runnable {

	private static final Logger LOGGER = LogManager.getLogger();
	private final AtomicInteger c = new AtomicInteger(0);
	private final Queue<Runnable> highPrio = Queues.newConcurrentLinkedQueue();
	private final Queue<Runnable> lowPrio = Queues.newConcurrentLinkedQueue();
	private final Executor d;
	private final String e;

	public static ThreadedMailbox a(Executor executor, String s) {
		return new ThreadedMailbox(executor, s);
	}

	public ThreadedMailbox(Executor executor, String s) {
		this.d = executor;
		this.e = s;
	}

	private boolean a() {
		int i;

		do {
			i = this.c.get();
			if ((i & 3) != 0) {
				return false;
			}
		} while (!this.c.compareAndSet(i, i | 2));

		return true;
	}

	private void b() {
		int i;

		do {
			i = this.c.get();
		} while (!this.c.compareAndSet(i, i & -3));

	}

	private boolean isEnpty() {
		return highPrio.isEmpty() && lowPrio.isEmpty();
	}

	@Nullable
	private Runnable poll() {
		Runnable runnable = highPrio.poll();
		if (runnable == null)
			runnable = lowPrio.poll();
		return runnable;
	}

	private boolean c() {
		return (this.c.get() & 1) != 0 ? false : !isEnpty();
	}

	@Override
	public void close() {
		int i;

		do {
			i = this.c.get();
		} while (!this.c.compareAndSet(i, i | 1));

	}

	private boolean d() {
		return (this.c.get() & 2) != 0;
	}

	private boolean e() {
		if (!this.d()) {
			return false;
		}
		Runnable runnable = poll();

		if (runnable == null) {
			return false;
		}
		Thread thread;
		String s;

		thread = Thread.currentThread();
		s = thread.getName();
		thread.setName(this.e);

		runnable.run();
		if (thread != null) {
			thread.setName(s);
		}

		return true;
	}

	public void run() {
		try {
			this.a((i) -> {
				return i == 0;
			});
		} finally {
			this.b();
			this.f();
		}

	}

	@Override
	public void setMessage(PrioRunnable runnable) {
		if (runnable.high)
			highPrio.add(runnable);
		else
			lowPrio.add(runnable);
		this.f();
	}

	private void f() {
		if (this.c() && this.a()) {
			try {
				this.d.execute(this);
			} catch (RejectedExecutionException rejectedexecutionexception) {
				try {
					this.d.execute(this);
				} catch (RejectedExecutionException rejectedexecutionexception1) {
					ThreadedMailbox.LOGGER.error("Cound not schedule mailbox", rejectedexecutionexception1);
				}
			}
		}

	}

	private int a(Int2BooleanFunction int2booleanfunction) {
		int i;

		for (i = 0; int2booleanfunction.get(i) && this.e(); ++i) {
			;
		}

		return i;
	}

	public String toString() {
		return this.e + " " + this.c.get() + " " + isEnpty();
	}

	@Override
	public String getName() {
		return this.e;
	}
}
