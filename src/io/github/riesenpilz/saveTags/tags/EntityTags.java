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
public class EntityTags implements Tagable {
	public static final Map<Entity, JsonObjectWrapper> entityTags = new HashMap<>();
	@NonNull
	private final Entity entity;

	private EntityTags(Entity entity) {
		this.entity = entity;
	}

	private String getId() {
		return entity.getUniqueId().toString();
	}

	public static EntityTags of(Entity entity) {
		return new EntityTags(entity);
	}

	@Override
	public boolean hasTags() {
		return entityTags.containsKey(entity);
	}

	@Override
	public JsonObjectWrapper getTags() {
		return entityTags.getOrDefault(entity, new JsonObjectWrapper());
	}

	@Override
	public void setTags(@Nullable JsonObjectWrapper jsonObject) {
		if (jsonObject == null)
			entityTags.remove(entity);
		else
			entityTags.put(entity, jsonObject);
	}

	static JsonObjectWrapper save(Entity[] entities) {
		JsonObjectWrapper jsonObject = new JsonObjectWrapper();
		for (Entity entity : entities) {
			EntityTags entityTags = of(entity);
			if (entityTags.hasTags()) {
				jsonObject.add(entityTags.getId(), entityTags.getTags());
				entityTags.removeTags();
			}
		}
		return jsonObject;
	}

	static void load(JsonObjectWrapper jsonObject) {
		for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			EntityTags entityTags = of(Bukkit.getEntity(UUID.fromString(entry.getKey())));
			entityTags.setTags(new JsonObjectWrapper((JsonObject) entry.getValue()));
		}
	}

	public static void checkEntities() {
		Entity[] entities = (Entity[]) EntityTags.entityTags.keySet().toArray();
		for (Entity entity : entities)
			if (!entity.isValid())
				EntityTags.entityTags.remove(entity);
	}
}
