package io.github.riesenpilz.saveTags.tags;

import org.bukkit.Location;
import org.bukkit.block.Block;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;
import lombok.NonNull;

public class BlockTags extends Tag {
	@NonNull
	private final Location location;

	private BlockTags(@NonNull Location location) {
		super(getAllBlockTags(location).getJsonObjectOrDef(getId(location), null),
				getAllBlockTags(location).hasJsonObject(getId(location)));
		this.location = location;
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

	@Override
	protected void addThis() {
		getAllBlockTags(location).add(getId(location), this);
	}

	@Override
	protected void removeThis() {
		getAllBlockTags(location).remove(getId(location));
	}

}
