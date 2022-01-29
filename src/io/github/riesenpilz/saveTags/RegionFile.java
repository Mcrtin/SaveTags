package io.github.riesenpilz.saveTags;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.BitSet;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;

import com.google.common.annotations.VisibleForTesting;

import lombok.extern.log4j.Log4j2;

@Log4j2
class RegionFile implements AutoCloseable {

	private static final ByteBuffer buff = ByteBuffer.allocateDirect(1);
	private final FileChannel dataFile;
	private final Path dir;
	private final ByteBuffer byteBuff8192 = ByteBuffer.allocateDirect(8192);
	private final IntBuffer firstByteBuffAsInt4096Bytes = byteBuff8192.asIntBuffer();
	private final IntBuffer secondByteBuffAsInt4096Bytes;
	@VisibleForTesting
	private final RegionFileBitSet freeSectors = new RegionFileBitSet();

	RegionFile(File dir, String fileName, boolean sync) throws IOException {
		this.dir = dir.toPath();
		Path file = new File(dir, fileName).toPath();
		Validate.isTrue(Files.isDirectory(this.dir, new LinkOption[0]),
				"Expected directory, got " + this.dir.toAbsolutePath());

		firstByteBuffAsInt4096Bytes.limit(1024);
		byteBuff8192.position(4096);
		secondByteBuffAsInt4096Bytes = byteBuff8192.asIntBuffer();
		byteBuff8192.position(0);

		dataFile = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE, sync ? StandardOpenOption.SYNC : StandardOpenOption.DSYNC);

		freeSectors.add(0, 2);

		int header = dataFile.read(byteBuff8192, 0L);

		if (header == -1)
			return;

		if (header != 8192)
			log.warn("Region file {} has truncated header: {}", file, header);

		long size = Files.size(file);

