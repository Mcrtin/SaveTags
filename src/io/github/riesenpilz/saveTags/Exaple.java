package io.github.riesenpilz.saveTags;

import java.io.File;
import java.io.IOException;

import com.google.gson.JsonObject;

public class Exaple {
	public static void main(String[] args) throws IOException {
		IOWorker ioWorker = new IOWorker(new File("D:/storageTest"), true, "test");
		final JsonObject json = new JsonObject();
		json.addProperty("test1", false);
		ioWorker.write(new ChunkCoords(), json);
		System.out.println(ioWorker.read(new ChunkCoords()));
		ioWorker.close();
		System.out.println("finished");
	}
}
