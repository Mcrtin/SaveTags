package io.github.riesenpilz.saveTags.storage;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CompletableJson {
	@NonNull JsonObject json;
	final CompletableFuture<?> completableFuture = new CompletableFuture<>();
}

