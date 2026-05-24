package com.amagari.translationtool.client.bilingual;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;

public final class BilingualBookText {
	private static final String SOURCE_MARK = " ⓘ";

	private BilingualBookText() {
	}

	public static Component withSourceHover(Component component) {
		if (!BilingualLanguageController.isSourceDisplayActive()) {
			return component;
		}

		MutableComponent copied = copyWithSourceHover(component);
		return copied.equals(component) ? component : copied;
	}

	private static MutableComponent copyWithSourceHover(Component component) {
		MutableComponent copied = MutableComponent.create(component.getContents()).setStyle(sourceAwareStyle(component));
		for (Component sibling : component.getSiblings()) {
			copied.append(copyWithSourceHover(sibling));
		}
		sourceText(component).ifPresent(sourceText -> copied.append(Component.literal(SOURCE_MARK)
				.withStyle(ChatFormatting.AQUA)
				.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(sourceText))))));
		return copied;
	}

	private static Style sourceAwareStyle(Component component) {
		return sourceText(component)
				.map(sourceText -> component.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(sourceText))))
				.orElse(component.getStyle());
	}

	private static Optional<String> sourceText(Component component) {
		ComponentContents contents = component.getContents();
		if (!(contents instanceof TranslatableContents translatableContents)) {
			return Optional.empty();
		}
		return BilingualSourceTranslations.sourceText(translatableContents.getKey()).filter(text -> !text.isBlank());
	}
}
