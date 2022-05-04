package io.github.riesenpilz.saveTags.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.annotation.Nullable;

import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.Value;

@Value
public class RegionFileCache implements AutoCloseable {

	private Long2ObjectLinkedOpenHashMap<RegionFile> cache = new Long2ObjectLinkedOpenHashMap<RegionFile>();
	@NonNull
	private File dir;
	private boolean sync;

	private RegionFile getFile(ChunkCoords chunk) throws IOException {
		long chunkId = chunk.getX() & 4294967295L | (chunk.getZ() & 4294967295L) << 32;
		RegionFile regionfile = cache.getAndMoveToFirst(chunkId);
		
		if (regionfile != null)
			return regionfile;
		if (cache.size() >= 256)
			cache.removeLast().close();
		if (!dir.exists())
			dir.mkdirs();

		RegionFile regionfile1 = new RegionFile(this.dir, "r." + chunk.getX() + "." + chunk.getZ() + ".mca", this.sync);

		cache.putAndMoveToFirst(chunkId, regionfile1);
		return regionfile1;
	}

	@Nullable
	public JsonObject read(ChunkCoords chunk) throws IOException {
		RegionFile regionFile = getFile(chunk);
		DataInputStream dis = regionFile.openInputStream(chunk);
		dis.readUTF();
		@Cleanup
		final InputStreamReader isw = new InputStreamReader(dis);
		return new Gson().fromJson(isw, JsonObject.class);
	}

	public void write(ChunkCoords chunk, JsonObject json) throws IOException {
		final RegionFile regionFile = getFile(chunk);
		final DataOutputStream dos = regionFile.openOutputStream(chunk);
		dos.writeUTF("");
		@Cleanup
		final OutputStreamWriter osw = new OutputStreamWriter(dos);
		new Gson().toJson(json, osw);

	}

	public void close() throws IOException {
		IOException ex = null;
		for (RegionFile regionFile : cache.values())
			try {
				regionFile.close();
			} catch (IOException ioe) {
				if (ex == null)
					ex = ioe;
				else
					ex.addSuppressed(ioe);
			}
		if (ex != null)
			throw ex;
	}

	public void close2() throws IOException {
		for (RegionFile regionFile : cache.values())
			regionFile.close();
	}
}
