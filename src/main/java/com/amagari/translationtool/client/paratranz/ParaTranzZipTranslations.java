package com.amagari.translationtool.client.paratranz;

import net.minecraft.locale.Language;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ParaTranzZipTranslations {
	private static final String JSON_EXTENSION = ".json";
	private static final int MAX_ENTRY_BYTES = 16 * 1024 * 1024;

	private ParaTranzZipTranslations() {
	}

	public static Optional<String> languageCode(String zipPath) {
		if (zipPath == null || !zipPath.endsWith(JSON_EXTENSION)) {
			return Optional.empty();
		}
		String normalizedPath = zipPath.replace('\\', '/');
		String fileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
		String baseName = fileName.substring(0, fileName.length() - JSON_EXTENSION.length());
		int splitIndex = baseName.indexOf('.');
		String languageCode = splitIndex < 0 ? baseName : baseName.substring(0, splitIndex);
		return languageCode.isBlank() ? Optional.empty() : Optional.of(languageCode);
	}

	public static ParseResult parse(byte[] zipData) throws IOException {
		Map<String, Map<String, String>> translationsByLanguage = new LinkedHashMap<>();
		int loadedFiles = 0;
		int failedFiles = 0;

		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipData))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}

				Optional<String> languageCode = languageCode(entry.getName());
				if (languageCode.isEmpty()) {
					continue;
				}

				try {
					Map<String, String> fileTranslations = loadTranslations(readEntry(zip));
					translationsByLanguage
							.computeIfAbsent(languageCode.get(), ignored -> new LinkedHashMap<>())
							.putAll(fileTranslations);
					loadedFiles++;
				} catch (IOException | RuntimeException exception) {
					failedFiles++;
				}
			}
		}

		return ParseResult.loaded(translationsByLanguage, loadedFiles, failedFiles);
	}

	private static byte[] readEntry(ZipInputStream zip) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int totalBytes = 0;
		int readBytes;
		while ((readBytes = zip.read(buffer)) >= 0) {
			totalBytes += readBytes;
			if (totalBytes > MAX_ENTRY_BYTES) {
				throw new IOException("ParaTranz zip entry is too large");
			}
			bytes.write(buffer, 0, readBytes);
		}
		return bytes.toByteArray();
	}

	private static Map<String, String> loadTranslations(byte[] json) throws IOException {
		Map<String, String> translations = new LinkedHashMap<>();
		try (ByteArrayInputStream input = new ByteArrayInputStream(json)) {
			Language.loadFromJson(input, translations::put);
		}
		return translations;
	}

	public record ParseResult(
			Map<String, Map<String, String>> translationsByLanguage,
			int loadedFiles,
			int loadedEntries,
			int failedFiles
	) {
		private static ParseResult loaded(Map<String, Map<String, String>> translationsByLanguage, int loadedFiles, int failedFiles) {
			Map<String, Map<String, String>> copiedTranslations = new LinkedHashMap<>();
			int loadedEntries = 0;
			for (Map.Entry<String, Map<String, String>> entry : translationsByLanguage.entrySet()) {
				Map<String, String> languageTranslations = Map.copyOf(entry.getValue());
				copiedTranslations.put(entry.getKey(), languageTranslations);
				loadedEntries += languageTranslations.size();
			}
			return new ParseResult(Map.copyOf(copiedTranslations), loadedFiles, loadedEntries, failedFiles);
		}

		public Map<String, String> mergedForLanguages(Iterable<String> languageCodes) {
			Map<String, String> translations = new HashMap<>();
			for (String languageCode : languageCodes) {
				Map<String, String> languageTranslations = translationsByLanguage.get(languageCode);
				if (languageTranslations != null) {
					translations.putAll(languageTranslations);
				}
			}
			return translations;
		}
	}
}
