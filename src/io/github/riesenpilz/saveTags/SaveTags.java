package io.github.riesenpilz.saveTags;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SaveTags extends JavaPlugin implements Listener {
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	public void onDisable() {
		for (Entry<Chunk, JsonObject> entry : tags.entrySet())
			getWorker(entry.getKey().getWorld()).write(new ChunkCoords(entry.getKey()), entry.getValue());
		for (IOWorker worker : workers.values())
			try {
				worker.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private static final Map<World, IOWorker> workers = new HashMap<>();
	public static final Map<Chunk, JsonObject> tags = new HashMap<>();
	public static final Map<Entity, JsonObject> entityTags = new HashMap<>();

	@EventHandler
	public void onWorldUnload(WorldUnloadEvent e) {
		workers.remove(e.getWorld());
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		final World world = e.getWorld();
		final Chunk chunk = e.getChunk();
		final IOWorker worker = getWorker(world);
		try {
			tags.put(chunk, worker.read(new ChunkCoords(chunk)));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent e) {
		final World world = e.getWorld();
		final Chunk chunk = e.getChunk();
		final IOWorker worker = getWorker(world);
		final JsonObject tag = tags.get(chunk);
		JsonArray entityTags = new JsonArray();
		for (Entity entity : chunk.getEntities())
			if (SaveTags.entityTags.containsKey(entity)) {
				entityTags.add(SaveTags.entityTags.get(entity));
				SaveTags.entityTags.remove(entity);
			}
		tag.add("EntityTags", entityTags);
		worker.write(new ChunkCoords(chunk), tag);
	}

	@EventHandler
	public void onEntityDie(EntityDeathEvent e) {
		entityTags.remove(e.getEntity());
	}
	
	@EventHandler
	public void onItemDespawn(ItemDespawnEvent e) {
		entityTags.remove(e.getEntity());
	}

	public IOWorker getWorker(final World world) {
		IOWorker worker = workers.get(world);
		if (worker == null) {
			worker = new IOWorker(new File(world.getWorldFolder(), "tags"), false, world.getName());
			workers.put(world, worker);
		}
		return worker;
	}

	@Nullable
	public static JsonObject getAllTags(Chunk chunk) {
		tags.putIfAbsent(chunk, new JsonObject());
		return chunk.isLoaded() ? tags.get(chunk) : null;
	}

	public static JsonObject getBlockTags(Block block) {
		final Location location = block.getLocation();
		final JsonObject tag = getAllTags(block.getChunk());
		final JsonObject blockTags = get(tag, "block tags");
		return get(blockTags, location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ());
	}

	public static JsonObject getChunkTags(Chunk chunk) {
		return get(getAllTags(chunk), "chunk tag");
	}

	private static JsonObject get(JsonObject json, String key) {
		if (json.has(key) && json.get(key).isJsonObject())
			return json.getAsJsonObject(key);
		final JsonObject value = new JsonObject();
		json.add(key, value);
		return value;
	}

	public static void setBlockTags(Block block, JsonObject jsonObject) {
		final JsonObject tag = getAllTags(block.getChunk());
		final Location location = block.getLocation();
		final JsonObject blockTags = get(tag, "block tags");
		blockTags.add(location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ(), jsonObject);
	}

	public static void setChunkTags(Chunk chunk, JsonObject jsonObject) {
		getAllTags(chunk).add("chunk tag", jsonObject);
	}

	public static void setEtityTags(Entity entity, JsonObject jsonObject) {
		entityTags.put(entity, jsonObject);
	}

	public static JsonObject getEntityTags(Entity entity) {
		entityTags.putIfAbsent(entity, new JsonObject());
		return entityTags.get(entity);
	}

	public static void checkForEntities() {
		Entity[] entities = (Entity[]) entityTags.keySet().toArray();
		for (Entity entity : entities)
			if (!entity.isValid())
				entityTags.remove(entity);
	}
//	public JsonObject getEntityTags(Chunk chunk) {
//		JsonObject json = new JsonObject();
//		for (Entity entity : chunk.getEntities()) {
//			JsonObject entityTags = readPersistentDataContainer(entity.getPersistentDataContainer());
//			if (entityTags.size() != 0)
//				json.add(entity.getUniqueId().toString(), entityTags);
//		}
//		return json;
//	}
//	
//	public static JsonObject getEntityTags(Entity entity) {
//		return readPersistentDataContainer(entity.getPersistentDataContainer());
//	}
//
//	public static void setEntityTags(Entity entity, JsonObject jsonObject) {
//		readJsonObject(jsonObject, entity.getPersistentDataContainer());
//	}
//
//	@SuppressWarnings("deprecation")
//	public static void readJsonObject(final JsonObject json, final PersistentDataContainer pdc) {
//		for (Entry<String, JsonElement> entry : json.entrySet()) {
//			final JsonElement value = entry.getValue();
//			final String[] s = entry.getKey().split(":");
//			final NamespacedKey key = new NamespacedKey(s[0], s[1]);
//			if (value.isJsonPrimitive()) {
//				final JsonPrimitive jsonPrimitive = value.getAsJsonPrimitive();
//				if (jsonPrimitive.isString()) {
//					pdc.set(key, PersistentDataType.STRING, jsonPrimitive.getAsString());
//				} else {
//					Number n = jsonPrimitive.getAsNumber();// TODO
//				}
//			} else if (value.isJsonObject()) {
//				PersistentDataContainer pdc2 = pdc.getAdapterContext().newPersistentDataContainer();
//				readJsonObject(value.getAsJsonObject(), pdc2);
//				pdc.set(key, PersistentDataType.TAG_CONTAINER, pdc2);
//			} else if (value.isJsonArray()) {
//				// TODO
//			}
//		}
//	}
//
//	public static JsonObject readPersistentDataContainer(final PersistentDataContainer pdc) {
//		JsonObject json = new JsonObject();
//		for (NamespacedKey key : pdc.getKeys()) {
//			if (pdc.has(key, PersistentDataType.BYTE)) {
//				json.addProperty(key.toString(), pdc.get(key, PersistentDataType.BYTE));
//			} else if (pdc.has(key, PersistentDataType.DOUBLE)) {
//				json.addProperty(key.toString(), pdc.get(key, PersistentDataType.DOUBLE));
//			} else if (pdc.has(key, PersistentDataType.FLOAT)) {
//				json.addProperty(key.toString(), pdc.get(key, PersistentDataType.FLOAT));
//			} else if (pdc.has(key, PersistentDataType.INTEGER)) {
//				json.addProperty(key.toString(), pdc.get(key, PersistentDataType.INTEGER));
//			} else if (pdc.has(key, PersistentDataType.LONG)) {
//				json.addProperty(key.toString(), pdc.get(key, PersistentDataType.LONG));
//			} else if (pdc.has(key, PersistentDataType.SHORT)) {
//				json.addProperty(key.toString(), pdc.get(key, PersistentDataType.SHORT));
//			} else if (pdc.has(key, PersistentDataType.STRING)) {
//				json.addProperty(key.toString(), pdc.get(key, PersistentDataType.STRING));
//			} else if (pdc.has(key, PersistentDataType.TAG_CONTAINER)) {
//				json.add(key.toString(), readPersistentDataContainer(pdc.get(key, PersistentDataType.TAG_CONTAINER)));
//			} else if (pdc.has(key, PersistentDataType.TAG_CONTAINER_ARRAY)) {
//				JsonArray jsonArray = new JsonArray();
//				for (PersistentDataContainer pdc2 : pdc.get(key, PersistentDataType.TAG_CONTAINER_ARRAY))
//					jsonArray.add(readPersistentDataContainer(pdc2));
//				json.add(key.toString(), jsonArray);
//			} else if (pdc.has(key, PersistentDataType.BYTE_ARRAY)) {
//				JsonArray jsonArray = new JsonArray();
//				for (byte i : pdc.get(key, PersistentDataType.BYTE_ARRAY))
//					jsonArray.add(i);
//				json.add(key.toString(), jsonArray);
//			} else if (pdc.has(key, PersistentDataType.INTEGER_ARRAY)) {
//				JsonArray jsonArray = new JsonArray();
//				for (int i : pdc.get(key, PersistentDataType.INTEGER_ARRAY))
//					jsonArray.add(i);
//				json.add(key.toString(), jsonArray);
//			} else if (pdc.has(key, PersistentDataType.LONG_ARRAY)) {
//				JsonArray jsonArray = new JsonArray();
//				for (long i : pdc.get(key, PersistentDataType.LONG_ARRAY))
//					jsonArray.add(i);
//				json.add(key.toString(), jsonArray);
//			}
//
//		}
//		return json;
//	}
}
