package com.amagari.translationtool;

import com.amagari.translationtool.client.bilingual.BilingualSourceText;
import com.amagari.translationtool.client.WorldLanguageContext;
import com.amagari.translationtool.client.paratranz.ParaTranzArtifact;
import com.amagari.translationtool.client.paratranz.ParaTranzConfig;
import com.amagari.translationtool.client.paratranz.ParaTranzContext;
import com.amagari.translationtool.client.paratranz.ParaTranzJson;
import com.amagari.translationtool.client.paratranz.ParaTranzLiteralTranslations;
import com.amagari.translationtool.client.paratranz.ParaTranzProject;
import com.amagari.translationtool.client.paratranz.ParaTranzProjectMatcher;
import com.amagari.translationtool.client.paratranz.ParaTranzProjectSuggestions;
import com.amagari.translationtool.client.paratranz.ParaTranzReport;
import com.amagari.translationtool.client.paratranz.ParaTranzSignText;
import com.amagari.translationtool.client.paratranz.ParaTranzZipTranslations;
import com.amagari.translationtool.translation.WorldLanguageFiles;
import com.amagari.translationtool.translation.WorldLanguageMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
		mapsConfiguredParaTranzSourceLanguageToTargetLanguage();
		overwritesWorldLanguageFileForTargetLanguage();
		resolvesLiteralWorldBlockTranslations();
		resolvesSourceLiteralWorldBlockTranslations();
		resolvesWorldFileLiteralSignTranslations();
		loadsWorldSourceLanguageForLiteralSignDisplay();
		translatesStyledLiteralSignComponents();
		preservesStyledBilingualSourceComponents();
		buildsClickableParaTranzProjectList();
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
		check(!ParaTranzConfig.defaultConfig().overwriteWorldLanguageFiles(), "expected world language overwrite to be disabled by default");
	}

	private static void createsNestedParaTranzConfig() throws Exception {
		Path gameDirectory = Files.createTempDirectory("amagari-config");

		ParaTranzConfig config = ParaTranzConfig.load(gameDirectory);

		Path configPath = gameDirectory.resolve("config").resolve("amagari_lang").resolve("config.json");
		check(config.paratranzApiToken().isBlank(), "expected newly generated config to use a blank token");
		check(Files.exists(configPath), "expected config/amagari_lang/config.json to be created");
		check(Files.readString(configPath, StandardCharsets.UTF_8).contains("\"paratranzApiToken\""), "expected generated config to contain paratranzApiToken");
		check(Files.readString(configPath, StandardCharsets.UTF_8).contains("\"overwriteWorldLanguageFiles\""), "expected generated config to contain overwriteWorldLanguageFiles");
	}

	private static void migratesLegacyParaTranzConfig() throws Exception {
		Path gameDirectory = Files.createTempDirectory("amagari-config-migration");
		Path legacyPath = gameDirectory.resolve("config").resolve("amagari_translation_tool.json");
		Files.createDirectories(legacyPath.getParent());
		Files.writeString(legacyPath, "{\"paratranzApiToken\":\"legacy-token\"}", StandardCharsets.UTF_8);

		ParaTranzConfig config = ParaTranzConfig.load(gameDirectory);

		Path configPath = gameDirectory.resolve("config").resolve("amagari_lang").resolve("config.json");
		check("legacy-token".equals(config.paratranzApiToken()), "expected legacy token to be loaded");
		check(!config.overwriteWorldLanguageFiles(), "expected migrated config to keep world language overwrite disabled");
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

	private static void mapsConfiguredParaTranzSourceLanguageToTargetLanguage() throws Exception {
		byte[] zipData = zip(Map.of("utf8/en_us.json", "{\"item.minecraft.diamond\":\"神馐\"}"));

		ParaTranzZipTranslations.ParseResult result = ParaTranzZipTranslations.parse(zipData).mappedLanguage("en_us", "zh_cn");

		check(result.translationsByLanguage().get("zh_cn").get("item.minecraft.diamond").equals("神馐"), "expected configured source language to be mapped into target language");
	}

	private static void overwritesWorldLanguageFileForTargetLanguage() throws Exception {
		Path worldDirectory = Files.createTempDirectory("amagari-world");
		Path languageDirectory = worldDirectory.resolve(WorldLanguageFiles.LANG_DIRECTORY);
		Files.createDirectories(languageDirectory);
		Files.writeString(languageDirectory.resolve("zh_cn.json"), "{\"item.minecraft.diamond\":\"old target\"}", StandardCharsets.UTF_8);
		Files.writeString(languageDirectory.resolve("zh_cn.items.json"), "{\"item.minecraft.book\":\"old split\"}", StandardCharsets.UTF_8);
		Files.writeString(languageDirectory.resolve("en_us.json"), "{\"item.minecraft.diamond\":\"Diamond\"}", StandardCharsets.UTF_8);

		WorldLanguageFiles.overwriteLanguages(worldDirectory, Map.of(
				"zh_cn",
				Map.of("item.minecraft.diamond", "new target")
		));

		String updatedTarget = Files.readString(languageDirectory.resolve("zh_cn.json"), StandardCharsets.UTF_8);
		check(updatedTarget.contains("new target"), "expected target language file to be overwritten");
		check(Files.notExists(languageDirectory.resolve("zh_cn.items.json")), "expected old target-language split files to be removed");
		check(Files.exists(languageDirectory.resolve("en_us.json")), "expected other language files to be preserved");
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

	private static void resolvesSourceLiteralWorldBlockTranslations() throws Exception {
		AtomicReference<Map<String, String>> literalTranslations = privateAtomicReference(
				ParaTranzContext.class,
				"ACTIVE_LITERAL_TRANSLATIONS"
		);
		AtomicReference<Map<String, String>> sourceLiteralTranslations = privateAtomicReference(
				ParaTranzContext.class,
				"ACTIVE_SOURCE_LITERAL_TRANSLATIONS"
		);
		AtomicBoolean sourceLanguageActive = privateAtomicBoolean(
				Class.forName("com.amagari.translationtool.client.bilingual.BilingualLanguageController"),
				"SOURCE_LANGUAGE_ACTIVE"
		);
		Map<String, String> previousTranslations = literalTranslations.get();
		Map<String, String> previousSourceTranslations = sourceLiteralTranslations.get();
		boolean previousSourceLanguageActive = sourceLanguageActive.get();
		try {
			literalTranslations.set(ParaTranzLiteralTranslations.index(Map.of(
					"permafrost.i18n.world.block.credits",
					"翻译制作"
			)));
			sourceLiteralTranslations.set(ParaTranzLiteralTranslations.sourceIndex(
					Map.of("permafrost.i18n.world.block.credits", "Credits"),
					Map.of("permafrost.i18n.world.block.credits", "翻译制作")
			));
			sourceLanguageActive.set(false);
			check(ParaTranzContext.translateLiteralWorldText("Credits").orElseThrow().equals("翻译制作"), "expected literal sign text to translate normally");
			check(ParaTranzContext.translateLiteralWorldText("翻译制作").orElseThrow().equals("翻译制作"), "expected target-language mode to keep already-target literal sign text");

			sourceLanguageActive.set(true);
			check(ParaTranzContext.translateLiteralWorldText("翻译制作").orElseThrow().equals("Credits"), "expected source-language mode to restore literal sign source text");
			check(ParaTranzContext.translateLiteralWorldText("Credits").orElseThrow().equals("Credits"), "expected source-language mode to keep already-source literal sign text");
			check(ParaTranzContext.sourceLiteralWorldTextForDisplay("翻译制作").orElseThrow().equals("Credits"), "expected source display to show source text for translated signs");
			check(ParaTranzContext.sourceLiteralWorldTextForDisplay("Credits").orElseThrow().equals("Credits"), "expected source display to show source text for translatable source signs");
			check(ParaTranzContext.sourceLiteralWorldTextForDisplay("Plain stone").isEmpty(), "expected source display to ignore untranslated block names");
		} finally {
			literalTranslations.set(previousTranslations);
			sourceLiteralTranslations.set(previousSourceTranslations);
			sourceLanguageActive.set(previousSourceLanguageActive);
		}
	}

	private static void resolvesWorldFileLiteralSignTranslations() throws Exception {
		AtomicReference<Map<String, String>> literalTranslations = privateAtomicReference(
				ParaTranzContext.class,
				"ACTIVE_LITERAL_TRANSLATIONS"
		);
		AtomicReference<Map<String, String>> sourceLiteralTranslations = privateAtomicReference(
				ParaTranzContext.class,
				"ACTIVE_SOURCE_LITERAL_TRANSLATIONS"
		);
		AtomicBoolean sourceLanguageActive = privateAtomicBoolean(
				Class.forName("com.amagari.translationtool.client.bilingual.BilingualLanguageController"),
				"SOURCE_LANGUAGE_ACTIVE"
		);
		Map<String, String> previousTranslations = literalTranslations.get();
		Map<String, String> previousSourceTranslations = sourceLiteralTranslations.get();
		boolean previousSourceLanguageActive = sourceLanguageActive.get();
		try {
			literalTranslations.set(Map.of());
			sourceLiteralTranslations.set(Map.of());
			ParaTranzContext.updateActiveConfig(new ParaTranzConfig("", "en_us", "zh_cn", true, 1, false));
			ParaTranzContext.refreshWorldLiteralTranslations(Map.of(
					"zh_cn",
					Map.of("permafrost.i18n.world.block.credits", "鸣谢")
			));

			sourceLanguageActive.set(false);
			check("鸣谢".equals(ParaTranzContext.translateLiteralWorldText("Credits").orElseThrow()), "expected target sign text from world language files");

			sourceLanguageActive.set(true);
			check("Credits".equals(ParaTranzContext.translateLiteralWorldText("鸣谢").orElseThrow()), "expected source sign text derived from world language keys");
		} finally {
			ParaTranzContext.refreshWorldLiteralTranslations(Map.of());
			literalTranslations.set(previousTranslations);
			sourceLiteralTranslations.set(previousSourceTranslations);
			sourceLanguageActive.set(previousSourceLanguageActive);
			ParaTranzContext.updateActiveConfig(ParaTranzConfig.defaultConfig());
		}
	}

	private static void loadsWorldSourceLanguageForLiteralSignDisplay() throws Exception {
		Path worldDirectory = Files.createTempDirectory("amagari-world-source-display");
		Path languageDirectory = worldDirectory.resolve(WorldLanguageFiles.LANG_DIRECTORY);
		Files.createDirectories(languageDirectory);
		Files.writeString(
				languageDirectory.resolve("en_us.json"),
				"{\"dark_ship.i18n.world.block.rename_piece_solution_and_text_2\":\"the\"}",
				StandardCharsets.UTF_8
		);
		Files.writeString(
				languageDirectory.resolve("zh_cn.json"),
				"{\"dark_ship.i18n.world.block.rename_piece_solution_and_text_2\":\"谜底\"}",
				StandardCharsets.UTF_8
		);

		AtomicReference<Map<String, String>> literalTranslations = privateAtomicReference(
				ParaTranzContext.class,
				"WORLD_LITERAL_TRANSLATIONS"
		);
		AtomicReference<Map<String, String>> sourceLiteralTranslations = privateAtomicReference(
				ParaTranzContext.class,
				"WORLD_SOURCE_LITERAL_TRANSLATIONS"
		);
		Map<String, String> previousTranslations = literalTranslations.get();
		Map<String, String> previousSourceTranslations = sourceLiteralTranslations.get();
		try {
			ParaTranzContext.updateActiveConfig(new ParaTranzConfig("", "en_us", "zh_cn", true, 1, false));
			WorldLanguageContext.enterWorld(worldDirectory);

			Map<String, String> mergedTranslations = new java.util.HashMap<>();
			WorldLanguageContext.mergeTranslations(List.of("zh_cn"), mergedTranslations);

			check("谜底".equals(mergedTranslations.get("dark_ship.i18n.world.block.rename_piece_solution_and_text_2")), "expected selected target language to be merged");
			check("the".equals(ParaTranzContext.sourceLiteralWorldTextForDisplay("谜底").orElseThrow()), "expected source display to use en_us world language text");
		} finally {
			WorldLanguageContext.leaveWorld();
			literalTranslations.set(previousTranslations);
			sourceLiteralTranslations.set(previousSourceTranslations);
			ParaTranzContext.updateActiveConfig(ParaTranzConfig.defaultConfig());
		}
	}

	private static void translatesStyledLiteralSignComponents() throws Exception {
		AtomicReference<Map<String, String>> literalTranslations = privateAtomicReference(
				ParaTranzContext.class,
				"ACTIVE_LITERAL_TRANSLATIONS"
		);
		AtomicReference<Map<String, String>> sourceLiteralTranslations = privateAtomicReference(
				ParaTranzContext.class,
				"ACTIVE_SOURCE_LITERAL_TRANSLATIONS"
		);
		AtomicBoolean sourceLanguageActive = privateAtomicBoolean(
				Class.forName("com.amagari.translationtool.client.bilingual.BilingualLanguageController"),
				"SOURCE_LANGUAGE_ACTIVE"
		);
		Map<String, String> previousTranslations = literalTranslations.get();
		Map<String, String> previousSourceTranslations = sourceLiteralTranslations.get();
		boolean previousSourceLanguageActive = sourceLanguageActive.get();
		try {
			literalTranslations.set(ParaTranzLiteralTranslations.index(Map.of(
					"permafrost.i18n.world.block.enable_fabulous",
					"启用高品质"
			)));
			sourceLiteralTranslations.set(ParaTranzLiteralTranslations.sourceIndex(
					Map.of("permafrost.i18n.world.block.enable_fabulous", "Enable Fabulous"),
					Map.of("permafrost.i18n.world.block.enable_fabulous", "启用高品质")
			));

			SignText sourceSignText = new SignText()
					.setMessage(0, Component.literal("Enable ").append(Component.literal("Fabulous")));
			SignText targetSignText = new SignText()
					.setMessage(0, Component.literal("启用").append(Component.literal("高品质")));

			sourceLanguageActive.set(false);
			check("启用高品质".equals(ParaTranzSignText.translate(sourceSignText).getMessage(0, false).getString()), "expected styled source sign text to translate to target text");

			sourceLanguageActive.set(true);
			check("Enable Fabulous".equals(ParaTranzSignText.translate(targetSignText).getMessage(0, false).getString()), "expected styled target sign text to restore source text");
		} finally {
			literalTranslations.set(previousTranslations);
			sourceLiteralTranslations.set(previousSourceTranslations);
			sourceLanguageActive.set(previousSourceLanguageActive);
		}
	}

	private static void preservesStyledBilingualSourceComponents() throws Exception {
		AtomicReference<ParaTranzZipTranslations.ParseResult> activeTranslations = privateActiveTranslationsReference();
		ParaTranzZipTranslations.ParseResult previousTranslations = activeTranslations.get();
		try {
			ParaTranzContext.updateActiveConfig(new ParaTranzConfig("", "en_us", "zh_cn", true, 1, false));
			activeTranslations.set(new ParaTranzZipTranslations.ParseResult(
					Map.of("en_us", Map.of("tooltip.test.damage", "Damage %s")),
					1,
					1,
					0
			));

			Component argument = Component.literal("+4").withStyle(ChatFormatting.AQUA);
			Component translated = Component.translatable("tooltip.test.damage", argument)
					.withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC);
			Component ownSource = BilingualSourceText.ownSourceComponent(translated).orElseThrow();

			check("Damage +4".equals(ownSource.getString()), "expected source component to format translation arguments");
			check(ownSource.getSiblings().size() == 2, "expected source component to keep literal and argument pieces separate");
			check(
					ownSource.getSiblings().get(0).getStyle().equals(Component.literal("").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC).getStyle()),
					"expected source literal text to inherit the translated component style"
			);
			check(
					ownSource.getSiblings().get(1).getStyle().equals(argument.getStyle()),
					"expected source argument to preserve its own component style"
			);

			Component tooltipLine = Component.empty()
					.append(translated)
					.append(Component.literal("!").withStyle(ChatFormatting.RED));
			check("Damage +4!".equals(BilingualSourceText.sourceComponent(tooltipLine).orElseThrow().getString()), "expected source tooltip line to keep literal layout siblings");
			check(BilingualSourceText.sourceComponent(Component.literal("纯字面量")).isEmpty(), "expected pure literal target text to be ignored");
		} finally {
			activeTranslations.set(previousTranslations);
			ParaTranzContext.updateActiveConfig(ParaTranzConfig.defaultConfig());
		}
	}

	private static void buildsClickableParaTranzProjectList() {
		List<Component> messages = WorldLanguageMessages.paraTranzClickableProjectList(
				List.of(new ParaTranzProject(19173, "Permafrost-i18n", 3, 0, "mc")),
				"en_us"
		);

		check(messages.size() == 2, "expected clickable project list header and one project entry");
		ClickEvent clickEvent = messages.get(1).getSiblings().getFirst().getStyle().getClickEvent();
		check(clickEvent != null, "expected project name to have a click event");
		check(clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND, "expected project click to run a command");
		check("/amagari_lang paratranz pull Permafrost-i18n".equals(clickEvent.getValue()), "expected project click to pull the project");
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

	@SuppressWarnings("unchecked")
	private static AtomicReference<Map<String, String>> privateAtomicReference(Class<?> owner, String fieldName) throws Exception {
		Field field = owner.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (AtomicReference<Map<String, String>>) field.get(null);
	}

	private static AtomicBoolean privateAtomicBoolean(Class<?> owner, String fieldName) throws Exception {
		Field field = owner.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (AtomicBoolean) field.get(null);
	}

	@SuppressWarnings("unchecked")
	private static AtomicReference<ParaTranzZipTranslations.ParseResult> privateActiveTranslationsReference() throws Exception {
		Field field = ParaTranzContext.class.getDeclaredField("ACTIVE_TRANSLATIONS");
		field.setAccessible(true);
		return (AtomicReference<ParaTranzZipTranslations.ParseResult>) field.get(null);
	}
}
