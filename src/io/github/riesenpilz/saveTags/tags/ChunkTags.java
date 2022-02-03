package io.github.riesenpilz.saveTags.tags;

import org.bukkit.Chunk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;
import lombok.NonNull;

public class ChunkTags extends JsonObjectWrapper {
	@NonNull
	private final Chunk chunk;
	private boolean exists;

	private ChunkTags(@NonNull Chunk chunk) {
		super(SaveTags.getAllTags(chunk).getJsonObjectOrDef("chunkTags", null));
		this.chunk = chunk;
		exists = SaveTags.getAllTags(chunk).hasJsonObject("chunkTags");
	}

	public static ChunkTags of(@NonNull Chunk chunk) {
		return new ChunkTags(chunk);
	}

	public boolean hasTags() {
		return exists;
	}

	@Override
	public void add(String property, boolean value) {
		add();
		super.add(property, value);
	}

	@Override
	public void add(String property, char value) {
		add();
		super.add(property, value);
	}

	@Override
	public void add(String property, String value) {
		add();
		super.add(property, value);
	}

	@Override
	public void add(String property, Number value) {
		add();
		super.add(property, value);
	}

	@Override
	public void add(String property, JsonElement value) {
		add();
		super.add(property, value);
	}

	@Override
	public void add(String property, JsonObjectWrapper value) {
		add();
		super.add(property, value);
	}

	@Override
	public JsonObjectWrapper getJsonObject(String memberName) {
		add();
		return super.getJsonObject(memberName);
	}

	@Override
	public JsonArray getJsonArray(String memberName) {
		add();
		return super.getJsonArray(memberName);
	}

	private void add() {
		if (exists)
			return;
		exists = true;
		SaveTags.getAllTags(chunk).add("chunkTags", this);
	}

	public void remove() {
		if (!exists)
			return;
		exists = false;
		SaveTags.getAllTags(chunk).remove("chunkTags");
	}

}
