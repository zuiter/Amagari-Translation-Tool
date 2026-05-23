package com.amagari.translationtool.client.paratranz;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ParaTranzProjectSuggestions {
	private ParaTranzProjectSuggestions() {
	}

	public static List<String> matchingNames(List<ParaTranzProject> projects, String input) {
		String needle = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
		return projects.stream()
				.map(ParaTranzProject::name)
				.filter(name -> needle.isBlank() || name.toLowerCase(Locale.ROOT).contains(needle))
				.sorted(Comparator.comparing(name -> name.toLowerCase(Locale.ROOT)))
				.toList();
	}
}
