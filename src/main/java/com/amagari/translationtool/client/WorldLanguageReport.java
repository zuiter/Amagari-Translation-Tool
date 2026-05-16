package com.amagari.translationtool.client;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.translation.WorldLanguageFiles;

import java.nio.file.Path;
import java.util.Map;

public record WorldLanguageReport(
		Status status,
		Path worldDirectory,
		String languageCode,
		Path languageDirectory,
		int loadedFiles,
		int loadedEntries,
		int failedFiles,
		String error
) {
	public static WorldLanguageReport notLoaded() {
		return new WorldLanguageReport(Status.NOT_LOADED, null, "", null, 0, 0, 0, "");
	}

	public static WorldLanguageReport empty(Path worldDirectory) {
		return new WorldLanguageReport(Status.EMPTY, worldDirectory, "", worldDirectory.resolve(com.amagari.translationtool.translation.WorldLanguageFiles.LANG_DIRECTORY), 0, 0, 0, "");
	}

	public static WorldLanguageReport failed(Path worldDirectory, String languageCode, Path languageDirectory, Exception exception) {
		return new WorldLanguageReport(Status.FAILED, worldDirectory, languageCode, languageDirectory, 0, 0, 0, exception.toString());
	}

	public static WorldLanguageReport loaded(Path worldDirectory, String languageCode, Path languageDirectory, int loadedFiles, int loadedEntries, int failedFiles) {
		Status status = loadedFiles == 0 ? Status.EMPTY : Status.LOADED;
		return new WorldLanguageReport(status, worldDirectory, languageCode, languageDirectory, loadedFiles, loadedEntries, failedFiles, "");
	}

	public static WorldLanguageReport local(WorldLanguageFiles.WorldLanguageResult result) {
		if (result.failed()) {
			return new WorldLanguageReport(Status.FAILED, result.worldDirectory(), result.languageCode(), result.languageDirectory(), 0, 0, 0, result.error());
		}
		return loaded(result.worldDirectory(), result.languageCode(), result.languageDirectory(), result.loadedFiles(), result.loadedEntries(), result.failedFiles());
	}

	public static WorldLanguageReport remote(Map<String, Map<String, String>> translationsByLanguage) {
		int loadedEntries = translationsByLanguage.values().stream().mapToInt(Map::size).sum();
		return new WorldLanguageReport(Status.REMOTE_READY, null, "", null, translationsByLanguage.size(), loadedEntries, 0, "");
	}

	public static WorldLanguageReport remoteManifest(int languageCount, int cachedLanguageCount, int pendingLanguageCount) {
		return new WorldLanguageReport(Status.REMOTE_MANIFEST, null, "", null, languageCount, cachedLanguageCount, pendingLanguageCount, Integer.toString(pendingLanguageCount));
	}

	public static WorldLanguageReport loadedRemote(int loadedLanguages, int loadedEntries) {
		return new WorldLanguageReport(Status.REMOTE_LOADED, null, "", null, loadedLanguages, loadedEntries, 0, "");
	}

	public String describe() {
		return switch (status) {
			case NOT_LOADED -> "No singleplayer world language directory is active.";
			case EMPTY -> "No world language files loaded from " + languageDirectory + ".";
			case LOADED -> "Loaded " + loadedEntries + " translation entries from " + loadedFiles + " world language file(s)"
					+ (failedFiles == 0 ? "" : "; " + failedFiles + " file(s) failed")
					+ (languageCode.isBlank() ? "." : " for " + languageCode + ".");
			case REMOTE_READY -> "Received " + loadedEntries + " remote world language entries for " + loadedFiles + " language(s).";
			case REMOTE_MANIFEST -> "Received remote language manifest for " + loadedFiles + " language(s); "
					+ loadedEntries + " cached, " + failedFiles + " pending download.";
			case REMOTE_LOADED -> "Loaded " + loadedEntries + " remote world language entries for " + loadedFiles + " active language(s).";
			case FAILED -> "Failed to scan world language directory " + languageDirectory + ": " + error;
		};
	}

	public void log() {
		if (status == Status.LOADED) {
			AmagariTranslationTool.LOGGER.info(describe());
		} else if (status == Status.FAILED) {
			AmagariTranslationTool.LOGGER.warn(describe());
		}
	}

	public enum Status {
		NOT_LOADED,
		EMPTY,
		LOADED,
		REMOTE_READY,
		REMOTE_MANIFEST,
		REMOTE_LOADED,
		FAILED
	}
}
