package com.amagari.translationtool.client.paratranz;

import com.amagari.translationtool.translation.WorldLanguageMessages;

import java.util.List;

public record ParaTranzReport(
		Status status,
		ParaTranzProject project,
		ParaTranzArtifact artifact,
		int loadedFiles,
		int failedFiles,
		int loadedEntries,
		List<String> languageCodes,
		String error
) {
	public static ParaTranzReport idle() {
		return new ParaTranzReport(Status.IDLE, null, null, 0, 0, 0, List.of(), "");
	}

	public static ParaTranzReport listing(int projects) {
		return new ParaTranzReport(Status.LISTED, null, null, projects, 0, 0, List.of(), "");
	}

	public static ParaTranzReport downloading(ParaTranzProject project) {
		return new ParaTranzReport(Status.DOWNLOADING, project, null, 0, 0, 0, List.of(), "");
	}

	public static ParaTranzReport applied(ParaTranzProject project, ParaTranzArtifact artifact, int loadedFiles, int failedFiles, int loadedEntries, List<String> languageCodes) {
		return new ParaTranzReport(Status.APPLIED, project, artifact, loadedFiles, failedFiles, loadedEntries, List.copyOf(languageCodes), "");
	}

	public static ParaTranzReport failed(String error) {
		return new ParaTranzReport(Status.FAILED, null, null, 0, 0, 0, List.of(), error);
	}

	public int loadedLanguages() {
		return languageCodes.size();
	}

	public String describe(String languageCode) {
		return WorldLanguageMessages.paraTranzStatus(this, languageCode);
	}

	public enum Status {
		IDLE,
		LISTED,
		DOWNLOADING,
		APPLIED,
		FAILED
	}
}
