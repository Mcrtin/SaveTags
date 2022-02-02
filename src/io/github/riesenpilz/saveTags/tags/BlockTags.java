package io.github.riesenpilz.saveTags.tags;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.block.Block;

import com.google.gson.JsonObject;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;
import lombok.Data;
import lombok.NonNull;

@Data
public class BlockTags {
	@NonNull
	private final Location location;

	public static BlockTags getTags(Block block) {
		return new BlockTags(block.getLocation());
	}

	private String getId() {
		return (location.getBlockX() % 16) + ":" + (location.getBlockY() % 16) + ":" + (location.getBlockZ() % 16);
	}

	public boolean hasTags() {
		return getAllBlockTags().hasJsonObject(getId());
	}

	private JsonObjectWrapper getAllBlockTags() {
		return SaveTags.getAllTags(location.getChunk()).getJsonObjectOrDef("blockTags", new JsonObject());
	}

	public JsonObjectWrapper getTags() {
		return getAllBlockTags().getJsonObjectOrDef(getId(), new JsonObject());
	}

	public void setTags(@Nullable JsonObject jsonObject) {
		SaveTags.getAllTags(location.getChunk()).getJsonObject("blockTags").add(getId(), jsonObject);
	}
}
