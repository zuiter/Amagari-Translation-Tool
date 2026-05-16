package com.amagari.translationtool.translation;

import com.amagari.translationtool.AmagariTranslationTool;
import net.minecraft.locale.Language;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;

public final class WorldLanguageFiles {
	public static final String LANG_DIRECTORY = "amagari_translation_tool/lang";

	private static final String JSON_EXTENSION = ".json";

	private WorldLanguageFiles() {
	}

	public static WorldLanguageResult loadSelectedInto(Path worldDirectory, List<String> languageCodes, Map<String, String> translations) {
		Path languageDirectory = worldDirectory.resolve(LANG_DIRECTORY);
		if (!Files.isDirectory(languageDirectory)) {
			return WorldLanguageResult.empty(worldDirectory);
		}

		StringJoiner loadedLanguages = new StringJoiner(", ");
		int totalFiles = 0;
		int totalEntries = 0;
		int totalFailedFiles = 0;
		for (String languageCode : languageCodes) {
			LanguageLoadResult result = loadLanguage(languageDirectory, languageCode, translations);
			if (result.failedToScan()) {
				return WorldLanguageResult.failed(worldDirectory, languageDirectory, result.error());
			}
			totalFiles += result.loadedFiles();
			totalEntries += result.loadedEntries();
			totalFailedFiles += result.failedFiles();
			if (result.loadedFiles() > 0 || result.failedFiles() > 0) {
				loadedLanguages.add(result.languageCode());
			}
		}

		return WorldLanguageResult.loaded(worldDirectory, loadedLanguages.toString(), languageDirectory, totalFiles, totalEntries, totalFailedFiles);
	}

	public static WorldLanguageCollection loadAll(Path worldDirectory) {
		Path languageDirectory = worldDirectory.resolve(LANG_DIRECTORY);
		if (!Files.isDirectory(languageDirectory)) {
			return WorldLanguageCollection.empty(worldDirectory);
		}

		List<Path> languageFiles;
		try (Stream<Path> files = Files.list(languageDirectory)) {
			languageFiles = files
					.filter(Files::isRegularFile)
					.filter(WorldLanguageFiles::isJsonFile)
					.sorted(Comparator.comparing(path -> path.getFileName().toString()))
					.toList();
		} catch (IOException exception) {
			return WorldLanguageCollection.failed(worldDirectory, languageDirectory, exception);
		}

		Map<String, Map<String, String>> translationsByLanguage = new LinkedHashMap<>();
		int failedFiles = 0;
		for (Path languageFile : languageFiles) {
			String languageCode = languageCode(languageFile);
			Map<String, String> translations = translationsByLanguage.computeIfAbsent(languageCode, ignored -> new LinkedHashMap<>());
			try (InputStream input = Files.newInputStream(languageFile)) {
				Language.loadFromJson(input, translations::put);
			} catch (IOException | RuntimeException exception) {
				failedFiles++;
				AmagariTranslationTool.LOGGER.warn("Skipped world language file {}", languageFile, exception);
			}
		}

		return WorldLanguageCollection.loaded(worldDirectory, languageDirectory, translationsByLanguage, languageFiles.size(), failedFiles);
	}

	private static LanguageLoadResult loadLanguage(Path languageDirectory, String languageCode, Map<String, String> translations) {
		List<Path> languageFiles;
		try (Stream<Path> files = Files.list(languageDirectory)) {
			languageFiles = files
					.filter(Files::isRegularFile)
					.filter(path -> isLanguageFile(path, languageCode))
					.sorted(Comparator.comparing(path -> path.getFileName().toString()))
					.toList();
		} catch (IOException exception) {
			return LanguageLoadResult.failedToScan(languageCode, exception);
		}

		int loadedEntries = 0;
		int failedFiles = 0;
		for (Path languageFile : languageFiles) {
			Map<String, String> fileTranslations = new LinkedHashMap<>();
			try (InputStream input = Files.newInputStream(languageFile)) {
				Language.loadFromJson(input, fileTranslations::put);
				translations.putAll(fileTranslations);
				loadedEntries += fileTranslations.size();
			} catch (IOException | RuntimeException exception) {
				failedFiles++;
				AmagariTranslationTool.LOGGER.warn("Skipped world language file {}", languageFile, exception);
			}
		}

		return LanguageLoadResult.loaded(languageCode, languageFiles.size(), loadedEntries, failedFiles);
	}

