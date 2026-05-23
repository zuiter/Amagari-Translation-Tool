package com.amagari.translationtool.client;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.client.paratranz.ParaTranzContext;
import com.amagari.translationtool.network.WorldLanguageDataPayload;
import com.amagari.translationtool.network.WorldLanguageManifestPayload;
import com.amagari.translationtool.translation.WorldLanguageFiles;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class WorldLanguageContext {
	private static final Duration PENDING_REMOTE_LANGUAGE_TIMEOUT = Duration.ofMinutes(2);
	private static final AtomicReference<Path> CURRENT_WORLD_DIRECTORY = new AtomicReference<>();
	private static final AtomicReference<Map<String, Map<String, String>>> REMOTE_TRANSLATIONS = new AtomicReference<>(Map.of());
	private static final AtomicReference<Map<String, WorldLanguageManifestPayload.LanguageManifestEntry>> REMOTE_MANIFEST = new AtomicReference<>(Map.of());
	private static final AtomicReference<Map<String, String>> PENDING_REMOTE_LANGUAGES = new AtomicReference<>(Map.of());
	private static final AtomicReference<Map<String, PendingRemoteLanguage>> PENDING_REMOTE_LANGUAGE_CHUNKS = new AtomicReference<>(Map.of());
	private static final AtomicReference<String> REMOTE_SERVER_KEY = new AtomicReference<>("");
	private static final AtomicReference<Boolean> REMOTE_READY = new AtomicReference<>(false);
	private static final AtomicReference<WorldLanguageReport> LAST_REPORT = new AtomicReference<>(WorldLanguageReport.notLoaded());

	private WorldLanguageContext() {
	}

	public static void enterWorld(Path worldDirectory) {
		CURRENT_WORLD_DIRECTORY.set(worldDirectory);
		clearRemote();
		LAST_REPORT.set(WorldLanguageReport.empty(worldDirectory));
		AmagariTranslationTool.LOGGER.info("Using world language directory {}", worldDirectory.resolve(WorldLanguageFiles.LANG_DIRECTORY));
	}

	public static void leaveWorld() {
		CURRENT_WORLD_DIRECTORY.set(null);
		clearRemote();
		LAST_REPORT.set(WorldLanguageReport.notLoaded());
	}

	public static Map<String, String> receiveRemoteManifest(Minecraft client, Map<String, WorldLanguageManifestPayload.LanguageManifestEntry> manifest) {
		String serverKey = WorldLanguageCache.currentServerKey(client);
		WorldLanguageCache.CacheLookup cacheLookup = WorldLanguageCache.loadCachedLanguages(client, serverKey, manifest);
		REMOTE_SERVER_KEY.set(serverKey);
		REMOTE_MANIFEST.set(Map.copyOf(manifest));
		REMOTE_TRANSLATIONS.set(copyTranslations(cacheLookup.cachedTranslations()));
		PENDING_REMOTE_LANGUAGES.set(Map.copyOf(cacheLookup.missingLanguages()));
		PENDING_REMOTE_LANGUAGE_CHUNKS.set(Map.of());
		REMOTE_READY.set(true);
		LAST_REPORT.set(WorldLanguageReport.remoteManifest(manifest.size(), cacheLookup.cachedTranslations().size(), cacheLookup.missingLanguages().size()));
		return cacheLookup.missingLanguages();
	}

	public static boolean receiveRemoteLanguageData(Minecraft client, WorldLanguageDataPayload payload) {
		WorldLanguageManifestPayload.LanguageManifestEntry manifestEntry = REMOTE_MANIFEST.get().get(payload.languageCode());
		if (manifestEntry == null || !manifestEntry.hash().equals(payload.hash())) {
			AmagariTranslationTool.LOGGER.warn("Ignored unexpected world language data for {}", payload.languageCode());
			return false;
		}
		if (manifestEntry.uncompressedBytes() != payload.uncompressedBytes()
				|| manifestEntry.compressedBytes() != payload.totalCompressedBytes()
				|| manifestEntry.entries() != payload.entries()) {
			AmagariTranslationTool.LOGGER.warn("Ignored mismatched world language data for {}", payload.languageCode());
			return false;
		}
		if (!PENDING_REMOTE_LANGUAGES.get().containsKey(payload.languageCode())) {
			return false;
		}

		try {
			PendingRemoteLanguage pendingLanguage = receiveChunk(client, payload);
			if (!pendingLanguage.complete()) {
				return false;
			}

			Map<String, String> translations = WorldLanguageCache.store(
					client,
					REMOTE_SERVER_KEY.get(),
					payload.languageCode(),
					payload.hash(),
					payload.uncompressedBytes(),
					pendingLanguage.assemble()
			);
			Map<String, Map<String, String>> remoteTranslations = new HashMap<>(REMOTE_TRANSLATIONS.get());
			remoteTranslations.put(payload.languageCode(), translations);
			REMOTE_TRANSLATIONS.set(copyTranslations(remoteTranslations));

			Map<String, String> pendingLanguages = new HashMap<>(PENDING_REMOTE_LANGUAGES.get());
			pendingLanguages.remove(payload.languageCode());
			PENDING_REMOTE_LANGUAGES.set(Map.copyOf(pendingLanguages));
			Map<String, PendingRemoteLanguage> pendingChunks = new HashMap<>(PENDING_REMOTE_LANGUAGE_CHUNKS.get());
			pendingChunks.remove(payload.languageCode());
			PENDING_REMOTE_LANGUAGE_CHUNKS.set(Map.copyOf(pendingChunks));
			LAST_REPORT.set(WorldLanguageReport.remote(REMOTE_TRANSLATIONS.get()));
			return true;
		} catch (Exception exception) {
			AmagariTranslationTool.LOGGER.warn("Could not store world language data for {}", payload.languageCode(), exception);
			removePendingChunks(payload.languageCode());
			return false;
		}
	}

	public static Optional<Path> getWorldDirectory() {
		return Optional.ofNullable(CURRENT_WORLD_DIRECTORY.get());
	}

	public static void mergeTranslations(List<String> languageCodes, Map<String, String> translations) {
		Map<String, Map<String, String>> remoteTranslations = REMOTE_TRANSLATIONS.get();
		if (REMOTE_READY.get()) {
			WorldLanguageReport report = mergeRemoteTranslations(languageCodes, translations, remoteTranslations);
			LAST_REPORT.set(report);
			report.log();
			ParaTranzContext.mergeTranslations(languageCodes, translations);
			return;
		}

		Optional<Path> worldDirectory = getWorldDirectory();
		if (worldDirectory.isEmpty()) {
			LAST_REPORT.set(WorldLanguageReport.notLoaded());
			ParaTranzContext.mergeTranslations(languageCodes, translations);
			return;
		}

		WorldLanguageReport report = WorldLanguageReport.local(WorldLanguageFiles.loadSelectedInto(worldDirectory.get(), languageCodes, translations));
		LAST_REPORT.set(report);
		report.log();
		ParaTranzContext.mergeTranslations(languageCodes, translations);
	}

	public static String describeLastReport() {
		return LAST_REPORT.get().describe();
	}

	public static String describeLastReport(String languageCode) {
		return LAST_REPORT.get().describe(languageCode);
	}

	private static WorldLanguageReport mergeRemoteTranslations(List<String> languageCodes, Map<String, String> translations, Map<String, Map<String, String>> translationsByLanguage) {
		int loadedEntries = 0;
		int loadedLanguages = 0;
		for (String languageCode : languageCodes) {
			Map<String, String> languageTranslations = translationsByLanguage.get(languageCode);
			if (languageTranslations == null) {
				continue;
			}
			translations.putAll(languageTranslations);
			loadedEntries += languageTranslations.size();
			loadedLanguages++;
		}
		return WorldLanguageReport.loadedRemote(loadedLanguages, loadedEntries);
	}

	private static Map<String, Map<String, String>> copyTranslations(Map<String, Map<String, String>> translationsByLanguage) {
		Map<String, Map<String, String>> copiedTranslations = new HashMap<>();
		for (Map.Entry<String, Map<String, String>> entry : translationsByLanguage.entrySet()) {
			copiedTranslations.put(entry.getKey(), Map.copyOf(entry.getValue()));
		}
		return Map.copyOf(copiedTranslations);
	}

	private static void clearRemote() {
		REMOTE_TRANSLATIONS.set(Map.of());
		REMOTE_MANIFEST.set(Map.of());
		PENDING_REMOTE_LANGUAGES.set(Map.of());
		PENDING_REMOTE_LANGUAGE_CHUNKS.set(Map.of());
		REMOTE_SERVER_KEY.set("");
		REMOTE_READY.set(false);
	}

	private static PendingRemoteLanguage receiveChunk(Minecraft client, WorldLanguageDataPayload payload) {
		Map<String, PendingRemoteLanguage> pendingChunks = new HashMap<>(PENDING_REMOTE_LANGUAGE_CHUNKS.get());
		PendingRemoteLanguage pendingLanguage = pendingChunks.get(payload.languageCode());
		if (pendingLanguage == null) {
			pendingLanguage = new PendingRemoteLanguage(payload);
			pendingChunks.put(payload.languageCode(), pendingLanguage);
			schedulePendingChunkCleanup(client, payload.languageCode(), pendingLanguage.createdAtNanos());
		}
		pendingLanguage.add(payload);
		PENDING_REMOTE_LANGUAGE_CHUNKS.set(Map.copyOf(pendingChunks));
		return pendingLanguage;
	}

	private static void schedulePendingChunkCleanup(Minecraft client, String languageCode, long createdAtNanos) {
		CompletableFuture.runAsync(
				() -> client.execute(() -> removePendingChunks(languageCode, createdAtNanos)),
				CompletableFuture.delayedExecutor(PENDING_REMOTE_LANGUAGE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
		);
	}

	private static void removePendingChunks(String languageCode) {
		Map<String, PendingRemoteLanguage> pendingChunks = new HashMap<>(PENDING_REMOTE_LANGUAGE_CHUNKS.get());
		pendingChunks.remove(languageCode);
		PENDING_REMOTE_LANGUAGE_CHUNKS.set(Map.copyOf(pendingChunks));
	}

	private static void removePendingChunks(String languageCode, long createdAtNanos) {
		PendingRemoteLanguage pendingLanguage = PENDING_REMOTE_LANGUAGE_CHUNKS.get().get(languageCode);
		if (pendingLanguage == null || pendingLanguage.createdAtNanos() != createdAtNanos) {
			return;
		}

		removePendingChunks(languageCode);
		AmagariTranslationTool.LOGGER.warn("Discarded incomplete world language data for {} after {} seconds", languageCode, PENDING_REMOTE_LANGUAGE_TIMEOUT.toSeconds());
	}

	private static final class PendingRemoteLanguage {
		private final String hash;
		private final int uncompressedBytes;
		private final int entries;
		private final int totalCompressedBytes;
		private final long createdAtNanos;
		private final byte[][] chunks;
		private int receivedChunks;
		private int receivedBytes;

		private PendingRemoteLanguage(WorldLanguageDataPayload payload) {
			this.hash = payload.hash();
			this.uncompressedBytes = payload.uncompressedBytes();
			this.entries = payload.entries();
			this.totalCompressedBytes = payload.totalCompressedBytes();
			this.createdAtNanos = System.nanoTime();
			this.chunks = new byte[payload.chunkCount()][];
		}

		private void add(WorldLanguageDataPayload payload) {
			if (!hash.equals(payload.hash())
					|| uncompressedBytes != payload.uncompressedBytes()
					|| entries != payload.entries()
					|| totalCompressedBytes != payload.totalCompressedBytes()
					|| chunks.length != payload.chunkCount()) {
				throw new IllegalArgumentException("Mismatched world language chunk metadata");
			}

			if (chunks[payload.chunkIndex()] != null) {
				return;
			}

			chunks[payload.chunkIndex()] = payload.chunkData();
			receivedChunks++;
			receivedBytes += chunks[payload.chunkIndex()].length;
		}

		private boolean complete() {
			return receivedChunks == chunks.length && receivedBytes == totalCompressedBytes;
		}

		private long createdAtNanos() {
			return createdAtNanos;
		}

		private byte[] assemble() {
			if (!complete()) {
				throw new IllegalStateException("World language data is incomplete");
			}

			byte[] compressedData = new byte[totalCompressedBytes];
			int offset = 0;
			for (byte[] chunk : chunks) {
				System.arraycopy(chunk, 0, compressedData, offset, chunk.length);
				offset += chunk.length;
			}
			return Arrays.copyOf(compressedData, compressedData.length);
		}
	}
}
