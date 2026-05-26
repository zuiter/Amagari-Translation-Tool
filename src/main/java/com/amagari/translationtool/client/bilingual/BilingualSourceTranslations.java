package com.amagari.translationtool.client.bilingual;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.client.paratranz.ParaTranzConfig;
import com.amagari.translationtool.client.paratranz.ParaTranzContext;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class BilingualSourceTranslations {
	private static final AtomicReference<Cache> CACHE = new AtomicReference<>(Cache.empty());

	private BilingualSourceTranslations() {
	}

	public static Optional<String> sourceText(String translationKey) {
		if (translationKey == null || translationKey.isBlank()) {
			return Optional.empty();
		}

		String sourceLanguage = ParaTranzContext.sourceLanguage();
		return preferredText(
				resourceTranslation(sourceLanguage, translationKey),
				ParaTranzContext.sourceTranslation(translationKey)
		);
	}

	public static boolean hasTranslations(String languageCode) {
		return !translations(languageCode).isEmpty();
	}

	public static void clearCache() {
		CACHE.set(Cache.empty());
	}

	static Optional<String> preferredText(Optional<String> resourceText, Optional<String> fallbackText) {
		return resourceText
				.filter(text -> !text.isBlank())
				.or(() -> fallbackText.filter(text -> !text.isBlank()));
	}

	private static Optional<String> resourceTranslation(String languageCode, String translationKey) {
		return Optional.ofNullable(translations(languageCode).get(translationKey));
	}

	private static Map<String, String> translations(String languageCode) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getResourceManager() == null) {
			return Map.of();
		}

		ResourceManager resourceManager = client.getResourceManager();
		String normalizedLanguage = normalizedLanguage(languageCode);
		Cache cached = CACHE.get();
		if (cached.matches(resourceManager, normalizedLanguage)) {
			return cached.translations();
		}

		Map<String, String> translations = loadResourceTranslations(resourceManager, normalizedLanguage);
		CACHE.set(new Cache(resourceManager, normalizedLanguage, translations));
		return translations;
	}

	private static Map<String, String> loadResourceTranslations(ResourceManager resourceManager, String languageCode) {
		Map<String, String> translations = new HashMap<>();
		for (String loadLanguage : loadOrder(languageCode)) {
			String path = String.format(Locale.ROOT, "lang/%s.json", loadLanguage);
			for (String namespace : resourceManager.getNamespaces()) {
				try {
					Identifier location = Identifier.fromNamespaceAndPath(namespace, path);
					appendFrom(loadLanguage, resourceManager.getResourceStack(location), translations);
				} catch (Exception exception) {
					AmagariTranslationTool.LOGGER.warn("Skipped source language file {}:{}", namespace, path, exception);
				}
			}
		}
		return Map.copyOf(translations);
	}

	private static void appendFrom(String languageCode, List<Resource> resources, Map<String, String> translations) {
		for (Resource resource : resources) {
			try (InputStream input = resource.open()) {
				Language.loadFromJson(input, translations::put);
			} catch (Exception exception) {
				AmagariTranslationTool.LOGGER.warn(
						"Failed to load source translations for {} from pack {}",
						languageCode,
						resource.sourcePackId(),
						exception
				);
			}
		}
	}

	private static List<String> loadOrder(String languageCode) {
		List<String> languages = new ArrayList<>();
		languages.add(Language.DEFAULT);
		if (!Language.DEFAULT.equals(languageCode)) {
			languages.add(languageCode);
		}
		return languages;
	}

	private static String normalizedLanguage(String languageCode) {
		if (languageCode == null || languageCode.isBlank()) {
			return ParaTranzConfig.DEFAULT_SOURCE_LANGUAGE;
		}
		return languageCode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
	}

	private record Cache(ResourceManager resourceManager, String languageCode, Map<String, String> translations) {
		private static Cache empty() {
			return new Cache(null, "", Map.of());
		}

		private boolean matches(ResourceManager currentResourceManager, String currentLanguageCode) {
			return resourceManager == currentResourceManager && languageCode.equals(currentLanguageCode);
		}
	}
}
