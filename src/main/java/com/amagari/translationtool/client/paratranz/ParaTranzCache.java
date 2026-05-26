package com.amagari.translationtool.client.paratranz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class ParaTranzCache {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CACHE_DIRECTORY = "amagari_translation_tool/paratranz_cache";

	private ParaTranzCache() {
	}

	public static CachedTranslations store(Path gameDirectory, ParaTranzProject project, ParaTranzArtifact artifact, byte[] zipData, ParaTranzConfig config) throws IOException {
		ParaTranzZipTranslations.ParseResult translations = ParaTranzZipTranslations.parse(zipData)
				.mappedLanguage(config.sourceLanguage(), config.targetLanguage());
		Path projectDirectory = gameDirectory.resolve(CACHE_DIRECTORY).resolve(Integer.toString(project.id()));
		Files.createDirectories(projectDirectory);
		Files.write(projectDirectory.resolve("artifact-" + artifact.id() + ".zip"), zipData);
		Files.writeString(projectDirectory.resolve("metadata.json"), GSON.toJson(CacheMetadata.from(project, artifact, translations)), StandardCharsets.UTF_8);
		cleanupProjectCache(projectDirectory, config.maxCachedArtifacts());
		return new CachedTranslations(project, artifact, translations, config);
	}

	private static void cleanupProjectCache(Path projectDirectory, int maxCachedArtifacts) throws IOException {
		if (maxCachedArtifacts <= 0 || !Files.isDirectory(projectDirectory)) {
			return;
		}

		try (Stream<Path> artifacts = Files.list(projectDirectory)) {
			List<Path> cachedArtifacts = artifacts
					.filter(path -> path.getFileName().toString().startsWith("artifact-") && path.getFileName().toString().endsWith(".zip"))
					.sorted(Comparator.comparingLong(ParaTranzCache::lastModifiedMillis).reversed())
					.toList();
			for (int index = maxCachedArtifacts; index < cachedArtifacts.size(); index++) {
				Files.deleteIfExists(cachedArtifacts.get(index));
			}
		}
	}

	private static long lastModifiedMillis(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException exception) {
			return 0L;
		}
	}

	public record CachedTranslations(
			ParaTranzProject project,
			ParaTranzArtifact artifact,
			ParaTranzZipTranslations.ParseResult translations,
			ParaTranzConfig config
	) {
	}

	private record CacheMetadata(
			int projectId,
			String projectName,
			int artifactId,
			String artifactCreatedAt,
			int loadedFiles,
			int failedFiles,
			int loadedEntries,
			List<String> languageCodes
	) {
		private static CacheMetadata from(ParaTranzProject project, ParaTranzArtifact artifact, ParaTranzZipTranslations.ParseResult translations) {
			return new CacheMetadata(
					project.id(),
					project.name(),
					artifact.id(),
					artifact.createdAt().toString(),
					translations.loadedFiles(),
					translations.failedFiles(),
					translations.loadedEntries(),
					translations.translationsByLanguage().keySet().stream().sorted().toList()
			);
		}
	}
}
