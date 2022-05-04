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
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;
import io.github.riesenpilz.saveTags.storage.ChunkCoords;
import io.github.riesenpilz.saveTags.storage.IOWorker;

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

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onWorldUnload(WorldUnloadEvent e) {
		workers.remove(e.getWorld());
		Bukkit.getScheduler().runTask(this, () -> EntityTags.checkEntities());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
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

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onChunkUnload(ChunkUnloadEvent e) {
		final World world = e.getWorld();
		final Chunk chunk = e.getChunk();
		final IOWorker worker = getWorker(world);
		final JsonObjectWrapper tag = tags.get(chunk);
		tag.add("EntityTags", EntityTags.save(chunk.getEntities()));
		worker.write(new ChunkCoords(chunk), tag.jsonObject());
		tags.remove(chunk);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onEntityDie(EntityDeathEvent e) {
		EntityTags.of(e.getEntity()).remove();
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onItemDespawn(ItemDespawnEvent e) {
		EntityTags.of(e.getEntity()).remove();
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onBlockBreak(BlockBreakEvent e) {
		BlockTags.of(e.getBlock()).remove();
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onBlockChange(BlockBurnEvent e) {
		BlockTags.of(e.getBlock()).remove();
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onPistonExtend(BlockPistonExtendEvent e) {

		Map<Block, BlockTags> newLocations = new HashMap<>();
		for (org.bukkit.block.Block block : e.getBlocks()) {
			final BlockTags tags = BlockTags.of(e.getBlock());
			switch (block.getPistonMoveReaction()) {
			case MOVE:
			case PUSH_ONLY:
				newLocations.put(block.getRelative(e.getDirection()), tags);
			case BREAK:
				tags.remove();
				break;
			case BLOCK:
			case IGNORE:
			default:
				break;
			}
		}
		newLocations.forEach((block, tags) -> new BlockTags(block, tags));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onPistonRetract(BlockPistonRetractEvent e) {
		Map<Block, BlockTags> newLocations = new HashMap<>();
		for (org.bukkit.block.Block block : e.getBlocks()) {
			final BlockTags tags = BlockTags.of(e.getBlock());
			switch (block.getPistonMoveReaction()) {
			case MOVE:
			case PUSH_ONLY:
				newLocations.put(block.getRelative(e.getDirection()), tags);
			case BREAK:
				tags.remove();
				break;
			case BLOCK:
			case IGNORE:
			default:
				break;
			}
		}
		newLocations.forEach((block, tags) -> new BlockTags(block, tags));
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
}
