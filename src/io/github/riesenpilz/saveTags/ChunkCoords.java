package io.github.riesenpilz.saveTags;

import org.bukkit.Chunk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Tolerate;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ChunkCoords {
	private int x;
	private int z;

	@Tolerate
	public ChunkCoords(Chunk chunk) {
		x = chunk.getX();
		z = chunk.getZ();
	}
}
