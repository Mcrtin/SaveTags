package io.github.riesenpilz.saveTags.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;
import lombok.Data;
import lombok.NonNull;

@Data
public class EntityTags {
	public static final Map<Entity, JsonObject> entityTags = new HashMap<>();
	@NonNull
	private final Entity entity;

	private String getId() {
		return entity.getUniqueId().toString();
	}

	public static EntityTags get(Entity entity) {
		return new EntityTags(entity);
	}

	public boolean hasTags() {
		return entityTags.containsKey(entity);
	}

	public JsonObjectWrapper getTags() {
		return new JsonObjectWrapper(entityTags.getOrDefault(entity, new JsonObject()));
	}

	public void setTags(@Nullable JsonObject jsonObject) {
		if (jsonObject == null)
			entityTags.remove(entity);
		else
			entityTags.put(entity, jsonObject);
	}

	static JsonObjectWrapper save(Entity[] entities) {
		JsonObjectWrapper jsonObject = new JsonObjectWrapper();
		for (Entity entity : entities) {
			EntityTags entityTags = get(entity);
			if (entityTags.hasTags()) {
				jsonObject.add(entityTags.getId(), entityTags.getTags());
				entityTags.setTags(null);
			}
		}
		return jsonObject;
	}

	static void load(JsonObjectWrapper jsonObject) {
		for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			EntityTags entityTags = get(Bukkit.getEntity(UUID.fromString(entry.getKey())));
			entityTags.setTags((JsonObject) entry.getValue());
		}
	}
}
