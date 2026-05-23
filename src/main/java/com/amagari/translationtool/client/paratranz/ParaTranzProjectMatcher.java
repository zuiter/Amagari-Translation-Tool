package com.amagari.translationtool.client.paratranz;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ParaTranzProjectMatcher {
	private ParaTranzProjectMatcher() {
	}

	public static MatchResult match(List<ParaTranzProject> projects, String query) {
		String normalizedQuery = normalize(query);
		if (normalizedQuery.isBlank()) {
			return new MatchResult(MatchStatus.NOT_FOUND, Optional.empty(), List.of());
		}

		List<ParaTranzProject> exactMatches = projects.stream()
				.filter(project -> normalize(project.name()).equals(normalizedQuery))
				.toList();
		if (exactMatches.size() == 1) {
			return MatchResult.matched(exactMatches.get(0));
		}
		if (exactMatches.size() > 1) {
			return MatchResult.ambiguous(exactMatches);
		}

		List<ParaTranzProject> partialMatches = projects.stream()
				.filter(project -> normalize(project.name()).contains(normalizedQuery))
				.toList();
		if (partialMatches.size() == 1) {
			return MatchResult.matched(partialMatches.get(0));
		}
		if (partialMatches.size() > 1) {
			return MatchResult.ambiguous(partialMatches);
		}

		return new MatchResult(MatchStatus.NOT_FOUND, Optional.empty(), List.of());
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	public record MatchResult(
			MatchStatus status,
			Optional<ParaTranzProject> project,
			List<ParaTranzProject> candidates
	) {
		private static MatchResult matched(ParaTranzProject project) {
			return new MatchResult(MatchStatus.MATCHED, Optional.of(project), List.of(project));
		}

		private static MatchResult ambiguous(List<ParaTranzProject> candidates) {
			return new MatchResult(MatchStatus.AMBIGUOUS, Optional.empty(), List.copyOf(candidates));
		}
	}

	public enum MatchStatus {
		MATCHED,
		AMBIGUOUS,
		NOT_FOUND
	}
}