		for (int pos = 0; pos < 1024; ++pos) {
			int current = firstByteBuffAsInt4096Bytes.get(pos);

			if (current == 0)
				continue;

			int last3Bytes = last3Bytes(current);
			int sectorSize = fistByte(current);

			if (last3Bytes < 2) {
				log.warn("Region file {} has invalid sector at index: {}; sector {} overlaps with header", file,
						pos, last3Bytes);
				firstByteBuffAsInt4096Bytes.put(pos, 0);
			} else if (sectorSize == 0) {
				log.warn("Region file {} has an invalid sector at index: {}; size has to be > 0", file, pos);
				firstByteBuffAsInt4096Bytes.put(pos, 0);
			} else if (last3Bytes * 4096L > size) {
				log.warn("Region file {} has an invalid sector at index: {}; sector {} is out of bounds", file,
						pos, last3Bytes);
				firstByteBuffAsInt4096Bytes.put(pos, 0);
			} else
				freeSectors.add(last3Bytes, sectorSize);
		}
	}

	private Path getPath(ChunkCoords chunk) {
		String fileName = "c." + chunk.getX() + "." + chunk.getZ() + ".mcc";
		return dir.resolve(fileName);
	}

	@Nullable
	synchronized DataInputStream openInputStream(ChunkCoords chunk) throws IOException {
		int offset = getOffset(chunk);

		if (offset == 0)
			return null;

		int last3Bytes = last3Bytes(offset);
		int fistByte = fistByte(offset);
		int l = fistByte * 4096;
		ByteBuffer bytebuffer = ByteBuffer.allocate(l);

		dataFile.read(bytebuffer, last3Bytes * 4096);
		bytebuffer.flip();
		if (bytebuffer.remaining() < 5) {
			log.error("ChunkCoords {} header is truncated: expected {} but read {}", chunk, l, bytebuffer.remaining());
			return null;
		}
		int stream = bytebuffer.getInt();

		if (stream == 0) {
			log.warn("ChunkCoords {} is allocated, but stream is missing", chunk);
			return null;
		}
		int streamMinus1 = stream - 1;

		if (streamMinus1 > bytebuffer.remaining()) {
			log.error("ChunkCoords {} stream is truncated: expected {} but read {}", chunk, streamMinus1,
					bytebuffer.remaining());
			return null;
		} else if (streamMinus1 < 0) {
			log.error("Declared size {} of chunk {} is negative", stream, chunk);
			return null;
		} else
			return toDataInputStream(clip(bytebuffer, streamMinus1));
	}

	@Nullable
	private DataInputStream toDataInputStream(InputStream inputstream) {
		return new DataInputStream(new BufferedInputStream(new InflaterInputStream((inputstream))));
	}

	@Nullable
	private DataInputStream a(ChunkCoords chunk) throws IOException {
		Path path = getPath(chunk);

		if (!Files.isRegularFile(path, new LinkOption[0])) {
			log.error("External chunk path {} is not file", path);
			return null;
		}
		return this.toDataInputStream(Files.newInputStream(path));
	}

	private static ByteArrayInputStream clip(ByteBuffer bytebuffer, int count) {
		return new ByteArrayInputStream(bytebuffer.array(), bytebuffer.position(), count);
	}

	private int a(int i, int j) {
		return i << 8 | j;
	}

	private static int fistByte(int i) {
		return i & 255;
	}

	private static int last3Bytes(int i) {
		return i >> 8 & 16777215;
	}

	/**
	 * i > 4096 ? i == 2 : 1;
	 * 
	 * @param size
	 * @return
	 */
	private static int buffsNeeded(int size) {
		return (size + 4096 - 1) / 4096;
	}

	DataOutputStream openOutputStream(ChunkCoords chunk) {
		return new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(((new ChunkCoordsBuffer(chunk))))));
	}

	void force() throws IOException {
		dataFile.force(true);
	}

	private synchronized void write(ChunkCoords chunk, ByteBuffer bytebuffer) throws IOException {
		int identifier = chunk.getX() & 31 + chunk.getZ() & 31 * 32;
		int offset = firstByteBuffAsInt4096Bytes.get(identifier);
		int last3Bytes = last3Bytes(offset);
		int fistByte = fistByte(offset);
		int size = bytebuffer.remaining();
		int buffsNeeded = buffsNeeded(size);
		int buffs;
		Runnable runnable;

		if (buffsNeeded >= 256) {
			Path path = getPath(chunk);

			log.warn("Saving oversized chunk {} ({} bytes) to external file {}", chunk, size, path);
			buffsNeeded = 1;
			buffs = freeSectors.add(buffsNeeded);
			runnable = createTmpFile(path, bytebuffer);
			ByteBuffer firstBuff = createFirstBuff();

			dataFile.write(firstBuff, buffs * 4096);
		} else {
			buffs = freeSectors.add(buffsNeeded);
			runnable = () -> Files.deleteIfExists(getPath(chunk));
			dataFile.write(bytebuffer, buffs * 4096);
		}

		int timeSeconds = (int) (Instant.now().toEpochMilli() / 1000L);

		firstByteBuffAsInt4096Bytes.put(identifier, a(buffs, buffsNeeded));
		secondByteBuffAsInt4096Bytes.put(identifier, timeSeconds);
		writeByteBuff();
		runnable.run();
		if (last3Bytes != 0)
			freeSectors.remove(last3Bytes, fistByte);

	}

	private ByteBuffer createFirstBuff() {
		ByteBuffer bytebuffer = ByteBuffer.allocate(5);
		bytebuffer.putInt(1);
		bytebuffer.put((byte) 0);
		bytebuffer.flip();
		return bytebuffer;
	}

	private Runnable createTmpFile(Path path, ByteBuffer bytebuffer) throws IOException {
		Path tmp = Files.createTempFile(dir, "tmp", null);
		FileChannel filechannel = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		Throwable throwable = null;

		try {
			bytebuffer.position(5);
			filechannel.write(bytebuffer);
		} catch (Throwable throwable1) {
			throwable = throwable1;
			throw throwable1;
		} finally {
			if (filechannel != null)
				if (throwable != null)
					try {
						filechannel.close();
					} catch (Throwable throwable2) {
						throwable.addSuppressed(throwable2);
					}
				else
					filechannel.close();

		}

		return () -> Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
	}

	private void writeByteBuff() throws IOException {
		byteBuff8192.position(0);
		dataFile.write(byteBuff8192, 0L);
	}

	private int getOffset(ChunkCoords chunk) {
		return firstByteBuffAsInt4096Bytes.get(chunk.getX() & 31 + chunk.getZ() & 31 * 32);
	}

	public void close() throws IOException {
		try {
			flush();
		} finally {
			try {
				dataFile.force(true);
			} finally {
				dataFile.close();
			}
		}

	}

	private void flush() throws IOException {
		int size = (int) dataFile.size();
		int buffsNeeded = buffsNeeded(size) * 4096;

		if (size == buffsNeeded)
			return;

		ByteBuffer bytebuffer = RegionFile.buff.duplicate();
		bytebuffer.position(0);
		dataFile.write(bytebuffer, buffsNeeded - 1);
	}

	/**
	 * Runnable with IOException
	 */
	@FunctionalInterface
	private static interface Runnable {

		void run() throws IOException;
	}

	/**
	 * Prepare OutputStream
	 *
	 */
	private class ChunkCoordsBuffer extends ByteArrayOutputStream {

		private final ChunkCoords chunk;

		private ChunkCoordsBuffer(ChunkCoords chunk) {
			super(8096);
			write(0);
			write(0);
			write(0);
			write(0);
			this.chunk = chunk;
		}

		public void close() throws IOException {
			ByteBuffer bytebuffer = ByteBuffer.wrap(buf, 0, count);

			bytebuffer.putInt(0, count - 5 + 1);
			RegionFile.this.write(chunk, bytebuffer);
		}
	}

	/**
	 * Stores where which chunk is
	 */
	private static class RegionFileBitSet {

		private final BitSet bits = new BitSet();

		void add(int startpos, int lengh) {
			bits.set(startpos, startpos + lengh);
		}

		void remove(int startpos, int lengh) {
			bits.clear(startpos, startpos + lengh);
		}

		int add(int count) {
			int counter = 0;

			while (true) {
				int nextClear = bits.nextClearBit(counter);
				int nextSet = bits.nextSetBit(nextClear);

				if (nextSet == -1 || nextSet - nextClear >= count) {
					add(nextClear, count);
					return nextClear;
				}

				counter = nextSet;
			}
		}
	}
}
