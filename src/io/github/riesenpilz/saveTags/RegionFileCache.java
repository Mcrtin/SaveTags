package io.github.riesenpilz.saveTags;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.annotation.Nullable;

import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.ObjectIterator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.minecraft.server.v1_16_R3.ExceptionSuppressor;

public final class RegionFileCache implements AutoCloseable {

	public final Long2ObjectLinkedOpenHashMap<RegionFile> cache = new Long2ObjectLinkedOpenHashMap<RegionFile>();
	private final File b;
	private final boolean c;

	public RegionFileCache(File file, boolean flag) {
		this.b = file;
		this.c = flag;
	}

	private RegionFile getFile(ChunkCoords chunk) throws IOException {
		long i = chunk.getX() & 4294967295L | (chunk.getZ() & 4294967295L) << 32;
		RegionFile regionfile = this.cache.getAndMoveToFirst(i);

		if (regionfile != null) {
			return regionfile;
		}
		if (this.cache.size() >= 256) {
			this.cache.removeLast().close();
		}

		if (!this.b.exists()) {
			this.b.mkdirs();
		}

		RegionFile regionfile1 = new RegionFile(this.b, "r." + chunk.getX() + "." + chunk.getZ() + ".mca", this.c);

		this.cache.putAndMoveToFirst(i, regionfile1);
		return regionfile1;
	}

	@Nullable
	public JsonObject read(ChunkCoords chunk) throws Exception {
		RegionFile regionfile = this.getFile(chunk);
		DataInputStream datainputstream = regionfile.openInputStream(chunk);
		Throwable throwable = null;

		JsonObject nbttagcompound;

		try {
			if (datainputstream != null) {
				datainputstream.readUTF();

				return new Gson().fromJson(new InputStreamReader(datainputstream), JsonObject.class);
//				try {
//					return NBTReader.readJsonObject(datainputstream, 0, new NBTReadLimiter(2097152));
//				} catch (IOException ioexception) {
//					throw new DataFormatException("can't read NBT data");
//				}
			}

			nbttagcompound = null;
		} catch (Throwable throwable1) {
			throwable = throwable1;
			throw throwable1;
		} finally {
			if (datainputstream != null) {
				if (throwable != null) {
					try {
						datainputstream.close();
					} catch (Throwable throwable2) {
						throwable.addSuppressed(throwable2);
					}
				} else {
					datainputstream.close();
				}
			}

		}

		return nbttagcompound;
	}

	protected void write(ChunkCoords chunk, JsonObject nbttagcompound) throws IOException {
		RegionFile regionfile = this.getFile(chunk);
		DataOutputStream dataoutputstream = regionfile.openOutputStream(chunk);
		Throwable throwable = null;

		try {
			dataoutputstream.writeUTF("");
			new Gson().toJson(nbttagcompound, new OutputStreamWriter(dataoutputstream));
//			nbttagcompound.write(dataoutputstream);
		} catch (Throwable throwable1) {
			throwable = throwable1;
			throw throwable1;
		} finally {
			if (dataoutputstream != null) {
				if (throwable != null) {
					try {
						dataoutputstream.close();
					} catch (Throwable throwable2) {
						throwable.addSuppressed(throwable2);
					}
				} else {
					dataoutputstream.close();
				}
			}

		}

	}

	public void close() throws IOException {
		ExceptionSuppressor<IOException> exceptionsuppressor = new ExceptionSuppressor<>();
		ObjectIterator<RegionFile> objectiterator = this.cache.values().iterator();

		while (objectiterator.hasNext()) {
			RegionFile regionfile = objectiterator.next();

			try {
				regionfile.close();
			} catch (IOException ioexception) {
				exceptionsuppressor.a(ioexception);
			}
		}

		exceptionsuppressor.a();
	}

	public void a() throws IOException {
		ObjectIterator<RegionFile> objectiterator = this.cache.values().iterator();

		while (objectiterator.hasNext()) {
			RegionFile regionfile = objectiterator.next();

			regionfile.close();
		}

	}
}
