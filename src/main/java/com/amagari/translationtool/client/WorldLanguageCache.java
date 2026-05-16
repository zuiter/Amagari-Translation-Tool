package com.amagari.translationtool.client;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.network.WorldLanguageDataPayload;
import com.amagari.translationtool.network.WorldLanguageManifestPayload;
import com.amagari.translationtool.translation.WorldLanguageTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public final class WorldLanguageCache {
	private static final String CACHE_DIRECTORY = "amagari_translation_tool/lang_cache";

	private WorldLanguageCache() {
	}

	public static String currentServerKey(Minecraft client) {
		ServerData server = client.getCurrentServer();
		String serverAddress = server == null ? "unknown" : server.ip;
		return sha256Hex(serverAddress);
	}

	public static CacheLookup loadCachedLanguages(Minecraft client, String serverKey, Map<String, WorldLanguageManifestPayload.LanguageManifestEntry> manifest) {
		Path serverCacheDirectory = serverCacheDirectory(client, serverKey);
		Map<String, Map<String, String>> cachedTranslations = new HashMap<>();
		Map<String, String> missingLanguages = new HashMap<>();

		for (Map.Entry<String, WorldLanguageManifestPayload.LanguageManifestEntry> language : manifest.entrySet()) {
			try {
				Path cacheFile = cacheFile(serverCacheDirectory, language.getKey(), language.getValue().hash());
				if (!Files.isRegularFile(cacheFile)) {
					missingLanguages.put(language.getKey(), language.getValue().hash());
					continue;
				}

				cachedTranslations.put(language.getKey(), WorldLanguageTransfer.decodeCompressed(
						Files.readAllBytes(cacheFile),
						language.getValue().uncompressedBytes(),
						language.getValue().hash()
				));
			} catch (IOException exception) {
				missingLanguages.put(language.getKey(), language.getValue().hash());
				AmagariTranslationTool.LOGGER.warn("Could not load cached world language {}", language.getKey(), exception);
			}
		}

		return new CacheLookup(cachedTranslations, missingLanguages);
	}

	public static Map<String, String> store(Minecraft client, String serverKey, WorldLanguageDataPayload payload) throws IOException {
		Map<String, String> translations = WorldLanguageTransfer.decodeCompressed(
				payload.compressedData(),
				payload.uncompressedBytes(),
				payload.hash()
		);

		Path serverCacheDirectory = serverCacheDirectory(client, serverKey);
		Files.createDirectories(serverCacheDirectory);
		Files.write(cacheFile(serverCacheDirectory, payload.languageCode(), payload.hash()), payload.compressedData());
		return translations;
	}

	private static Path serverCacheDirectory(Minecraft client, String serverKey) {
		return client.gameDirectory.toPath().resolve(CACHE_DIRECTORY).resolve(serverKey);
	}

	private static Path cacheFile(Path serverCacheDirectory, String languageCode, String hash) {
		return serverCacheDirectory.resolve(safeSegment(languageCode) + "-" + hash + ".gzbin");
	}

	private static String safeSegment(String value) {
		return value.replaceAll("[^a-zA-Z0-9_.-]", "_");
	}

	private static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder text = new StringBuilder(hash.length * 2);
			for (byte hashByte : hash) {
				text.append(String.format("%02x", hashByte));
			}
			return text.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	public record CacheLookup(
			Map<String, Map<String, String>> cachedTranslations,
			Map<String, String> missingLanguages
	) {
	}
}