	private static boolean isLanguageFile(Path path, String languageCode) {
		String fileName = path.getFileName().toString();
		if (!fileName.endsWith(JSON_EXTENSION)) {
			return false;
		}

		String baseName = fileName.substring(0, fileName.length() - JSON_EXTENSION.length());
		return baseName.equals(languageCode) || baseName.startsWith(languageCode + ".");
	}

	private static boolean isJsonFile(Path path) {
		return path.getFileName().toString().endsWith(JSON_EXTENSION);
	}

	private static String languageCode(Path path) {
		String fileName = path.getFileName().toString();
		String baseName = fileName.substring(0, fileName.length() - JSON_EXTENSION.length());
		int splitIndex = baseName.indexOf('.');
		return splitIndex < 0 ? baseName : baseName.substring(0, splitIndex);
	}

	private record LanguageLoadResult(
			String languageCode,
			int loadedFiles,
			int loadedEntries,
			int failedFiles,
			Exception error
	) {
		private static LanguageLoadResult loaded(String languageCode, int loadedFiles, int loadedEntries, int failedFiles) {
			return new LanguageLoadResult(languageCode, loadedFiles, loadedEntries, failedFiles, null);
		}

		private static LanguageLoadResult failedToScan(String languageCode, Exception error) {
			return new LanguageLoadResult(languageCode, 0, 0, 0, error);
		}

		private boolean failedToScan() {
			return error != null;
		}
	}

	public record WorldLanguageResult(
			Path worldDirectory,
			String languageCode,
			Path languageDirectory,
			int loadedFiles,
			int loadedEntries,
			int failedFiles,
			String error
	) {
		private static WorldLanguageResult empty(Path worldDirectory) {
			return new WorldLanguageResult(worldDirectory, "", worldDirectory.resolve(LANG_DIRECTORY), 0, 0, 0, "");
		}

		private static WorldLanguageResult failed(Path worldDirectory, Path languageDirectory, Exception exception) {
			return new WorldLanguageResult(worldDirectory, "", languageDirectory, 0, 0, 0, exception.toString());
		}

		private static WorldLanguageResult loaded(Path worldDirectory, String languageCode, Path languageDirectory, int loadedFiles, int loadedEntries, int failedFiles) {
			return new WorldLanguageResult(worldDirectory, languageCode, languageDirectory, loadedFiles, loadedEntries, failedFiles, "");
		}

		public boolean failed() {
			return !error.isBlank();
		}
	}

	public record WorldLanguageCollection(
			Path worldDirectory,
			Path languageDirectory,
			Map<String, Map<String, String>> translationsByLanguage,
			int loadedFiles,
			int loadedEntries,
			int failedFiles,
			String error
	) {
		public static WorldLanguageCollection empty(Path worldDirectory) {
			return new WorldLanguageCollection(worldDirectory, worldDirectory.resolve(LANG_DIRECTORY), Map.of(), 0, 0, 0, "");
		}

		public static WorldLanguageCollection failed(Path worldDirectory, Path languageDirectory, Exception exception) {
			return new WorldLanguageCollection(worldDirectory, languageDirectory, Map.of(), 0, 0, 0, exception.toString());
		}

		public static WorldLanguageCollection loaded(Path worldDirectory, Path languageDirectory, Map<String, Map<String, String>> translationsByLanguage, int loadedFiles, int failedFiles) {
			Map<String, Map<String, String>> copiedTranslations = new LinkedHashMap<>();
			int loadedEntries = 0;
			for (Map.Entry<String, Map<String, String>> entry : translationsByLanguage.entrySet()) {
				Map<String, String> languageTranslations = Map.copyOf(entry.getValue());
				copiedTranslations.put(entry.getKey(), languageTranslations);
				loadedEntries += languageTranslations.size();
			}
			return new WorldLanguageCollection(worldDirectory, languageDirectory, Map.copyOf(copiedTranslations), loadedFiles, loadedEntries, failedFiles, "");
		}

		public boolean failed() {
			return !error.isBlank();
		}

		public boolean hasTranslations() {
			return !translationsByLanguage.isEmpty();
		}

		public WorldLanguageCollection filterFor(String languageCode) {
			Map<String, Map<String, String>> filtered = new HashMap<>();
			addIfPresent(filtered, "en_us");
			addIfPresent(filtered, languageCode);
			return loaded(worldDirectory, languageDirectory, filtered, loadedFiles, failedFiles);
		}

		private void addIfPresent(Map<String, Map<String, String>> filtered, String languageCode) {
			Map<String, String> translations = translationsByLanguage.get(languageCode);
			if (translations != null) {
				filtered.put(languageCode, translations);
			}
		}
	}
}
