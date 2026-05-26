package com.amagari.translationtool.client.paratranz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public record ParaTranzConfig(
		String paratranzApiToken,
		String sourceLanguage,
		String targetLanguage,
		boolean triggerExport,
		int maxCachedArtifacts,
		boolean overwriteWorldLanguageFiles
) {
	public static final String DEFAULT_API_TOKEN = "";
	public static final String DEFAULT_SOURCE_LANGUAGE = "en_us";
	public static final String DEFAULT_TARGET_LANGUAGE = "zh_cn";
	public static final boolean DEFAULT_TRIGGER_EXPORT = true;
	public static final int DEFAULT_MAX_CACHED_ARTIFACTS = 1;
	public static final boolean DEFAULT_OVERWRITE_WORLD_LANGUAGE_FILES = false;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIRECTORY = Path.of("config", "amagari_lang");
	private static final String CONFIG_FILE = "config.json";
	private static final String LEGACY_CONFIG_FILE = "amagari_translation_tool.json";

	public static ParaTranzConfig defaultConfig() {
		return new ParaTranzConfig(
				DEFAULT_API_TOKEN,
				DEFAULT_SOURCE_LANGUAGE,
				DEFAULT_TARGET_LANGUAGE,
				DEFAULT_TRIGGER_EXPORT,
				DEFAULT_MAX_CACHED_ARTIFACTS,
				DEFAULT_OVERWRITE_WORLD_LANGUAGE_FILES
		);
	}

	public static ParaTranzConfig load(Path gameDirectory) throws IOException {
		Path configPath = configPath(gameDirectory);
		if (Files.notExists(configPath)) {
			ParaTranzConfig config = Files.exists(legacyConfigPath(gameDirectory))
					? read(legacyConfigPath(gameDirectory))
					: defaultConfig();
			writeConfig(configPath, config);
			return config;
		}

		return read(configPath);
	}

	public static void save(Path gameDirectory, ParaTranzConfig config) throws IOException {
		writeConfig(configPath(gameDirectory), config);
	}

	public static Path configPath(Path gameDirectory) {
		return gameDirectory.resolve(CONFIG_DIRECTORY).resolve(CONFIG_FILE);
	}

	private static Path legacyConfigPath(Path gameDirectory) {
		return gameDirectory.resolve("config").resolve(LEGACY_CONFIG_FILE);
	}

	private static ParaTranzConfig read(Path configPath) throws IOException {
		try {
			String json = Files.readString(configPath, StandardCharsets.UTF_8);
			ParaTranzConfig config = GSON.fromJson(json, ParaTranzConfig.class);
			if (config == null || config.paratranzApiToken == null) {
				return defaultConfig();
			}
			return new ParaTranzConfig(
					config.paratranzApiToken(),
					normalizedLanguage(config.sourceLanguage(), DEFAULT_SOURCE_LANGUAGE),
					normalizedLanguage(config.targetLanguage(), DEFAULT_TARGET_LANGUAGE),
					config.triggerExport(),
					config.maxCachedArtifacts() <= 0 ? DEFAULT_MAX_CACHED_ARTIFACTS : config.maxCachedArtifacts(),
					config.overwriteWorldLanguageFiles()
			);
		} catch (JsonSyntaxException exception) {
			throw new IOException("config file is not valid JSON", exception);
		}
	}

	private static String normalizedLanguage(String language, String fallback) {
		if (language == null || language.isBlank()) {
			return fallback;
		}
		return language.trim().toLowerCase().replace('-', '_');
	}

	private static void writeConfig(Path configPath, ParaTranzConfig config) throws IOException {
		Files.createDirectories(configPath.getParent());
		Files.writeString(configPath, GSON.toJson(config), StandardCharsets.UTF_8);
	}
}
