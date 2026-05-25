package com.amagari.translationtool;

import com.amagari.translationtool.client.paratranz.ParaTranzApiClient;
import com.amagari.translationtool.client.paratranz.ParaTranzProject;
import com.amagari.translationtool.client.paratranz.ParaTranzZipTranslations;

public final class ParaTranzSmokeCheck {
	private ParaTranzSmokeCheck() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1 || args[0].isBlank()) {
			throw new IllegalArgumentException("ParaTranz token argument is required");
		}

		ParaTranzApiClient.DownloadedArtifact artifact = new ParaTranzApiClient().exportAndDownload(
				new ParaTranzProject(19173, "Permafrost-i18n", 3, 0, "mc"),
				args[0].trim(),
				true
		);
		ParaTranzZipTranslations.ParseResult translations = ParaTranzZipTranslations.parse(artifact.zipData());

		if (artifact.artifact().id() <= 0) {
			throw new AssertionError("expected downloaded artifact id");
		}
		if (translations.loadedFiles() <= 0 || translations.loadedEntries() <= 0) {
			throw new AssertionError("expected downloaded artifact to contain translation JSON");
		}

		System.out.println("artifact=" + artifact.artifact().id()
				+ ", files=" + translations.loadedFiles()
				+ ", entries=" + translations.loadedEntries()
				+ ", languages=" + translations.translationsByLanguage().keySet());
	}
}
