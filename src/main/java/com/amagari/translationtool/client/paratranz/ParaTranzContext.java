package com.amagari.translationtool.client.paratranz;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.client.bilingual.BilingualLanguageController;
import com.amagari.translationtool.client.bilingual.BilingualSourceTranslations;
import com.amagari.translationtool.client.WorldLanguageClient;
import com.amagari.translationtool.client.WorldLanguageContext;
import com.amagari.translationtool.translation.WorldLanguageFiles;
import com.amagari.translationtool.translation.WorldLanguageMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public final class ParaTranzContext {
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "Amagari ParaTranz");
		thread.setDaemon(true);
		return thread;
	});
	private static final AtomicReference<ParaTranzReport> LAST_REPORT = new AtomicReference<>(ParaTranzReport.idle());
	private static final AtomicReference<ParaTranzZipTranslations.ParseResult> ACTIVE_TRANSLATIONS = new AtomicReference<>();
	private static final AtomicReference<ParaTranzConfig> ACTIVE_CONFIG = new AtomicReference<>(ParaTranzConfig.defaultConfig());
	private static final AtomicReference<Map<String, String>> ACTIVE_LITERAL_TRANSLATIONS = new AtomicReference<>(Map.of());
	private static final AtomicReference<Map<String, String>> ACTIVE_SOURCE_LITERAL_TRANSLATIONS = new AtomicReference<>(Map.of());
	private static final AtomicReference<Map<String, Map<String, String>>> WORLD_TRANSLATIONS_BY_LANGUAGE = new AtomicReference<>(Map.of());
	private static final AtomicReference<Map<String, String>> WORLD_LITERAL_TRANSLATIONS = new AtomicReference<>(Map.of());
	private static final AtomicReference<Map<String, String>> WORLD_SOURCE_LITERAL_TRANSLATIONS = new AtomicReference<>(Map.of());
	private static final AtomicReference<List<ParaTranzProject>> PROJECTS_CACHE = new AtomicReference<>(List.of());
	private static final ParaTranzApiClient API_CLIENT = new ParaTranzApiClient();

	private ParaTranzContext() {
	}

	public static void listProjects(Minecraft client) {
		LAST_REPORT.set(ParaTranzReport.listing(0));
		CompletableFuture
				.supplyAsync(() -> loadProjects(client.gameDirectory.toPath()), EXECUTOR)
				.whenComplete((projects, throwable) -> client.execute(() -> {
					if (throwable != null) {
						reportFailure(client, throwable);
						return;
					}
					LAST_REPORT.set(ParaTranzReport.listing(projects.size()));
					send(client, WorldLanguageMessages.paraTranzClickableProjectList(projects, selectedLanguage(client)));
				}));
	}

	public static void applyProject(Minecraft client, String projectName) {
		Path gameDirectory = client.gameDirectory.toPath();
		CompletableFuture
				.supplyAsync(() -> findProject(gameDirectory, projectName), EXECUTOR)
				.whenComplete((match, throwable) -> client.execute(() -> {
					if (throwable != null) {
						reportFailure(client, throwable);
						return;
					}
					handleMatch(client, gameDirectory, projectName, match);
				}));
	}

	public static void resetSessionState() {
		ACTIVE_TRANSLATIONS.set(null);
		ACTIVE_CONFIG.set(ParaTranzConfig.defaultConfig());
		ACTIVE_LITERAL_TRANSLATIONS.set(Map.of());
		ACTIVE_SOURCE_LITERAL_TRANSLATIONS.set(Map.of());
		WORLD_TRANSLATIONS_BY_LANGUAGE.set(Map.of());
		WORLD_LITERAL_TRANSLATIONS.set(Map.of());
		WORLD_SOURCE_LITERAL_TRANSLATIONS.set(Map.of());
		LAST_REPORT.set(ParaTranzReport.idle());
	}

	public static ParaTranzConfig refreshConfig(Path gameDirectory) {
		try {
			ParaTranzConfig config = ParaTranzConfig.load(gameDirectory);
			ACTIVE_CONFIG.set(config);
			refreshActiveLiteralTranslations();
			refreshWorldLiteralTranslationIndexes(WORLD_TRANSLATIONS_BY_LANGUAGE.get());
			return config;
		} catch (IOException exception) {
			AmagariTranslationTool.LOGGER.warn("Could not load ParaTranz config; keeping active/default language settings", exception);
			return ACTIVE_CONFIG.get();
		}
	}

	public static void updateActiveConfig(ParaTranzConfig config) {
		ACTIVE_CONFIG.set(config == null ? ParaTranzConfig.defaultConfig() : config);
		refreshActiveLiteralTranslations();
		refreshWorldLiteralTranslationIndexes(WORLD_TRANSLATIONS_BY_LANGUAGE.get());
	}

	public static void mergeTranslations(List<String> languageCodes, Map<String, String> translations) {
		ParaTranzZipTranslations.ParseResult activeTranslations = ACTIVE_TRANSLATIONS.get();
		if (activeTranslations == null) {
			ACTIVE_LITERAL_TRANSLATIONS.set(Map.of());
			ACTIVE_SOURCE_LITERAL_TRANSLATIONS.set(Map.of());
			return;
		}

		refreshLiteralTranslations(activeTranslations);
		if (BilingualLanguageController.isSourceLanguageActive()) {
			return;
		}

		Map<String, String> mergedTranslations = activeTranslations.mergedForLanguages(languageCodes);
		translations.putAll(mergedTranslations);
	}

	public static Optional<String> translateLiteralWorldText(String text) {
		if (BilingualLanguageController.isSourceLanguageActive()) {
			return sourceLiteralWorldTextForDisplay(text);
		}
		return targetLiteralWorldTextForDisplay(text);
	}

	public static Optional<String> targetLiteralWorldText(String text) {
		if (text == null || text.isBlank()) {
			return Optional.empty();
		}
		return ParaTranzLiteralTranslations.translate(text, ACTIVE_LITERAL_TRANSLATIONS.get())
				.or(() -> ParaTranzLiteralTranslations.translate(text, WORLD_LITERAL_TRANSLATIONS.get()));
	}

	public static Optional<String> sourceLiteralWorldText(String text) {
		if (text == null || text.isBlank()) {
			return Optional.empty();
		}
		return ParaTranzLiteralTranslations.translate(text, ACTIVE_SOURCE_LITERAL_TRANSLATIONS.get())
				.or(() -> ParaTranzLiteralTranslations.translate(text, WORLD_SOURCE_LITERAL_TRANSLATIONS.get()));
	}

	public static Optional<String> sourceLiteralWorldTextForDisplay(String text) {
		Optional<String> sourceText = sourceLiteralWorldText(text);
		if (sourceText.isPresent()) {
			return sourceText;
		}
		return targetLiteralWorldText(text).isPresent() ? Optional.of(text) : Optional.empty();
	}

	public static Optional<String> targetLiteralWorldTextForDisplay(String text) {
		Optional<String> targetText = targetLiteralWorldText(text);
		if (targetText.isPresent()) {
			return targetText;
		}
		return sourceLiteralWorldText(text).isPresent() ? Optional.of(text) : Optional.empty();
	}

	public static boolean hasLiteralWorldTextTranslations() {
		return !ACTIVE_LITERAL_TRANSLATIONS.get().isEmpty()
				|| !ACTIVE_SOURCE_LITERAL_TRANSLATIONS.get().isEmpty()
				|| !WORLD_LITERAL_TRANSLATIONS.get().isEmpty()
				|| !WORLD_SOURCE_LITERAL_TRANSLATIONS.get().isEmpty();
	}

	public static void refreshWorldLiteralTranslations(Map<String, Map<String, String>> translationsByLanguage) {
		Map<String, Map<String, String>> copiedTranslations = copyTranslations(translationsByLanguage);
		WORLD_TRANSLATIONS_BY_LANGUAGE.set(copiedTranslations);
		refreshWorldLiteralTranslationIndexes(copiedTranslations);
	}

	public static Optional<String> sourceTranslation(String translationKey) {
		ParaTranzZipTranslations.ParseResult activeTranslations = ACTIVE_TRANSLATIONS.get();
		if (activeTranslations == null || translationKey == null || translationKey.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(activeTranslations.translationsByLanguage()
				.getOrDefault(sourceLanguage(), Map.of())
				.get(translationKey));
	}

	public static boolean hasSourceTranslations() {
		return hasSourceTranslations(sourceLanguage());
	}

	public static boolean hasSourceTranslations(String sourceLanguage) {
		ParaTranzZipTranslations.ParseResult activeTranslations = ACTIVE_TRANSLATIONS.get();
		if (activeTranslations == null) {
			return false;
		}
		return !activeTranslations.translationsByLanguage()
				.getOrDefault(sourceLanguage, Map.of())
				.isEmpty();
	}

	public static String sourceLanguage() {
		return ACTIVE_CONFIG.get().sourceLanguage();
	}

	public static String targetLanguage() {
		return ACTIVE_CONFIG.get().targetLanguage();
	}

	public static ParaTranzReport lastReport() {
		return LAST_REPORT.get();
	}

	public static CompletableFuture<List<String>> suggestProjectNames(Path gameDirectory, String input) {
		List<ParaTranzProject> cachedProjects = PROJECTS_CACHE.get();
		if (!cachedProjects.isEmpty()) {
			return CompletableFuture.completedFuture(ParaTranzProjectSuggestions.matchingNames(cachedProjects, input));
		}
		return CompletableFuture.supplyAsync(() -> ParaTranzProjectSuggestions.matchingNames(loadProjects(gameDirectory), input), EXECUTOR)
				.exceptionally(throwable -> List.of());
	}

	private static void handleMatch(Minecraft client, Path gameDirectory, String projectName, ParaTranzProjectMatcher.MatchResult match) {
		String languageCode = selectedLanguage(client);
		if (match.status() == ParaTranzProjectMatcher.MatchStatus.NOT_FOUND) {
			send(client, WorldLanguageMessages.paraTranzProjectNotFound(projectName, languageCode));
			return;
		}
		if (match.status() == ParaTranzProjectMatcher.MatchStatus.AMBIGUOUS) {
			send(client, WorldLanguageMessages.paraTranzProjectAmbiguous(projectName, match.candidates(), languageCode));
			return;
		}

		ParaTranzProject project = match.project().orElseThrow();
		LAST_REPORT.set(ParaTranzReport.downloading(project));
		send(client, WorldLanguageMessages.paraTranzStatus(LAST_REPORT.get(), languageCode));
		CompletableFuture
				.supplyAsync(() -> downloadAndCache(gameDirectory, project), EXECUTOR)
				.whenComplete((cachedTranslations, throwable) -> client.execute(() -> {
					if (throwable != null) {
						reportFailure(client, throwable);
						return;
					}
					applyCachedTranslations(client, cachedTranslations);
				}));
	}

	private static List<ParaTranzProject> loadProjects(Path gameDirectory) {
		try {
			List<ParaTranzProject> projects = API_CLIENT.listProjects(token(gameDirectory)).stream()
					.sorted(Comparator.comparing(ParaTranzProject::name, String.CASE_INSENSITIVE_ORDER))
					.toList();
			PROJECTS_CACHE.set(projects);
			return projects;
		} catch (IOException exception) {
			throw new ParaTranzRuntimeException(cleanMessage(exception), exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ParaTranzRuntimeException("ParaTranz request was interrupted", exception);
		}
	}

	private static ParaTranzProjectMatcher.MatchResult findProject(Path gameDirectory, String projectName) {
		return ParaTranzProjectMatcher.match(loadProjects(gameDirectory), projectName);
	}

	private static ParaTranzCache.CachedTranslations downloadAndCache(Path gameDirectory, ParaTranzProject project) {
		try {
			ParaTranzConfig config = ParaTranzConfig.load(gameDirectory);
			ParaTranzApiClient.DownloadedArtifact downloadedArtifact = API_CLIENT.exportAndDownload(project, token(config), config.triggerExport());
			return ParaTranzCache.store(gameDirectory, project, downloadedArtifact.artifact(), downloadedArtifact.zipData(), config);
		} catch (IOException exception) {
			throw new ParaTranzRuntimeException(cleanMessage(exception), exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ParaTranzRuntimeException("ParaTranz request was interrupted", exception);
		}
	}

	private static void applyCachedTranslations(Minecraft client, ParaTranzCache.CachedTranslations cachedTranslations) {
		ACTIVE_TRANSLATIONS.set(cachedTranslations.translations());
		String languageCode = selectedLanguage(client);
		ParaTranzConfig config = cachedTranslations.config();
		ACTIVE_CONFIG.set(config);
		refreshLiteralTranslations(cachedTranslations.translations());
		String overwriteMessage = "";
		if (config.overwriteWorldLanguageFiles()) {
			overwriteMessage = overwriteWorldLanguageFiles(cachedTranslations.translations(), config.targetLanguage(), languageCode);
		}
		LAST_REPORT.set(ParaTranzReport.applied(
				cachedTranslations.project(),
				cachedTranslations.artifact(),
				cachedTranslations.translations().loadedFiles(),
				cachedTranslations.translations().failedFiles(),
				cachedTranslations.translations().loadedEntries(),
				cachedTranslations.translations().translationsByLanguage().keySet().stream().sorted().toList()
		));
		String finalOverwriteMessage = overwriteMessage;
		WorldLanguageClient.reloadLanguage(client, () -> {
			send(client, LAST_REPORT.get().describe(selectedLanguage(client)));
			if (!finalOverwriteMessage.isBlank()) {
				send(client, finalOverwriteMessage);
			}
		});
	}

	private static void refreshActiveLiteralTranslations() {
		ParaTranzZipTranslations.ParseResult activeTranslations = ACTIVE_TRANSLATIONS.get();
		if (activeTranslations != null) {
			refreshLiteralTranslations(activeTranslations);
		}
	}

	private static void refreshWorldLiteralTranslationIndexes(Map<String, Map<String, String>> translationsByLanguage) {
		if (translationsByLanguage == null || translationsByLanguage.isEmpty()) {
			WORLD_LITERAL_TRANSLATIONS.set(Map.of());
			WORLD_SOURCE_LITERAL_TRANSLATIONS.set(Map.of());
			return;
		}

		ParaTranzConfig config = ACTIVE_CONFIG.get();
		Map<String, String> sourceTranslations = translationsByLanguage.getOrDefault(config.sourceLanguage(), Map.of());
		Map<String, String> targetTranslations = translationsByLanguage.getOrDefault(config.targetLanguage(), Map.of());
		if (targetTranslations.isEmpty() && !sourceTranslations.isEmpty()) {
			targetTranslations = sourceTranslations;
		}
		WORLD_LITERAL_TRANSLATIONS.set(ParaTranzLiteralTranslations.index(targetTranslations));
		WORLD_SOURCE_LITERAL_TRANSLATIONS.set(ParaTranzLiteralTranslations.sourceIndex(sourceTranslations, targetTranslations, BilingualSourceTranslations::sourceText));
	}

	private static void refreshLiteralTranslations(ParaTranzZipTranslations.ParseResult activeTranslations) {
		ParaTranzConfig config = ACTIVE_CONFIG.get();
		Map<String, String> targetTranslations = activeTranslations.translationsByLanguage().getOrDefault(config.targetLanguage(), Map.of());
		Map<String, String> sourceTranslations = activeTranslations.translationsByLanguage().getOrDefault(config.sourceLanguage(), Map.of());
		if (targetTranslations.isEmpty() && !sourceTranslations.isEmpty()) {
			targetTranslations = sourceTranslations;
		}
		ACTIVE_LITERAL_TRANSLATIONS.set(ParaTranzLiteralTranslations.index(targetTranslations));
		ACTIVE_SOURCE_LITERAL_TRANSLATIONS.set(ParaTranzLiteralTranslations.sourceIndex(sourceTranslations, targetTranslations, BilingualSourceTranslations::sourceText));
	}

	private static Map<String, Map<String, String>> copyTranslations(Map<String, Map<String, String>> translationsByLanguage) {
		if (translationsByLanguage == null || translationsByLanguage.isEmpty()) {
			return Map.of();
		}

		return translationsByLanguage.entrySet().stream()
				.filter(entry -> entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty())
				.collect(java.util.stream.Collectors.toUnmodifiableMap(
						Map.Entry::getKey,
						entry -> Map.copyOf(entry.getValue())
				));
	}

	private static String overwriteWorldLanguageFiles(ParaTranzZipTranslations.ParseResult translations, String targetLanguage, String feedbackLanguage) {
		Optional<Path> worldDirectory = WorldLanguageContext.getWorldDirectory();
		if (worldDirectory.isEmpty()) {
			return WorldLanguageMessages.paraTranzOverwriteSkippedNoWorld(feedbackLanguage);
		}

		Map<String, String> targetTranslations = translations.translationsByLanguage().get(targetLanguage);
		if (targetTranslations == null || targetTranslations.isEmpty()) {
			return WorldLanguageMessages.paraTranzOverwriteSkippedNoTarget(targetLanguage, feedbackLanguage);
		}

		try {
			WorldLanguageFiles.overwriteLanguages(worldDirectory.get(), Map.of(targetLanguage, targetTranslations));
			return WorldLanguageMessages.paraTranzOverwriteSucceeded(worldDirectory.get().resolve(WorldLanguageFiles.LANG_DIRECTORY), targetLanguage, feedbackLanguage);
		} catch (IOException exception) {
			return WorldLanguageMessages.paraTranzOverwriteFailed(exception.getMessage(), feedbackLanguage);
		}
	}

	private static String token(Path gameDirectory) throws IOException {
		return token(ParaTranzConfig.load(gameDirectory));
	}

	private static String token(ParaTranzConfig config) throws IOException {
		String token = config.paratranzApiToken();
		if (token == null || token.isBlank()) {
			throw new IOException("ParaTranz API token is not configured");
		}
		return token.trim();
	}

	private static void reportFailure(Minecraft client, Throwable throwable) {
		String message = cleanMessage(throwable);
		AmagariTranslationTool.LOGGER.warn("ParaTranz operation failed: {}", message);
		LAST_REPORT.set(ParaTranzReport.failed(message));
		send(client, LAST_REPORT.get().describe(selectedLanguage(client)));
	}

	private static String cleanMessage(Throwable throwable) {
		Throwable cause = unwrap(throwable);
		String message = cause.getMessage();
		return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
	}

	private static Throwable unwrap(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null
				&& (current instanceof ParaTranzRuntimeException
				|| current instanceof java.util.concurrent.CompletionException
				|| current instanceof java.util.concurrent.ExecutionException)) {
			current = current.getCause();
		}
		return current;
	}

	private static String selectedLanguage(Minecraft client) {
		return client.getLanguageManager().getSelected();
	}

	private static void send(Minecraft client, String message) {
		send(client, Component.literal(message));
	}

	private static void send(Minecraft client, Component message) {
		if (client.player != null) {
			client.player.sendSystemMessage(message);
		}
	}

	private static void send(Minecraft client, List<Component> messages) {
		messages.forEach(message -> send(client, message));
	}

	private static final class ParaTranzRuntimeException extends RuntimeException {
		private ParaTranzRuntimeException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
