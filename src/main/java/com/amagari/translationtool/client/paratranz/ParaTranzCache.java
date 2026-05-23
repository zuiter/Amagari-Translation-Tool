package com.amagari.translationtool.client.paratranz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ParaTranzCache {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CACHE_DIRECTORY = "amagari_translation_tool/paratranz_cache";

	private ParaTranzCache() {
	}

	public static CachedTranslations store(Path gameDirectory, ParaTranzProject project, ParaTranzArtifact artifact, byte[] zipData) throws IOException {
		ParaTranzZipTranslations.ParseResult translations = ParaTranzZipTranslations.parse(zipData);
		Path projectDirectory = gameDirectory.resolve(CACHE_DIRECTORY).resolve(Integer.toString(project.id()));
		Files.createDirectories(projectDirectory);
		Files.write(projectDirectory.resolve("artifact-" + artifact.id() + ".zip"), zipData);
		Files.writeString(projectDirectory.resolve("metadata.json"), GSON.toJson(CacheMetadata.from(project, artifact, translations)), StandardCharsets.UTF_8);
		return new CachedTranslations(project, artifact, translations);
	}

	public record CachedTranslations(
			ParaTranzProject project,
			ParaTranzArtifact artifact,
			ParaTranzZipTranslations.ParseResult translations
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
