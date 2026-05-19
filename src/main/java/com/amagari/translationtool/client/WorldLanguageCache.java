package com.amagari.translationtool.client;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.network.WorldLanguageManifestPayload;
import com.amagari.translationtool.translation.WorldLanguageTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class WorldLanguageCache {
	private static final String CACHE_DIRECTORY = "amagari_translation_tool/lang_cache";
	private static final String CACHE_FILE_EXTENSION = ".gzbin";
	private static final Duration MAX_UNUSED_CACHE_AGE = Duration.ofDays(7);
	private static final int MAX_HASHES_PER_LANGUAGE = 2;

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
				markUsed(cacheFile);
			} catch (IOException exception) {
				missingLanguages.put(language.getKey(), language.getValue().hash());
				AmagariTranslationTool.LOGGER.warn("Could not load cached world language {}", language.getKey(), exception);
			}
		}

		cleanupCache(client);
		return new CacheLookup(cachedTranslations, missingLanguages);
	}

	public static Map<String, String> store(Minecraft client, String serverKey, String languageCode, String hash, int uncompressedBytes, byte[] compressedData) throws IOException {
		Map<String, String> translations = WorldLanguageTransfer.decodeCompressed(
				compressedData,
				uncompressedBytes,
				hash
		);

		Path serverCacheDirectory = serverCacheDirectory(client, serverKey);
		Files.createDirectories(serverCacheDirectory);
		Path cacheFile = cacheFile(serverCacheDirectory, languageCode, hash);
		Files.write(cacheFile, compressedData);
		markUsed(cacheFile);
		cleanupCache(client);
		return translations;
	}

	private static Path serverCacheDirectory(Minecraft client, String serverKey) {
		return client.gameDirectory.toPath().resolve(CACHE_DIRECTORY).resolve(serverKey);
	}

	private static Path cacheFile(Path serverCacheDirectory, String languageCode, String hash) {
		return serverCacheDirectory.resolve(safeSegment(languageCode) + "-" + hash + ".gzbin");
	}

	private static void markUsed(Path cacheFile) {
		try {
			Files.setLastModifiedTime(cacheFile, FileTime.from(Instant.now()));
		} catch (IOException exception) {
			AmagariTranslationTool.LOGGER.warn("Could not update world language cache access time for {}", cacheFile, exception);
		}
	}

	private static void cleanupCache(Minecraft client) {
		Path cacheRoot = client.gameDirectory.toPath().resolve(CACHE_DIRECTORY);
		if (!Files.isDirectory(cacheRoot)) {
			return;
		}

		Instant cutoff = Instant.now().minus(MAX_UNUSED_CACHE_AGE);
		try (Stream<Path> serverDirectories = Files.list(cacheRoot)) {
			serverDirectories
					.filter(Files::isDirectory)
					.forEach(serverCacheDirectory -> cleanupServerCache(serverCacheDirectory, cutoff));
		} catch (IOException exception) {
			AmagariTranslationTool.LOGGER.warn("Could not clean world language cache {}", cacheRoot, exception);
		}
	}

	private static void cleanupServerCache(Path serverCacheDirectory, Instant cutoff) {
		Map<String, List<CacheFile>> retainedFilesByLanguage = new HashMap<>();

		try (Stream<Path> cacheFiles = Files.list(serverCacheDirectory)) {
			cacheFiles
					.filter(Files::isRegularFile)
					.map(WorldLanguageCache::cacheFile)
					.flatMap(Optional::stream)
					.forEach(cacheFile -> retainOrDelete(cacheFile, cutoff, retainedFilesByLanguage));
		} catch (IOException exception) {
			AmagariTranslationTool.LOGGER.warn("Could not inspect world language cache directory {}", serverCacheDirectory, exception);
			return;
		}

		for (List<CacheFile> languageCacheFiles : retainedFilesByLanguage.values()) {
			languageCacheFiles.sort(Comparator
					.comparing(CacheFile::lastUsed)
					.reversed()
					.thenComparing(cacheFile -> cacheFile.path().getFileName().toString()));
			for (int index = MAX_HASHES_PER_LANGUAGE; index < languageCacheFiles.size(); index++) {
				deleteCacheFile(languageCacheFiles.get(index).path());
			}
		}
	}

	private static void retainOrDelete(CacheFile cacheFile, Instant cutoff, Map<String, List<CacheFile>> retainedFilesByLanguage) {
		if (cacheFile.lastUsed().toInstant().isBefore(cutoff)) {
			deleteCacheFile(cacheFile.path());
			return;
		}

		retainedFilesByLanguage
				.computeIfAbsent(cacheFile.languageCode(), ignored -> new ArrayList<>())
				.add(cacheFile);
	}

	private static Optional<CacheFile> cacheFile(Path path) {
		String fileName = path.getFileName().toString();
		if (!fileName.endsWith(CACHE_FILE_EXTENSION)) {
			return Optional.empty();
		}

		String cacheKey = fileName.substring(0, fileName.length() - CACHE_FILE_EXTENSION.length());
		int separator = cacheKey.lastIndexOf('-');
		if (separator <= 0 || separator == cacheKey.length() - 1) {
			return Optional.empty();
		}

		String hash = cacheKey.substring(separator + 1);
		if (!isSha256Hex(hash)) {
			return Optional.empty();
		}

		try {
			return Optional.of(new CacheFile(path, cacheKey.substring(0, separator), Files.getLastModifiedTime(path)));
		} catch (IOException exception) {
			AmagariTranslationTool.LOGGER.warn("Could not read world language cache timestamp for {}", path, exception);
			return Optional.empty();
		}
	}

	private static boolean isSha256Hex(String value) {
		if (value.length() != 64) {
			return false;
		}
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			boolean digit = character >= '0' && character <= '9';
			boolean lowerHex = character >= 'a' && character <= 'f';
			boolean upperHex = character >= 'A' && character <= 'F';
			if (!digit && !lowerHex && !upperHex) {
				return false;
			}
		}
		return true;
	}

	private static void deleteCacheFile(Path cacheFile) {
		try {
			Files.deleteIfExists(cacheFile);
		} catch (IOException exception) {
			AmagariTranslationTool.LOGGER.warn("Could not delete stale world language cache {}", cacheFile, exception);
		}
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

	private record CacheFile(Path path, String languageCode, FileTime lastUsed) {
	}
}
