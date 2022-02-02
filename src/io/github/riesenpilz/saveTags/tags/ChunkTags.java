package io.github.riesenpilz.saveTags.tags;

import javax.annotation.Nullable;

import org.bukkit.Chunk;

import com.google.gson.JsonObject;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;
import lombok.Data;
import lombok.NonNull;

@Data
public class ChunkTags {
	@NonNull
	private final Chunk chunk;

	public static ChunkTags getTags(Chunk chunk) {
		return new ChunkTags(chunk);
	}

	public boolean hasTags() {
		return SaveTags.getAllTags(chunk).hasJsonObject("chunkTags");
	}

	public JsonObjectWrapper getTags() {
		return SaveTags.getAllTags(chunk).getJsonObjectOrDef("chunkTags", new JsonObject());
	}

	public void setTags(@Nullable JsonObject jsonObject) {
		SaveTags.getAllTags(chunk).add("chunkTags", jsonObject);
	}
}
