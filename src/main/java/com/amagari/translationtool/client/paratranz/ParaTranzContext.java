package com.amagari.translationtool.client.paratranz;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.client.WorldLanguageClient;
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
	private static final AtomicReference<Map<String, String>> ACTIVE_LITERAL_TRANSLATIONS = new AtomicReference<>(Map.of());
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
					send(client, WorldLanguageMessages.paraTranzProjectList(projects, selectedLanguage(client)));
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
		ACTIVE_LITERAL_TRANSLATIONS.set(Map.of());
		LAST_REPORT.set(ParaTranzReport.idle());
	}

	public static void mergeTranslations(List<String> languageCodes, Map<String, String> translations) {
		ParaTranzZipTranslations.ParseResult activeTranslations = ACTIVE_TRANSLATIONS.get();
		if (activeTranslations == null) {
			ACTIVE_LITERAL_TRANSLATIONS.set(Map.of());
			return;
		}
		Map<String, String> mergedTranslations = activeTranslations.mergedForLanguages(languageCodes);
		translations.putAll(mergedTranslations);
		ACTIVE_LITERAL_TRANSLATIONS.set(ParaTranzLiteralTranslations.index(mergedTranslations));
	}

	public static Optional<String> translateLiteralWorldText(String text) {
		return ParaTranzLiteralTranslations.translate(text, ACTIVE_LITERAL_TRANSLATIONS.get());
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
			ParaTranzApiClient.DownloadedArtifact downloadedArtifact = API_CLIENT.exportAndDownload(project, token(gameDirectory));
			return ParaTranzCache.store(gameDirectory, project, downloadedArtifact.artifact(), downloadedArtifact.zipData());
		} catch (IOException exception) {
			throw new ParaTranzRuntimeException(cleanMessage(exception), exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ParaTranzRuntimeException("ParaTranz request was interrupted", exception);
		}
	}

	private static void applyCachedTranslations(Minecraft client, ParaTranzCache.CachedTranslations cachedTranslations) {
		ACTIVE_TRANSLATIONS.set(cachedTranslations.translations());
		LAST_REPORT.set(ParaTranzReport.applied(
				cachedTranslations.project(),
				cachedTranslations.artifact(),
				cachedTranslations.translations().loadedFiles(),
				cachedTranslations.translations().failedFiles(),
				cachedTranslations.translations().loadedEntries(),
				cachedTranslations.translations().translationsByLanguage().keySet().stream().sorted().toList()
		));
		WorldLanguageClient.reloadLanguage(client, () -> send(client, LAST_REPORT.get().describe(selectedLanguage(client))));
	}

	private static String token(Path gameDirectory) throws IOException {
		String token = ParaTranzConfig.load(gameDirectory).paratranzApiToken();
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
		if (client.player != null) {
			client.player.sendSystemMessage(Component.literal(message));
		}
	}

	private static final class ParaTranzRuntimeException extends RuntimeException {
		private ParaTranzRuntimeException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
