package com.amagari.translationtool.client.paratranz;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ParaTranzLiteralTranslations {
	private static final String WORLD_BLOCK_MARKER = ".world.block.";
	private static final Map<String, String> SMALL_WORDS = Map.of(
			"a", "a",
			"an", "an",
			"and", "and",
			"for", "for",
			"if", "if",
			"of", "of",
			"or", "or",
			"the", "the"
	);

	private ParaTranzLiteralTranslations() {
	}

	public static Map<String, String> index(Map<String, String> translations) {
		Map<String, String> literalTranslations = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : translations.entrySet()) {
			int markerIndex = entry.getKey().indexOf(WORLD_BLOCK_MARKER);
			if (markerIndex < 0) {
				continue;
			}

			String key = normalize(entry.getKey().substring(markerIndex + WORLD_BLOCK_MARKER.length()));
			if (!key.isEmpty()) {
				literalTranslations.put(key, entry.getValue());
			}
		}
		return Map.copyOf(literalTranslations);
	}

	public static Map<String, String> sourceIndex(Map<String, String> sourceTranslations, Map<String, String> targetTranslations) {
		Map<String, String> literalTranslations = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : targetTranslations.entrySet()) {
			int markerIndex = entry.getKey().indexOf(WORLD_BLOCK_MARKER);
			if (markerIndex < 0 || entry.getValue().isBlank()) {
				continue;
			}

			String key = normalize(entry.getValue());
			if (key.isEmpty()) {
				continue;
			}

			String sourceText = sourceTranslations.get(entry.getKey());
			if (sourceText == null || sourceText.isBlank() || sourceText.equals(entry.getValue())) {
				sourceText = sourceTextFromKey(entry.getKey().substring(markerIndex + WORLD_BLOCK_MARKER.length()));
			}
			if (!sourceText.isBlank()) {
				literalTranslations.put(key, sourceText);
			}
		}
		return Map.copyOf(literalTranslations);
	}

	public static Optional<String> translate(String text, Map<String, String> literalTranslations) {
		String key = normalize(text);
		if (literalTranslations.containsKey(key)) {
			return Optional.ofNullable(literalTranslations.get(key));
		}

		String withoutArticle = stripTrailingArticle(key);
		if (!withoutArticle.equals(key) && literalTranslations.containsKey(withoutArticle)) {
			return Optional.ofNullable(literalTranslations.get(withoutArticle));
		}
		return Optional.empty();
	}

	private static String stripTrailingArticle(String key) {
		for (String article : new String[]{"_a", "_an", "_the"}) {
			if (key.endsWith(article)) {
				return key.substring(0, key.length() - article.length());
			}
		}
		return key;
	}

	private static String normalize(String text) {
		String lowerCaseText = text.trim().toLowerCase(Locale.ROOT);
		StringBuilder normalized = new StringBuilder(lowerCaseText.length());
		boolean separatorPending = false;
		for (int index = 0; index < lowerCaseText.length(); index++) {
			char character = lowerCaseText.charAt(index);
			if (Character.isLetterOrDigit(character)) {
				if (separatorPending && !normalized.isEmpty()) {
					normalized.append('_');
				}
				normalized.append(character);
				separatorPending = false;
			} else {
				separatorPending = true;
			}
		}
		return normalized.toString();
	}

	private static String sourceTextFromKey(String key) {
		String[] words = key.replace('.', '_').replace('-', '_').split("_+");
		StringBuilder sourceText = new StringBuilder(key.length());
		for (String word : words) {
			if (word.isBlank()) {
				continue;
			}
			if (!sourceText.isEmpty()) {
				sourceText.append(' ');
			}
			sourceText.append(sourceText.isEmpty() ? capitalized(word) : SMALL_WORDS.getOrDefault(word, capitalized(word)));
		}
		return sourceText.toString();
	}

	private static String capitalized(String word) {
		if (word.isBlank()) {
			return word;
		}
		if (word.length() == 1) {
			return word.toUpperCase(Locale.ROOT);
		}
		return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
	}
}
