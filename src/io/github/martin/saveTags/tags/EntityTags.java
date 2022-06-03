package io.github.martin.saveTags.tags;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.martin.saveTags.json.JsonObjectWrapper;
import lombok.NonNull;

public class EntityTags extends Tag {
	public static final Map<Entity, JsonObjectWrapper> entityTags = new HashMap<>();
	@NonNull
	private final Entity entity;

	private EntityTags(Entity entity) {
		super(entityTags.get(entity), entityTags.containsKey(entity));
		this.entity = entity;
	}

	private String getId() {
		return entity.getUniqueId().toString();
	}

	public static EntityTags of(Entity entity) {
		return new EntityTags(entity);
	}

	static JsonObjectWrapper save(Entity[] entities) {
		JsonObjectWrapper jsonObject = new JsonObjectWrapper();
		Arrays.stream(entities).map(EntityTags::of).filter(entityTags -> entityTags.hasTags()).forEach(entityTags -> {
			jsonObject.add(entityTags.getId(), entityTags);
			entityTags.remove();
		});
		return jsonObject;
	}

	static void load(JsonObjectWrapper jsonObject) {
		for (Entry<String, JsonElement> entry : jsonObject.entrySet())
			EntityTags.entityTags.put(Bukkit.getEntity(UUID.fromString(entry.getKey())),
					new JsonObjectWrapper((JsonObject) entry.getValue()));
	}

	public static void checkEntities() {
		EntityTags.entityTags.keySet().stream().filter(entity -> !entity.isValid())
				.forEach(entity -> EntityTags.entityTags.remove(entity));
	}

	@Override
	protected void addThis() {
		entityTags.put(entity, this);
	}

	@Override
	protected void removeThis() {
		entityTags.remove(entity);
	}
}
