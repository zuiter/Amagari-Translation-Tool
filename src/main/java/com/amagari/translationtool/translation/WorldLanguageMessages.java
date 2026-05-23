package com.amagari.translationtool.translation;

import com.amagari.translationtool.client.paratranz.ParaTranzProject;
import com.amagari.translationtool.client.paratranz.ParaTranzReport;

import java.nio.file.Path;
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
					"/amagari_lang paratranz - 列出当前 API Token 可用的 ParaTranz 项目。",
					"/amagari_lang paratranz <项目名> - 从 ParaTranz 拉取项目导出并应用。",
					"/amagari_lang pull - 从服务器请求最新地图语言清单。",
					"/amagari_lang push - OP 向在线玩家发布最新地图语言清单。"
			);
		}
		return List.of(
				"Amagari Translation Tool command help:",
				"/amagari_lang help - Show this help.",
				"/amagari_lang reload - Reload current world language files.",
				"/amagari_lang status - Show the latest world language load result.",
				"/amagari_lang paratranz - List ParaTranz projects available to the API token.",
				"/amagari_lang paratranz <project> - Pull and apply a ParaTranz project export.",
				"/amagari_lang pull - Request the latest world language manifest from the server.",
				"/amagari_lang push - Publish the latest world language manifest to online players. OP only."
		);
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
