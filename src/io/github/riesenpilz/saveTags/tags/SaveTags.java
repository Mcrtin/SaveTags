package io.github.riesenpilz.saveTags.tags;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.riesenpilz.saveTags.ChunkCoords;
import io.github.riesenpilz.saveTags.IOWorker;
import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;

public class SaveTags extends JavaPlugin implements Listener {
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	public void onDisable() {
		List<Chunk> chunks = new ArrayList<>(tags.keySet());
		for (Entity entity : EntityTags.entityTags.keySet())
			chunks.add(entity.getLocation().getChunk());

		for (Chunk chunk : chunks) {
			final IOWorker worker = getWorker(chunk.getWorld());
			final JsonObjectWrapper tag = tags.get(chunk);
			tag.add("EntityTags", EntityTags.save(chunk.getEntities()));
			worker.write(new ChunkCoords(chunk), tag.jsonObject());
		}

		for (IOWorker worker : workers.values())
			try {
				worker.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private static final Map<World, IOWorker> workers = new HashMap<>();
	public static final Map<Chunk, JsonObjectWrapper> tags = new HashMap<>();

	@EventHandler
	public void onWorldUnload(WorldUnloadEvent e) {
		workers.remove(e.getWorld());
		Bukkit.getScheduler().runTask(this, () -> checkForEntities());
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		final World world = e.getWorld();
		final Chunk chunk = e.getChunk();
		final IOWorker worker = getWorker(world);
		try {
			final JsonObjectWrapper read = new JsonObjectWrapper(worker.read(new ChunkCoords(chunk)));
			EntityTags.load(read.getJsonObject("EntityTags"));
			read.remove("EntityTags");
			tags.put(chunk, read);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent e) {
		final World world = e.getWorld();
		final Chunk chunk = e.getChunk();
		final IOWorker worker = getWorker(world);
		final JsonObjectWrapper tag = tags.get(chunk);
		tag.add("EntityTags", EntityTags.save(chunk.getEntities()));
		worker.write(new ChunkCoords(chunk), tag.jsonObject());
		tags.remove(chunk);
	}

	@EventHandler
	public void onEntityDie(EntityDeathEvent e) {
		EntityTags.get(e.getEntity()).setTags(null);
	}

	@EventHandler
	public void onItemDespawn(ItemDespawnEvent e) {
		EntityTags.get(e.getEntity()).setTags(null);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		BlockTags.getTags(e.getBlock()).setTags(null);
	}

	public IOWorker getWorker(final World world) {
		IOWorker worker = workers.get(world);
		if (worker == null) {
			worker = new IOWorker(new File(world.getWorldFolder(), "tags"), false, world.getName());
			workers.put(world, worker);
		}
		return worker;
	}

	public static JsonObjectWrapper getAllTags(Chunk chunk) {
		tags.putIfAbsent(chunk, new JsonObjectWrapper());
		return tags.get(chunk);
	}

	public static void checkForEntities() {
		Entity[] entities = (Entity[]) EntityTags.entityTags.keySet().toArray();
		for (Entity entity : entities)
			if (!entity.isValid())
				EntityTags.entityTags.remove(entity);
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
