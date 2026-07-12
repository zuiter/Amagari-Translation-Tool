package com.amagari.translationtool.client.bilingual;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class BilingualBookText {
	private static final String SOURCE_MARK = " ⓘ";
	private static final String SOURCE_INSERTION_PREFIX = "amagari_translation_tool:book_source:";
	private static final AtomicInteger CACHE_VERSION = new AtomicInteger();

	private BilingualBookText() {
	}

	public static Component withSourceHover(Component component) {
		if (!BilingualLanguageController.isSourceDisplayActive()) {
			return component;
		}

		if (!containsSourceText(component)) {
			return component;
		}

		MutableComponent flattened = Component.empty();
		appendWithSourceHover(flattened, component, Style.EMPTY);
		return flattened;
	}

	public static void invalidateCache() {
		CACHE_VERSION.incrementAndGet();
	}

	public static int cacheVersion() {
		return CACHE_VERSION.get();
	}

	public static Optional<Component> sourceFromStyle(Style style) {
		if (style == null || style.getInsertion() == null || !style.getInsertion().startsWith(SOURCE_INSERTION_PREFIX)) {
			return Optional.empty();
		}
		String sourceText = style.getInsertion().substring(SOURCE_INSERTION_PREFIX.length());
		return sourceText.isBlank() ? Optional.empty() : Optional.of(Component.literal(sourceText));
	}

	private static boolean containsSourceText(Component component) {
		if (sourceComponent(component).isPresent()) {
			return true;
		}
		for (Component sibling : component.getSiblings()) {
			if (containsSourceText(sibling)) {
				return true;
			}
		}
		return false;
	}

	private static void appendWithSourceHover(MutableComponent flattened, Component component, Style inheritedStyle) {
		Style resolvedStyle = component.getStyle().applyTo(inheritedStyle);
		flattened.append(MutableComponent.create(component.getContents()).setStyle(resolvedStyle));
		for (Component sibling : component.getSiblings()) {
			appendWithSourceHover(flattened, sibling, resolvedStyle);
		}
		sourceComponent(component).ifPresent(sourceText -> flattened.append(Component.literal(SOURCE_MARK)
				.withStyle(ChatFormatting.AQUA)
				.withStyle(style -> style.withInsertion(SOURCE_INSERTION_PREFIX + sourceText.getString()))));
	}

	private static Optional<Component> sourceComponent(Component component) {
		return BilingualSourceText.ownSourceComponent(component);
	}
}
