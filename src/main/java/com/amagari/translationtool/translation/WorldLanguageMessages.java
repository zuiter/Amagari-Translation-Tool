package com.amagari.translationtool.translation;

import com.amagari.translationtool.client.paratranz.ParaTranzProject;
import com.amagari.translationtool.client.paratranz.ParaTranzReport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class WorldLanguageMessages {
	private WorldLanguageMessages() {
	}

	public static boolean isChinese(String languageCode) {
		return languageCode != null && languageCode.toLowerCase(Locale.ROOT).startsWith("zh_");
	}

	public static String unsupportedClient(String languageCode) {
		if (isChinese(languageCode)) {
			return "此客户端不支持 Amagari 地图语言命令。";
		}
		return "This client does not support Amagari world language commands.";
	}

	public static List<String> help(String languageCode) {
		if (isChinese(languageCode)) {
			return List.of(
					"Amagari Translation Tool 命令帮助：",
					"/amagari_lang help - 显示这份帮助。",
					"/amagari_lang reload - 重新加载当前地图语言文件。",
					"/amagari_lang status - 查看最近一次地图语言加载结果。",
					"/amagari_lang paratranz projects - 列出当前 API Token 可用的 ParaTranz 项目。",
					"/amagari_lang paratranz config - 打开 ParaTranz 设置页面。",
					"/amagari_lang paratranz pull <项目名> - 从 ParaTranz 拉取项目导出并应用。",
					"/amagari_lang pull - 从服务器请求最新地图语言清单。",
					"/amagari_lang push - OP 向在线玩家发布最新地图语言清单。"
			);
		}
		return List.of(
				"Amagari Translation Tool command help:",
				"/amagari_lang help - Show this help.",
				"/amagari_lang reload - Reload current world language files.",
				"/amagari_lang status - Show the latest world language load result.",
				"/amagari_lang paratranz projects - List ParaTranz projects available to the API token.",
				"/amagari_lang paratranz config - Open the ParaTranz settings screen.",
				"/amagari_lang paratranz pull <project> - Pull and apply a ParaTranz project export.",
				"/amagari_lang pull - Request the latest world language manifest from the server.",
				"/amagari_lang push - Publish the latest world language manifest to online players. OP only."
		);
	}

	public static List<String> paraTranzHelp(String languageCode) {
		if (isChinese(languageCode)) {
			return List.of(
					"ParaTranz 命令：",
					"/amagari_lang paratranz projects - 列出可拉取项目。",
					"/amagari_lang paratranz config - 打开 ParaTranz 设置页面。",
					"/amagari_lang paratranz pull <项目名> - 拉取并应用指定项目。"
			);
		}
		return List.of(
				"ParaTranz commands:",
				"/amagari_lang paratranz projects - List pullable projects.",
				"/amagari_lang paratranz config - Open ParaTranz settings.",
				"/amagari_lang paratranz pull <project> - Pull and apply the selected project."
		);
	}

	public static String paraConfigTitle(String languageCode) {
		return isChinese(languageCode) ? "ATT ParaTranz 设置" : "ATT ParaTranz Settings";
	}

	public static String paraConfigTokenLabel(boolean hasToken, String languageCode) {
		if (isChinese(languageCode)) {
			return hasToken ? "Token（已设置，留空保留）" : "Token";
		}
		return hasToken ? "Token (set, leave empty to keep)" : "Token";
	}

	public static String paraConfigTokenHint(boolean hasToken, String languageCode) {
		if (isChinese(languageCode)) {
			return hasToken ? "留空保留已有 token" : "ParaTranz token";
		}
		return hasToken ? "Leave empty to keep token" : "ParaTranz token";
	}

	public static String paraConfigClearTokenLabel(String languageCode) {
		return isChinese(languageCode) ? "清除 token" : "Clear token";
	}

	public static String paraConfigSourceLabel(String languageCode) {
		return isChinese(languageCode) ? "源语言" : "Source";
	}

	public static String paraConfigTargetLabel(String languageCode) {
		return isChinese(languageCode) ? "目标语言" : "Target";
	}

	public static String paraConfigCacheCountLabel(String languageCode) {
		return isChinese(languageCode) ? "缓存数" : "Cached artifacts";
	}

	public static String paraConfigTriggerExportLabel(String languageCode) {
		return isChinese(languageCode) ? "拉取前触发导出" : "Trigger export";
	}

	public static String paraConfigOverwriteWorldFilesLabel(String languageCode) {
		return isChinese(languageCode) ? "覆盖当前地图语言文件" : "Overwrite current world language files";
	}

	public static String paraConfigSaveLabel(String languageCode) {
		return isChinese(languageCode) ? "保存" : "Save";
	}

	public static String paraConfigCancelLabel(String languageCode) {
		return isChinese(languageCode) ? "取消" : "Cancel";
	}

	public static String paraConfigSaved(String languageCode) {
		return isChinese(languageCode) ? "ParaTranz 设置已保存。" : "ParaTranz settings saved.";
	}

	public static String paraConfigFailed(String error, String languageCode) {
		if (isChinese(languageCode)) {
			return "ParaTranz 设置保存失败：" + error;
		}
		return "Failed to save ParaTranz settings: " + error;
	}

	public static String paraConfigInvalidCacheCount(String languageCode) {
		return isChinese(languageCode) ? "缓存数必须是正整数。" : "Cached artifact count must be a positive number.";
	}

	public static String bilingualSourceLanguageToggled(boolean enabled, String sourceLanguage, String targetLanguage, String feedbackLanguage) {
		if (isChinese(feedbackLanguage)) {
			return enabled
					? "双语切换：已切换到源语言 " + sourceLanguage + "。"
					: "双语切换：已切回目标语言 " + targetLanguage + "。";
		}
		return enabled
				? "Bilingual toggle: switched to source language " + sourceLanguage + "."
				: "Bilingual toggle: switched back to target language " + targetLanguage + ".";
	}

	public static String bilingualSourceDisplayToggled(boolean enabled, String sourceLanguage, String feedbackLanguage) {
		if (isChinese(feedbackLanguage)) {
			return enabled
					? "双语显示：已显示源语言 " + sourceLanguage + "。"
					: "双语显示：已关闭源语言显示。";
		}
		return enabled
				? "Bilingual display: showing source language " + sourceLanguage + "."
				: "Bilingual display: source language display disabled.";
	}

	public static String bilingualSourceUnavailable(String sourceLanguage, String feedbackLanguage) {
		if (isChinese(feedbackLanguage)) {
			return "双语功能：当前没有可用的源语言 " + sourceLanguage + " 翻译，请先拉取 ParaTranz 项目。";
		}
		return "Bilingual mode: no source language " + sourceLanguage + " translations are active. Pull a ParaTranz project first.";
	}

	public static String paraTranzProjectList(List<ParaTranzProject> projects, String languageCode) {
		if (projects.isEmpty()) {
			return isChinese(languageCode) ? "当前 API Token 没有关联的 ParaTranz 项目。" : "No ParaTranz projects are available to the API token.";
		}

		String projectList = projects.stream()
				.map(project -> project.name() + " (" + project.id() + ")")
				.collect(Collectors.joining(", "));
		if (isChinese(languageCode)) {
			return "当前 API Token 可用的 ParaTranz 项目：" + projectList;
		}
		return "ParaTranz projects available to the API token: " + projectList;
	}

	public static List<Component> paraTranzClickableProjectList(List<ParaTranzProject> projects, String languageCode) {
		if (projects.isEmpty()) {
			return List.of(Component.literal(paraTranzProjectList(projects, languageCode)));
		}

		List<Component> messages = new ArrayList<>();
		messages.add(Component.literal(isChinese(languageCode)
				? "当前 API Token 可用的 ParaTranz 项目（点击项目名直接拉取）："
				: "ParaTranz projects available to the API token (click a project name to pull):"));
		for (ParaTranzProject project : projects) {
			String command = "/amagari_lang paratranz pull " + project.name();
			MutableComponent line = Component.literal(" - ")
					.append(Component.literal(project.name()).withStyle(clickableProjectStyle(command)))
					.append(Component.literal(" (" + project.id() + ")"));
			messages.add(line);
		}
		return List.copyOf(messages);
	}

	private static Style clickableProjectStyle(String command) {
		return Style.EMPTY
				.withColor(ChatFormatting.AQUA)
				.withUnderlined(true)
				.withClickEvent(new ClickEvent.RunCommand(command));
	}

	public static String paraTranzProjectNotFound(String query, String languageCode) {
		if (isChinese(languageCode)) {
			return "未找到 ParaTranz 项目：" + query;
		}
		return "ParaTranz project not found: " + query;
	}

	public static String paraTranzProjectAmbiguous(String query, List<ParaTranzProject> candidates, String languageCode) {
		String projectList = candidates.stream()
				.map(project -> project.name() + " (" + project.id() + ")")
				.collect(Collectors.joining(", "));
		if (isChinese(languageCode)) {
			return "ParaTranz 项目名不唯一：" + query + "。候选：" + projectList;
		}
		return "ParaTranz project name is ambiguous: " + query + ". Candidates: " + projectList;
	}

	public static String paraTranzOverwriteSucceeded(Path languageDirectory, String targetLanguage, String feedbackLanguage) {
		if (isChinese(feedbackLanguage)) {
			return "ParaTranz：已覆盖当前地图语言文件 " + targetLanguage + ".json：" + languageDirectory;
		}
		return "ParaTranz: overwritten current world language file " + targetLanguage + ".json in " + languageDirectory + ".";
	}

	public static String paraTranzOverwriteSkippedNoWorld(String languageCode) {
		if (isChinese(languageCode)) {
			return "ParaTranz：当前没有可写入的本地地图语言目录，已跳过文件覆盖。";
		}
		return "ParaTranz: no writable local world language directory is active; skipped file overwrite.";
	}

	public static String paraTranzOverwriteSkippedNoTarget(String targetLanguage, String feedbackLanguage) {
		if (isChinese(feedbackLanguage)) {
			return "ParaTranz：拉取结果中没有 " + targetLanguage + " 翻译，已跳过文件覆盖。";
		}
		return "ParaTranz: no " + targetLanguage + " translations were found in the pull result; skipped file overwrite.";
	}

	public static String paraTranzOverwriteFailed(String error, String languageCode) {
		if (isChinese(languageCode)) {
			return "ParaTranz：覆盖当前地图语言文件失败：" + error;
		}
		return "ParaTranz: failed to overwrite current world language files: " + error;
	}

	public static String paraTranzStatus(ParaTranzReport report, String languageCode) {
		return switch (report.status()) {
			case IDLE -> isChinese(languageCode) ? "ParaTranz：尚未拉取项目。" : "ParaTranz: no project has been pulled.";
			case LISTED -> isChinese(languageCode)
					? "ParaTranz：已列出 " + report.loadedFiles() + " 个项目。"
					: "ParaTranz: listed " + report.loadedFiles() + " project(s).";
			case DOWNLOADING -> isChinese(languageCode)
					? "ParaTranz：正在拉取 " + report.project().name() + "。"
					: "ParaTranz: pulling " + report.project().name() + ".";
			case APPLIED -> paraTranzApplied(report, languageCode);
			case FAILED -> isChinese(languageCode)
					? "ParaTranz：失败：" + report.error()
					: "ParaTranz: failed: " + report.error();
		};
	}

	private static String paraTranzApplied(ParaTranzReport report, String languageCode) {
		String failedMessage = report.failedFiles() == 0 ? "" : failedFiles(report.failedFiles(), languageCode);
		String languages = String.join(", ", report.languageCodes());
		if (isChinese(languageCode)) {
			return "ParaTranz：已应用 " + report.project().name()
					+ "（项目 " + report.project().id()
					+ "，成品 " + report.artifact().id()
					+ "，时间 " + report.artifact().createdAt()
					+ "），从 " + report.loadedFiles() + " 个文件加载 " + report.loadedEntries()
					+ " 条翻译，覆盖 " + report.loadedLanguages()
					+ " 种语言：" + languages + failedMessage + "。";
		}
		return "ParaTranz: applied " + report.project().name()
				+ " (project " + report.project().id()
				+ ", artifact " + report.artifact().id()
				+ ", created " + report.artifact().createdAt()
				+ "), loaded " + report.loadedEntries()
				+ " translation entries from " + report.loadedFiles()
				+ " file(s) across " + report.loadedLanguages()
				+ " language(s): " + languages + failedMessage + ".";
	}

	public static String requestedManifest(boolean sent, String languageCode) {
		if (isChinese(languageCode)) {
			return sent ? "已请求最新地图语言清单。" : "没有可发送的地图语言清单。";
		}
		return sent ? "Requested latest world language manifest." : "No world language manifest was sent.";
	}

	public static String publishedManifest(int playerCount, String languageCode) {
		if (isChinese(languageCode)) {
			return "已向 " + playerCount + " 名玩家发布地图语言清单。";
		}
		return "Published world language manifest to " + playerCount + " player(s).";
	}

	public static String notLoaded(String languageCode) {
		if (isChinese(languageCode)) {
			return "当前没有已激活的单人地图语言目录。";
		}
		return "No singleplayer world language directory is active.";
	}

	public static String empty(Path languageDirectory, String languageCode) {
		if (isChinese(languageCode)) {
			return "未从 " + languageDirectory + " 加载到地图语言文件。";
		}
		return "No world language files loaded from " + languageDirectory + ".";
	}

	public static String loaded(int loadedEntries, int loadedFiles, int failedFiles, String loadedLanguageCode, String feedbackLanguageCode) {
		String failedMessage = failedFiles == 0 ? "" : failedFiles(failedFiles, feedbackLanguageCode);
		if (isChinese(feedbackLanguageCode)) {
			String languageMessage = loadedLanguageCode.isBlank() ? "" : "为 " + loadedLanguageCode + " ";
			return "已" + languageMessage + "从 " + loadedFiles + " 个地图语言文件加载 " + loadedEntries + " 条翻译"
					+ failedMessage + "。";
		}
		return "Loaded " + loadedEntries + " translation entries from " + loadedFiles + " world language file(s)"
				+ failedMessage
				+ (loadedLanguageCode.isBlank() ? "." : " for " + loadedLanguageCode + ".");
	}

	public static String remoteReady(int loadedEntries, int loadedLanguages, String languageCode) {
		if (isChinese(languageCode)) {
			return "已接收 " + loadedLanguages + " 种语言的 " + loadedEntries + " 条远程地图语言翻译。";
		}
		return "Received " + loadedEntries + " remote world language entries for " + loadedLanguages + " language(s).";
	}

	public static String remoteManifest(int languageCount, int cachedLanguageCount, int pendingLanguageCount, String languageCode) {
		if (isChinese(languageCode)) {
			return "已接收 " + languageCount + " 种语言的远程语言清单；" + cachedLanguageCount + " 个已缓存，" + pendingLanguageCount + " 个等待下载。";
		}
		return "Received remote language manifest for " + languageCount + " language(s); "
				+ cachedLanguageCount + " cached, " + pendingLanguageCount + " pending download.";
	}

	public static String remoteLoaded(int loadedLanguages, int loadedEntries, String languageCode) {
		if (isChinese(languageCode)) {
			return "已为 " + loadedLanguages + " 种当前语言加载 " + loadedEntries + " 条远程地图语言翻译。";
		}
		return "Loaded " + loadedEntries + " remote world language entries for " + loadedLanguages + " active language(s).";
	}

	public static String failed(Path languageDirectory, String error, String languageCode) {
		if (isChinese(languageCode)) {
			return "扫描地图语言目录 " + languageDirectory + " 失败：" + error;
		}
		return "Failed to scan world language directory " + languageDirectory + ": " + error;
	}

	private static String failedFiles(int failedFiles, String languageCode) {
		if (isChinese(languageCode)) {
			return "，" + failedFiles + " 个文件失败";
		}
		return "; " + failedFiles + " file(s) failed";
	}
}
