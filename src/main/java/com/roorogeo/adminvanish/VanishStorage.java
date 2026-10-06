package com.roorogeo.adminvanish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists who is vanished so the state survives relogs and restarts
 * (which is what makes silent joins possible).
 */
public final class VanishStorage {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("adminvanish.json");

	public static final class Entry {
		public String name;
		/** Game mode to restore when the player unvanishes. */
		public String previousGameMode;
		public boolean noclip = true;
	}

	private VanishStorage() {
	}

	public static Map<UUID, Entry> load() {
		Map<UUID, Entry> result = new LinkedHashMap<>();
		if (!Files.exists(FILE)) {
			return result;
		}
		try (Reader reader = Files.newBufferedReader(FILE)) {
			Map<String, Entry> raw = GSON.fromJson(reader, new TypeToken<Map<String, Entry>>() { }.getType());
			if (raw != null) {
				raw.forEach((uuid, entry) -> {
					try {
						result.put(UUID.fromString(uuid), entry);
					} catch (IllegalArgumentException ignored) {
						AdminVanish.LOGGER.warn("Ignoring invalid UUID '{}' in {}", uuid, FILE);
					}
				});
			}
		} catch (IOException | RuntimeException e) {
			AdminVanish.LOGGER.error("Failed to read {}", FILE, e);
		}
		return result;
	}

	public static void save(Map<UUID, Entry> entries) {
		Map<String, Entry> raw = new LinkedHashMap<>();
		entries.forEach((uuid, entry) -> raw.put(uuid.toString(), entry));
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(raw, writer);
			}
		} catch (IOException e) {
			AdminVanish.LOGGER.error("Failed to write {}", FILE, e);
		}
	}
}
