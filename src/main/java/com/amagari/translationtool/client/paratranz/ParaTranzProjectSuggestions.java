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

	public static List<String> matchingQueries(List<ParaTranzProject> projects, String input) {
		String needle = input == null ? "" : input.trim();
		if (!needle.isBlank() && needle.chars().allMatch(character -> character >= '0' && character <= '9')) {
			return projects.stream()
					.map(ParaTranzProject::id)
					.sorted()
					.map(String::valueOf)
					.filter(projectId -> projectId.startsWith(needle))
					.toList();
		}
		return matchingNames(projects, input);
	}
}
