package com.amagari.translationtool.translation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.locale.Language;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class WorldResourcePackLanguages {
	public static final String WORLD_RESOURCE_PACK_ZIP = "resources.zip";
	public static final String WORLD_RESOURCE_PACK_DIRECTORY = "resources";

	private static final String ASSETS_PREFIX = "assets/";
	private static final String DEFAULT_NAMESPACE = "amagari_translation_tool";
	private static final String MINECRAFT_NAMESPACE = "minecraft";
	private static final String LANG_SEGMENT = "/lang/";
	private static final String JSON_EXTENSION = ".json";
	private static final String BACKUP_SUFFIX = ".att-backup";
	private static final String PENDING_SUFFIX = ".att-pending";
	private static final int MAX_LANGUAGE_FILE_BYTES = 16 * 1024 * 1024;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private WorldResourcePackLanguages() {
	}

	public static Optional<WriteResult> writeTargetLanguage(
			Path worldDirectory,
			String languageCode,
			Map<String, String> translations
	) throws IOException {
		String normalizedLanguageCode = normalizedLanguageCode(languageCode);
		if (translations == null || translations.isEmpty()) {
			throw new IOException("translations are empty");
		}

		Path zipResourcePack = worldDirectory.resolve(WORLD_RESOURCE_PACK_ZIP);
		if (Files.isRegularFile(zipResourcePack)) {
			Path pendingResourcePack = pendingPath(zipResourcePack);
			Path mergeSource = preferredMergeSource(zipResourcePack, pendingResourcePack);
			return Optional.of(writeZipResourcePack(zipResourcePack, mergeSource, normalizedLanguageCode, translations));
		}

		Path directoryResourcePack = worldDirectory.resolve(WORLD_RESOURCE_PACK_DIRECTORY);
		if (Files.isDirectory(directoryResourcePack)) {
			return Optional.of(writeDirectoryResourcePack(directoryResourcePack, normalizedLanguageCode, translations));
		}

		return Optional.empty();
	}

	private static WriteResult writeDirectoryResourcePack(
			Path resourcePack,
			String languageCode,
			Map<String, String> translations
	) throws IOException {
		ResourcePackLayout layout = scanDirectoryLayout(resourcePack, languageCode);
		List<String> targetFiles = targetLanguageFiles(layout, languageCode);
		List<String> updatedFiles = new ArrayList<>();

		for (String targetFile : targetFiles) {
			Path languageFile = resourcePack.resolve(targetFile);
			Files.createDirectories(languageFile.getParent());
			Map<String, String> mergedTranslations = Files.isRegularFile(languageFile)
					? mergedTranslations(readLimited(Files.newInputStream(languageFile)), translations)
					: new LinkedHashMap<>(translations);
			writeDirectoryLanguageFile(languageFile, mergedTranslations);
			updatedFiles.add(targetFile);
		}

		return new WriteResult(resourcePack, List.copyOf(updatedFiles), false, false);
	}

	private static void writeDirectoryLanguageFile(Path languageFile, Map<String, String> translations) throws IOException {
		if (Files.isRegularFile(languageFile)) {
			Files.copy(languageFile, backupPath(languageFile), StandardCopyOption.REPLACE_EXISTING);
		}

		Path temporaryFile = Files.createTempFile(languageFile.getParent(), languageFile.getFileName().toString() + "-", ".tmp");
		try {
			Files.writeString(temporaryFile, GSON.toJson(translations), StandardCharsets.UTF_8);
			moveReplacing(temporaryFile, languageFile);
		} finally {
			Files.deleteIfExists(temporaryFile);
		}
	}

	private static WriteResult writeZipResourcePack(
			Path resourcePack,
			Path mergeSource,
			String languageCode,
			Map<String, String> translations
	) throws IOException {
		ResourcePackLayout layout = scanZipLayout(mergeSource, languageCode);
		List<String> targetFiles = targetLanguageFiles(layout, languageCode);
		Set<String> targetFileSet = Set.copyOf(targetFiles);
		Path temporaryZip = Files.createTempFile(resourcePack.getParent(), resourcePack.getFileName().toString() + "-", ".tmp");

		try {
			Set<String> writtenEntries = new LinkedHashSet<>();
			try (ZipFile sourceZip = new ZipFile(mergeSource.toFile());
				 ZipOutputStream targetZip = new ZipOutputStream(Files.newOutputStream(temporaryZip))) {
				var entries = sourceZip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry sourceEntry = entries.nextElement();
					String sourceEntryName = sourceEntry.getName();
					String entryName = normalizedZipPath(sourceEntryName);
					if (!writtenEntries.add(entryName)) {
						throw new IOException("duplicate zip entry: " + entryName);
					}

					ZipEntry targetEntry = copiedEntry(sourceEntry, sourceEntryName);
					targetZip.putNextEntry(targetEntry);
					if (!sourceEntry.isDirectory()) {
						if (targetFileSet.contains(entryName)) {
							byte[] existingJson = readLimited(sourceZip.getInputStream(sourceEntry));
							targetZip.write(GSON.toJson(mergedTranslations(existingJson, translations)).getBytes(StandardCharsets.UTF_8));
						} else {
							try (InputStream input = sourceZip.getInputStream(sourceEntry)) {
								input.transferTo(targetZip);
							}
						}
					}
					targetZip.closeEntry();
				}

				for (String targetFile : targetFiles) {
					if (writtenEntries.contains(targetFile)) {
						continue;
					}
					targetZip.putNextEntry(new ZipEntry(targetFile));
					targetZip.write(GSON.toJson(translations).getBytes(StandardCharsets.UTF_8));
					targetZip.closeEntry();
				}
			}

			Files.copy(resourcePack, backupPath(resourcePack), StandardCopyOption.REPLACE_EXISTING);
			Path pendingResourcePack = pendingPath(resourcePack);
			boolean pendingReplacement = false;
			try {
				moveReplacing(temporaryZip, resourcePack);
			} catch (IOException replacementException) {
				try {
					replacePendingPreservingExisting(temporaryZip, pendingResourcePack);
					pendingReplacement = true;
				} catch (IOException pendingException) {
					replacementException.addSuppressed(pendingException);
					throw replacementException;
				}
			}
			if (!pendingReplacement) {
				Files.deleteIfExists(pendingResourcePack);
			}
			return new WriteResult(resourcePack, targetFiles, true, pendingReplacement);
		} finally {
			Files.deleteIfExists(temporaryZip);
		}
	}

	public static boolean finishPendingWrite(Path worldDirectory) throws IOException {
		Path resourcePack = worldDirectory.resolve(WORLD_RESOURCE_PACK_ZIP);
		Path pendingResourcePack = pendingPath(resourcePack);
		if (!Files.isRegularFile(pendingResourcePack)) {
			return false;
		}
		if (Files.isRegularFile(resourcePack)
				&& Files.getLastModifiedTime(resourcePack).compareTo(Files.getLastModifiedTime(pendingResourcePack)) > 0) {
			Files.deleteIfExists(pendingResourcePack);
			return false;
		}
		if (Files.isRegularFile(resourcePack)) {
			Files.copy(resourcePack, backupPath(resourcePack), StandardCopyOption.REPLACE_EXISTING);
		}
		moveReplacing(pendingResourcePack, resourcePack);
		return true;
	}

	public static int finishPendingWrites(Path savesDirectory) throws IOException {
		if (!Files.isDirectory(savesDirectory)) {
			return 0;
		}

		int completedWrites = 0;
		try (var worlds = Files.list(savesDirectory)) {
			for (Path worldDirectory : worlds.filter(Files::isDirectory).toList()) {
				if (finishPendingWrite(worldDirectory)) {
					completedWrites++;
				}
			}
		}
		return completedWrites;
	}

	private static ResourcePackLayout scanDirectoryLayout(Path resourcePack, String languageCode) throws IOException {
		Path assetsDirectory = resourcePack.resolve("assets");
		if (!Files.isDirectory(assetsDirectory)) {
			return ResourcePackLayout.empty();
		}

		Set<String> namespaces = new LinkedHashSet<>();
		Set<String> languageDirectories = new LinkedHashSet<>();
		Set<String> targetLanguageFiles = new LinkedHashSet<>();
		try (var namespacePaths = Files.list(assetsDirectory)) {
			for (Path namespacePath : namespacePaths.filter(Files::isDirectory).sorted().toList()) {
				String namespace = namespacePath.getFileName().toString();
				namespaces.add(namespace);
				Path languageDirectory = namespacePath.resolve("lang");
				if (!Files.isDirectory(languageDirectory)) {
					continue;
				}

				String relativeLanguageDirectory = ASSETS_PREFIX + namespace + "/lang";
				languageDirectories.add(relativeLanguageDirectory);
				Path targetLanguageFile = languageDirectory.resolve(languageCode + JSON_EXTENSION);
				if (Files.isRegularFile(targetLanguageFile)) {
					targetLanguageFiles.add(relativeLanguageDirectory + "/" + languageCode + JSON_EXTENSION);
				}
			}
		}
		return new ResourcePackLayout(namespaces, languageDirectories, targetLanguageFiles);
	}

	private static ResourcePackLayout scanZipLayout(Path resourcePack, String languageCode) throws IOException {
		Set<String> namespaces = new LinkedHashSet<>();
		Set<String> languageDirectories = new LinkedHashSet<>();
		Set<String> targetLanguageFiles = new LinkedHashSet<>();
		try (ZipFile zip = new ZipFile(resourcePack.toFile())) {
			var entries = zip.entries();
			while (entries.hasMoreElements()) {
				String entryName = normalizedZipPath(entries.nextElement().getName());
				Optional<String> namespace = namespace(entryName);
				namespace.ifPresent(namespaces::add);
				Optional<String> languageDirectory = languageDirectory(entryName);
				languageDirectory.ifPresent(languageDirectories::add);
				if (isTargetLanguageFile(entryName, languageCode)) {
					targetLanguageFiles.add(entryName);
				}
			}
		}
		return new ResourcePackLayout(namespaces, languageDirectories, targetLanguageFiles);
	}

	private static List<String> targetLanguageFiles(ResourcePackLayout layout, String languageCode) {
		if (!layout.targetLanguageFiles().isEmpty()) {
			return layout.targetLanguageFiles().stream().sorted().toList();
		}

		String languageDirectory = preferredLanguageDirectory(layout);
		return List.of(languageDirectory + "/" + languageCode + JSON_EXTENSION);
	}

	private static String preferredLanguageDirectory(ResourcePackLayout layout) {
		String minecraftLanguageDirectory = ASSETS_PREFIX + MINECRAFT_NAMESPACE + "/lang";
		if (layout.languageDirectories().contains(minecraftLanguageDirectory)) {
			return minecraftLanguageDirectory;
		}
		if (!layout.languageDirectories().isEmpty()) {
			return layout.languageDirectories().stream().sorted().findFirst().orElseThrow();
		}
		if (layout.namespaces().contains(MINECRAFT_NAMESPACE)) {
			return minecraftLanguageDirectory;
		}
		String namespace = layout.namespaces().stream().sorted().findFirst().orElse(DEFAULT_NAMESPACE);
		return ASSETS_PREFIX + namespace + "/lang";
	}

	private static Map<String, String> mergedTranslations(byte[] existingJson, Map<String, String> translations) throws IOException {
		Map<String, String> mergedTranslations = new LinkedHashMap<>();
		try (ByteArrayInputStream input = new ByteArrayInputStream(existingJson)) {
			Language.loadFromJson(input, mergedTranslations::put);
		} catch (RuntimeException exception) {
			throw new IOException("resource-pack language file is not valid JSON", exception);
		}
		mergedTranslations.putAll(translations);
		return mergedTranslations;
	}

	private static byte[] readLimited(InputStream input) throws IOException {
		try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int totalBytes = 0;
			int readBytes;
			while ((readBytes = input.read(buffer)) >= 0) {
				totalBytes += readBytes;
				if (totalBytes > MAX_LANGUAGE_FILE_BYTES) {
					throw new IOException("resource-pack language file is too large");
				}
				output.write(buffer, 0, readBytes);
			}
			return output.toByteArray();
		}
	}

	private static ZipEntry copiedEntry(ZipEntry sourceEntry, String entryName) {
		ZipEntry copiedEntry = new ZipEntry(entryName);
		if (sourceEntry.getComment() != null) {
			copiedEntry.setComment(sourceEntry.getComment());
		}
		if (sourceEntry.getTime() >= 0) {
			copiedEntry.setTime(sourceEntry.getTime());
		}
		return copiedEntry;
	}

	private static Optional<String> namespace(String zipPath) {
		if (!zipPath.startsWith(ASSETS_PREFIX)) {
			return Optional.empty();
		}
		int namespaceEnd = zipPath.indexOf('/', ASSETS_PREFIX.length());
		if (namespaceEnd < 0) {
			return Optional.empty();
		}
		String namespace = zipPath.substring(ASSETS_PREFIX.length(), namespaceEnd);
		return namespace.isBlank() ? Optional.empty() : Optional.of(namespace);
	}

	private static Optional<String> languageDirectory(String zipPath) {
		Optional<String> namespace = namespace(zipPath);
		if (namespace.isEmpty()) {
			return Optional.empty();
		}
		String languageDirectory = ASSETS_PREFIX + namespace.get() + "/lang";
		return zipPath.equals(languageDirectory)
				|| zipPath.equals(languageDirectory + "/")
				|| zipPath.startsWith(languageDirectory + "/")
				? Optional.of(languageDirectory)
				: Optional.empty();
	}

	private static boolean isTargetLanguageFile(String zipPath, String languageCode) {
		return languageDirectory(zipPath)
				.map(directory -> zipPath.equals(directory + "/" + languageCode + JSON_EXTENSION))
				.orElse(false);
	}

	private static Path preferredMergeSource(Path resourcePack, Path pendingResourcePack) throws IOException {
		if (!Files.isRegularFile(pendingResourcePack)) {
			return resourcePack;
		}
		return Files.getLastModifiedTime(pendingResourcePack).compareTo(Files.getLastModifiedTime(resourcePack)) >= 0
				? pendingResourcePack
				: resourcePack;
	}

	private static void replacePendingPreservingExisting(Path source, Path pendingResourcePack) throws IOException {
		Path recoveryFile = null;
		if (Files.isRegularFile(pendingResourcePack)) {
			recoveryFile = Files.createTempFile(
					pendingResourcePack.getParent(),
					pendingResourcePack.getFileName().toString() + "-recovery-",
					".tmp"
			);
			Files.copy(pendingResourcePack, recoveryFile, StandardCopyOption.REPLACE_EXISTING);
		}

		try {
			moveReplacing(source, pendingResourcePack);
		} catch (IOException replacementException) {
			if (recoveryFile != null && Files.notExists(pendingResourcePack)) {
				try {
					moveReplacing(recoveryFile, pendingResourcePack);
					recoveryFile = null;
				} catch (IOException recoveryException) {
					replacementException.addSuppressed(recoveryException);
				}
			}
			throw replacementException;
		} finally {
			if (recoveryFile != null) {
				Files.deleteIfExists(recoveryFile);
			}
		}
	}

	private static String normalizedZipPath(String zipPath) throws IOException {
		String normalizedPath = zipPath.replace('\\', '/');
		while (normalizedPath.startsWith("/")) {
			normalizedPath = normalizedPath.substring(1);
		}
		for (String segment : normalizedPath.split("/")) {
			if (segment.equals(".") || segment.equals("..")) {
				throw new IOException("unsafe zip entry: " + zipPath);
			}
		}
		return normalizedPath;
	}

	private static Path backupPath(Path path) {
		return path.resolveSibling(path.getFileName().toString() + BACKUP_SUFFIX);
	}

	private static Path pendingPath(Path path) {
		return path.resolveSibling(path.getFileName().toString() + PENDING_SUFFIX);
	}

	private static void moveReplacing(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static String normalizedLanguageCode(String languageCode) throws IOException {
		if (languageCode == null || !languageCode.matches("[a-z0-9_]+")) {
			throw new IOException("invalid language code: " + languageCode);
		}
		return languageCode;
	}

	public record WriteResult(
			Path resourcePack,
			List<String> languageFiles,
			boolean zip,
			boolean pendingReplacement
	) {
	}

	private record ResourcePackLayout(
			Set<String> namespaces,
			Set<String> languageDirectories,
			Set<String> targetLanguageFiles
	) {
		private static ResourcePackLayout empty() {
			return new ResourcePackLayout(Set.of(), Set.of(), Set.of());
		}
	}
}
