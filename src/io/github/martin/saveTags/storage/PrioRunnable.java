package io.github.martin.saveTags.storage;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;

@AllArgsConstructor
class PrioRunnable implements Runnable {
	final boolean high;
	@Delegate
	private final Runnable runnable;
}