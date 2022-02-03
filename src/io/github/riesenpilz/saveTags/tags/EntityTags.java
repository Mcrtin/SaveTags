package io.github.riesenpilz.saveTags.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;
import lombok.NonNull;

public class EntityTags extends JsonObjectWrapper {
	public static final Map<Entity, JsonObjectWrapper> entityTags = new HashMap<>();
	@NonNull
	private final Entity entity;
	private boolean exists;

	private EntityTags(Entity entity) {
		super(entityTags.get(entity));
		this.entity = entity;
		exists = entityTags.containsKey(entity);
	}

	private String getId() {
		return entity.getUniqueId().toString();
	}

	public static EntityTags of(Entity entity) {
		return new EntityTags(entity);
	}

	static JsonObjectWrapper save(Entity[] entities) {
		JsonObjectWrapper jsonObject = new JsonObjectWrapper();
		for (Entity entity : entities) {
			EntityTags entityTags = of(entity);
			if (entityTags.hasTags()) {
				jsonObject.add(entityTags.getId(), entityTags);
				entityTags.remove();
			}
		}
		return jsonObject;
	}

	static void load(JsonObjectWrapper jsonObject) {
		for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			EntityTags.entityTags.put(Bukkit.getEntity(UUID.fromString(entry.getKey())),
					new JsonObjectWrapper((JsonObject) entry.getValue()));
		}
	}

	public static void checkEntities() {
		Entity[] entities = (Entity[]) EntityTags.entityTags.keySet().toArray();
		for (Entity entity : entities)
			if (!entity.isValid())
				EntityTags.entityTags.remove(entity);
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
		entityTags.put(entity, this);
	}

	public void remove() {
		if (!exists)
			return;
		exists = false;
		entityTags.remove(entity);
	}
}
