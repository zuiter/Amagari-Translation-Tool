package com.amagari.translationtool;

import com.amagari.translationtool.client.paratranz.ParaTranzArtifact;
import com.amagari.translationtool.client.paratranz.ParaTranzConfig;
import com.amagari.translationtool.client.paratranz.ParaTranzJson;
import com.amagari.translationtool.client.paratranz.ParaTranzLiteralTranslations;
import com.amagari.translationtool.client.paratranz.ParaTranzProject;
import com.amagari.translationtool.client.paratranz.ParaTranzProjectMatcher;
import com.amagari.translationtool.client.paratranz.ParaTranzProjectSuggestions;
import com.amagari.translationtool.client.paratranz.ParaTranzReport;
import com.amagari.translationtool.client.paratranz.ParaTranzZipTranslations;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class AmagariUnitChecks {
	private AmagariUnitChecks() {
	}

	public static void main(String[] args) throws Exception {
		matchesProjectsByExactCaseInsensitiveName();
		reportsAmbiguousProjectMatches();
		reportsMissingProjectMatches();
		suggestsParaTranzProjectNamesByPartialInput();
		parsesNestedParaTranzProjectMemberships();
		parsesParaTranzArtifacts();
		parsesSingleParaTranzArtifactObject();
		defaultConfigStartsBlank();
		createsNestedParaTranzConfig();
		migratesLegacyParaTranzConfig();
		infersLanguageCodesFromParaTranzZipPaths();
		parsesValidJsonFilesAndSkipsMalformedFiles();
		resolvesLiteralWorldBlockTranslations();
		describesParaTranzStatus();
	}

	private static void matchesProjectsByExactCaseInsensitiveName() {
		ParaTranzProjectMatcher.MatchResult result = ParaTranzProjectMatcher.match(List.of(
				new ParaTranzProject(1, "Permafrost-i18n", 3, 0, "mc"),
				new ParaTranzProject(2, "Permafrost Tools", 3, 0, "mc")
		), "permafrost-i18n");

		check(result.status() == ParaTranzProjectMatcher.MatchStatus.MATCHED, "expected exact case-insensitive match");
		check(result.project().orElseThrow().id() == 1, "expected Permafrost-i18n project id");
	}

	private static void reportsAmbiguousProjectMatches() {
		ParaTranzProjectMatcher.MatchResult result = ParaTranzProjectMatcher.match(List.of(
				new ParaTranzProject(1, "Permafrost-i18n", 3, 0, "mc"),
				new ParaTranzProject(2, "Permafrost Tools", 3, 0, "mc")
		), "permafrost");

		check(result.status() == ParaTranzProjectMatcher.MatchStatus.AMBIGUOUS, "expected ambiguous substring match");
		check(result.candidates().size() == 2, "expected both Permafrost projects as candidates");
	}

	private static void reportsMissingProjectMatches() {
		ParaTranzProjectMatcher.MatchResult result = ParaTranzProjectMatcher.match(List.of(
				new ParaTranzProject(1, "Permafrost-i18n", 3, 0, "mc")
		), "missing");

		check(result.status() == ParaTranzProjectMatcher.MatchStatus.NOT_FOUND, "expected missing project to be not found");
		check(result.project().isEmpty(), "expected no matched project");
	}

	private static void suggestsParaTranzProjectNamesByPartialInput() {
		List<ParaTranzProject> projects = List.of(
				new ParaTranzProject(2, "The Dark Ship-i18n", 3, 0, "mc"),
				new ParaTranzProject(1, "Permafrost-i18n", 3, 0, "mc"),
				new ParaTranzProject(3, "Battle of the Bards", 3, 0, "mc")
		);

		check(ParaTranzProjectSuggestions.matchingNames(projects, "").equals(List.of(
				"Battle of the Bards",
				"Permafrost-i18n",
				"The Dark Ship-i18n"
		)), "expected blank project suggestion input to list all names in stable order");
		check(ParaTranzProjectSuggestions.matchingNames(projects, "perma").equals(List.of("Permafrost-i18n")), "expected case-insensitive prefix suggestions");
		check(ParaTranzProjectSuggestions.matchingNames(projects, "i18n").equals(List.of("Permafrost-i18n", "The Dark Ship-i18n")), "expected substring suggestions");
		check(ParaTranzProjectSuggestions.matchingNames(projects, "battle").equals(List.of("Battle of the Bards")), "expected project names with spaces to be suggested");
	}

	private static void parsesNestedParaTranzProjectMemberships() {
		String json = """
				[
				  {
				    "permission": 3,
				    "project": {
				      "id": 19173,
				      "name": "Permafrost-i18n",
				      "privacy": 0,
				      "game": "mc"
				    }
				  }
				]
				""";

		List<ParaTranzProject> projects = ParaTranzJson.parseProjects(json);

		check(projects.size() == 1, "expected one parsed project");
		check(projects.getFirst().id() == 19173, "expected parsed project id");
		check(projects.getFirst().permission() == 3, "expected membership permission");
		check("mc".equals(projects.getFirst().game()), "expected game code");
	}

	private static void parsesParaTranzArtifacts() {
		String json = """
				[
				  {
				    "id": 1605565,
				    "createdAt": "2026-05-23T14:02:20.031Z",
				    "project": 19173,
				    "total": 866,
				    "translated": 235,
				    "size": 24637
				  }
				]
				""";

		List<ParaTranzArtifact> artifacts = ParaTranzJson.parseArtifacts(json);

		check(artifacts.size() == 1, "expected one parsed artifact");
		check(artifacts.getFirst().id() == 1605565, "expected parsed artifact id");
		check(artifacts.getFirst().projectId() == 19173, "expected parsed artifact project id");
		check(artifacts.getFirst().createdAt().equals(Instant.parse("2026-05-23T14:02:20.031Z")), "expected parsed createdAt");
	}

	private static void parsesSingleParaTranzArtifactObject() {
		String json = """
				{
				  "id": 1605721,
				  "createdAt": "2026-05-23T17:17:01.306Z",
				  "project": 19173,
				  "total": 866,
				  "translated": 774,
				  "size": 26863
				}
				""";

		List<ParaTranzArtifact> artifacts = ParaTranzJson.parseArtifacts(json);

		check(artifacts.size() == 1, "expected one parsed artifact from object response");
		check(artifacts.getFirst().id() == 1605721, "expected parsed single-object artifact id");
		check(artifacts.getFirst().projectId() == 19173, "expected parsed single-object project id");
	}

	private static void defaultConfigStartsBlank() {
		check(ParaTranzConfig.defaultConfig().paratranzApiToken().isBlank(), "expected editable blank ParaTranz token placeholder");
	}

	private static void createsNestedParaTranzConfig() throws Exception {
		Path gameDirectory = Files.createTempDirectory("amagari-config");

		ParaTranzConfig config = ParaTranzConfig.load(gameDirectory);

		Path configPath = gameDirectory.resolve("config").resolve("amagari_lang").resolve("config.json");
		check(config.paratranzApiToken().isBlank(), "expected newly generated config to use a blank token");
		check(Files.exists(configPath), "expected config/amagari_lang/config.json to be created");
		check(Files.readString(configPath, StandardCharsets.UTF_8).contains("\"paratranzApiToken\""), "expected generated config to contain paratranzApiToken");
	}

	private static void migratesLegacyParaTranzConfig() throws Exception {
		Path gameDirectory = Files.createTempDirectory("amagari-config-migration");
		Path legacyPath = gameDirectory.resolve("config").resolve("amagari_translation_tool.json");
		Files.createDirectories(legacyPath.getParent());
		Files.writeString(legacyPath, "{\"paratranzApiToken\":\"legacy-token\"}", StandardCharsets.UTF_8);

		ParaTranzConfig config = ParaTranzConfig.load(gameDirectory);

		Path configPath = gameDirectory.resolve("config").resolve("amagari_lang").resolve("config.json");
		check("legacy-token".equals(config.paratranzApiToken()), "expected legacy token to be loaded");
		check(Files.exists(configPath), "expected legacy config to be migrated to nested config path");
		check(Files.readString(configPath, StandardCharsets.UTF_8).contains("legacy-token"), "expected migrated config to preserve legacy token");
	}

	private static void infersLanguageCodesFromParaTranzZipPaths() {
		check("en_us".equals(ParaTranzZipTranslations.languageCode("utf8/en_us.json").orElseThrow()), "expected utf8/en_us.json -> en_us");
		check("zh_cn".equals(ParaTranzZipTranslations.languageCode("zh_cn.json").orElseThrow()), "expected zh_cn.json -> zh_cn");
		check("zh_cn".equals(ParaTranzZipTranslations.languageCode("nested/zh_cn.items.json").orElseThrow()), "expected zh_cn.items.json -> zh_cn");
		check(ParaTranzZipTranslations.languageCode("readme.txt").isEmpty(), "expected non-json files to be ignored");
	}

	private static void parsesValidJsonFilesAndSkipsMalformedFiles() throws Exception {
		byte[] zipData = zip(Map.of(
				"utf8/en_us.json", "{\"item.minecraft.diamond\":\"神馐\"}",
				"utf8/zh_cn.items.json", "{\"item.minecraft.book\":\"配方书\"}",
				"utf8/bad.json", "{not-json"
		));

		ParaTranzZipTranslations.ParseResult result = ParaTranzZipTranslations.parse(zipData);

		check(result.loadedFiles() == 2, "expected two valid json files");
		check(result.failedFiles() == 1, "expected one malformed json file to be skipped");
		check(result.translationsByLanguage().get("en_us").get("item.minecraft.diamond").equals("神馐"), "expected en_us translation");
		check(result.translationsByLanguage().get("zh_cn").get("item.minecraft.book").equals("配方书"), "expected zh_cn translation");
	}

	private static void resolvesLiteralWorldBlockTranslations() {
		Map<String, String> indexed = ParaTranzLiteralTranslations.index(Map.of(
				"permafrost.i18n.map.credits", "not a sign translation",
				"permafrost.i18n.world.block.credits", "translated credits",
				"permafrost.i18n.world.block.settings_for", "translated settings",
				"permafrost.i18n.world.block.install", "translated install"
		));

		check("translated credits".equals(ParaTranzLiteralTranslations.translate("Credits", indexed).orElseThrow()), "expected direct literal sign match");
		check("translated settings".equals(ParaTranzLiteralTranslations.translate("Settings for an", indexed).orElseThrow()), "expected trailing article sign match");
		check("translated install".equals(ParaTranzLiteralTranslations.translate("Install the", indexed).orElseThrow()), "expected trailing the sign match");
		check(ParaTranzLiteralTranslations.translate("Credits?", Map.of()).isEmpty(), "expected inactive sign translations to be ignored");
	}

	private static void describesParaTranzStatus() {
		check(ParaTranzReport.idle().describe("en_us").contains("no project"), "expected idle status");
		check(ParaTranzReport.listing(3).describe("en_us").contains("listed 3"), "expected listed status");
		check(ParaTranzReport.failed("permission denied").describe("en_us").contains("permission denied"), "expected failed status");

		ParaTranzProject project = new ParaTranzProject(19173, "Permafrost-i18n", 3, 0, "mc");
		check(ParaTranzReport.downloading(project).describe("en_us").contains("pulling Permafrost-i18n"), "expected downloading status");

		ParaTranzReport report = ParaTranzReport.applied(
				project,
				new ParaTranzArtifact(1605565, 19173, Instant.parse("2026-05-23T14:02:20.031Z"), 866, 235, 24637),
				1,
				2,
				242,
				List.of("en_us")
		);

		check(report.describe("en_us").contains("Permafrost-i18n"), "expected English status to name the project");
		check(report.describe("en_us").contains("2026-05-23T14:02:20.031Z"), "expected English status to include artifact time");
		check(report.describe("zh_cn").contains("Permafrost-i18n"), "expected Chinese status to name the project");
	}

	private static byte[] zip(Map<String, String> entries) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			for (Map.Entry<String, String> entry : entries.entrySet()) {
				zip.putNextEntry(new ZipEntry(entry.getKey()));
				zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		return bytes.toByteArray();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
