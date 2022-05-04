package io.github.martin.saveTags.tags;

import org.bukkit.Chunk;

import lombok.NonNull;

public class ChunkTags extends Tag {
	@NonNull
	private final Chunk chunk;

	private ChunkTags(@NonNull Chunk chunk) {
		super(SaveTags.getAllTags(chunk).getJsonObjectOrDef("chunkTags", null),
				SaveTags.getAllTags(chunk).hasJsonObject("chunkTags"));
		this.chunk = chunk;
	}

	public static ChunkTags of(@NonNull Chunk chunk) {
		return new ChunkTags(chunk);
	}

	@Override
	protected void addThis() {
		SaveTags.getAllTags(chunk).add("chunkTags", this);

	}

	@Override
	protected void removeThis() {
		SaveTags.getAllTags(chunk).remove("chunkTags");

	}

}
