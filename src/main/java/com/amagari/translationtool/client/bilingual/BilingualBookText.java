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

		MutableComponent copied = copyWithSourceHover(component);
		return copied.equals(component) ? component : copied;
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

	private static MutableComponent copyWithSourceHover(Component component) {
		MutableComponent copied = MutableComponent.create(component.getContents()).setStyle(component.getStyle());
		for (Component sibling : component.getSiblings()) {
			copied.append(copyWithSourceHover(sibling));
		}
		sourceComponent(component).ifPresent(sourceText -> copied.append(Component.literal(SOURCE_MARK)
				.withStyle(ChatFormatting.AQUA)
				.withStyle(style -> style.withInsertion(SOURCE_INSERTION_PREFIX + sourceText.getString()))));
		return copied;
	}

	private static Optional<Component> sourceComponent(Component component) {
		return BilingualSourceText.ownSourceComponent(component);
	}
}
