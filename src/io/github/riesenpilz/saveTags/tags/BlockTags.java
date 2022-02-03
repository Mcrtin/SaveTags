package io.github.riesenpilz.saveTags.tags;

import org.bukkit.Location;
import org.bukkit.block.Block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;
import lombok.NonNull;

public class BlockTags extends JsonObjectWrapper {
	@NonNull
	private final Location location;
	private boolean exists;

	private BlockTags(@NonNull Location location) {
		super(getAllBlockTags(location).getJsonObjectOrDef(getId(location), null));
		this.location = location;
		exists = getAllBlockTags(location).hasJsonObject(getId(location));
	}

	public static BlockTags of(@NonNull Block block) {
		return new BlockTags(block.getLocation());
	}

	private static JsonObjectWrapper getAllBlockTags(Location location) {
		return SaveTags.getAllTags(location.getChunk()).getJsonObject("blockTags");
	}

	private static String getId(Location location) {
		return (location.getBlockX() % 16) + ":" + (location.getBlockY() % 16) + ":" + (location.getBlockZ() % 16);
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
		getAllBlockTags(location).add(getId(location), this);
	}

	public void remove() {
		if (!exists)
			return;
		exists = false;
		getAllBlockTags(location).remove(getId(location));
	}

}
