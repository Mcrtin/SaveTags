package io.github.riesenpilz.saveTags.tags;

import javax.annotation.Nullable;

import org.bukkit.Chunk;

import com.google.gson.JsonObject;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;
import lombok.Data;
import lombok.NonNull;

@Data
public class ChunkTags implements Tagable {
	@NonNull
	private final Chunk chunk;

	private ChunkTags(@NonNull Chunk chunk) {
		this.chunk = chunk;
	}

	public static ChunkTags of(@NonNull Chunk chunk) {
		return new ChunkTags(chunk);
	}

	@Override
	public boolean hasTags() {
		return SaveTags.getAllTags(chunk).hasJsonObject("chunkTags");
	}

	@Override
	public JsonObjectWrapper getTags() {
		return SaveTags.getAllTags(chunk).getJsonObjectOrDef("chunkTags", new JsonObject());
	}

	@Override
	public void setTags(@Nullable JsonObject jsonObject) {
		SaveTags.getAllTags(chunk).add("chunkTags", jsonObject);
	}

}
