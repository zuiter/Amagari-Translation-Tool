package com.amagari.translationtool.client;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.network.WorldLanguageDataPayload;
import com.amagari.translationtool.network.WorldLanguageManifestPayload;
import com.amagari.translationtool.translation.WorldLanguageFiles;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class WorldLanguageContext {
	private static final AtomicReference<Path> CURRENT_WORLD_DIRECTORY = new AtomicReference<>();
	private static final AtomicReference<Map<String, Map<String, String>>> REMOTE_TRANSLATIONS = new AtomicReference<>(Map.of());
	private static final AtomicReference<Map<String, WorldLanguageManifestPayload.LanguageManifestEntry>> REMOTE_MANIFEST = new AtomicReference<>(Map.of());
	private static final AtomicReference<Map<String, String>> PENDING_REMOTE_LANGUAGES = new AtomicReference<>(Map.of());
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
		REMOTE_READY.set(true);
		LAST_REPORT.set(WorldLanguageReport.remoteManifest(manifest.size(), cacheLookup.cachedTranslations().size(), cacheLookup.missingLanguages().size()));
		return cacheLookup.missingLanguages();
	}

	public static void receiveRemoteLanguageData(Minecraft client, WorldLanguageDataPayload payload) {
		WorldLanguageManifestPayload.LanguageManifestEntry manifestEntry = REMOTE_MANIFEST.get().get(payload.languageCode());
		if (manifestEntry == null || !manifestEntry.hash().equals(payload.hash())) {
			AmagariTranslationTool.LOGGER.warn("Ignored unexpected world language data for {}", payload.languageCode());
			return;
		}

		try {
			Map<String, String> translations = WorldLanguageCache.store(client, REMOTE_SERVER_KEY.get(), payload);
			Map<String, Map<String, String>> remoteTranslations = new HashMap<>(REMOTE_TRANSLATIONS.get());
			remoteTranslations.put(payload.languageCode(), translations);
			REMOTE_TRANSLATIONS.set(copyTranslations(remoteTranslations));

			Map<String, String> pendingLanguages = new HashMap<>(PENDING_REMOTE_LANGUAGES.get());
			pendingLanguages.remove(payload.languageCode());
			PENDING_REMOTE_LANGUAGES.set(Map.copyOf(pendingLanguages));
			LAST_REPORT.set(WorldLanguageReport.remote(REMOTE_TRANSLATIONS.get()));
		} catch (Exception exception) {
			AmagariTranslationTool.LOGGER.warn("Could not store world language data for {}", payload.languageCode(), exception);
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
			return;
		}

		Optional<Path> worldDirectory = getWorldDirectory();
		if (worldDirectory.isEmpty()) {
			LAST_REPORT.set(WorldLanguageReport.notLoaded());
			return;
		}

		WorldLanguageReport report = WorldLanguageReport.local(WorldLanguageFiles.loadSelectedInto(worldDirectory.get(), languageCodes, translations));
		LAST_REPORT.set(report);
		report.log();
	}

	public static String describeLastReport() {
		return LAST_REPORT.get().describe();
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
		REMOTE_SERVER_KEY.set("");
		REMOTE_READY.set(false);
	}
}
