package io.github.riesenpilz.saveTags;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CompletableNBTTag {
	@NonNull JsonObject json;
	final CompletableFuture<?> completableFuture = new CompletableFuture<>();
}

