package com.amagari.translationtool.client.bilingual;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BilingualSourceText {
	private static final Pattern FORMAT_TOKEN = Pattern.compile("%(?:(\\d+)\\$)?[-#+ 0,(<]*\\d*(?:\\.\\d+)?([A-Za-z%])");

	private BilingualSourceText() {
	}

	public static Optional<Component> sourceComponent(Component component) {
		SourceComponentResult source = sourceComponentInternal(component);
		if (!source.hasSource() || source.component().getString().isBlank()) {
			return Optional.empty();
		}
		return Optional.of(source.component());
	}

	public static Optional<Component> ownSourceComponent(Component component) {
		return ownSourceComponentInternal(component)
				.filter(source -> !source.getString().isBlank())
				.map(source -> (Component) source);
	}

	private static SourceComponentResult sourceComponentInternal(Component component) {
		Optional<MutableComponent> ownSource = ownSourceComponentInternal(component);
		MutableComponent output = ownSource.orElseGet(() -> fallbackOwnComponent(component));
		boolean hasSource = ownSource.isPresent();
		for (Component sibling : component.getSiblings()) {
			SourceComponentResult siblingSource = sourceComponentInternal(sibling);
			output.append(siblingSource.component());
			hasSource |= siblingSource.hasSource();
		}
		return new SourceComponentResult(output, hasSource);
	}

	private static Optional<MutableComponent> ownSourceComponentInternal(Component component) {
		ComponentContents contents = component.getContents();
		if (!(contents instanceof TranslatableContents translatableContents)) {
			return Optional.empty();
		}

		return BilingualSourceTranslations.sourceText(translatableContents.getKey())
				.filter(pattern -> !pattern.isBlank())
				.map(pattern -> applyArgs(pattern, translatableContents.getArgs(), component.getStyle()));
	}

	private static MutableComponent fallbackOwnComponent(Component component) {
		if (component.getContents() instanceof TranslatableContents) {
			return Component.empty();
		}
		return MutableComponent.create(component.getContents()).setStyle(component.getStyle());
	}

	private static MutableComponent applyArgs(String pattern, Object[] args, Style baseStyle) {
		Matcher matcher = FORMAT_TOKEN.matcher(pattern);
		MutableComponent result = Component.empty();
		int nextArg = 0;
		int lastEnd = 0;
		while (matcher.find()) {
			if (matcher.start() > lastEnd) {
				appendLiteral(result, pattern.substring(lastEnd, matcher.start()), baseStyle);
			}

			String conversion = matcher.group(2);
			if ("%".equals(conversion)) {
				appendLiteral(result, "%", baseStyle);
			} else {
				int argIndex = matcher.group(1) == null ? nextArg++ : Integer.parseInt(matcher.group(1)) - 1;
				result.append(argumentComponent(args, argIndex, baseStyle));
			}
			lastEnd = matcher.end();
		}
		if (lastEnd < pattern.length()) {
			appendLiteral(result, pattern.substring(lastEnd), baseStyle);
		}
		return result;
	}

	private static void appendLiteral(MutableComponent component, String text, Style style) {
		if (!text.isEmpty()) {
			component.append(Component.literal(text).withStyle(style));
		}
	}

	private static Component argumentComponent(Object[] args, int index, Style fallbackStyle) {
		if (index < 0 || index >= args.length) {
			return Component.empty();
		}
		Object argument = args[index];
		if (argument instanceof Component component) {
			return sourceComponent(component).orElse(component.copy());
		}
		return Component.literal(String.valueOf(argument)).withStyle(fallbackStyle);
	}

	private record SourceComponentResult(MutableComponent component, boolean hasSource) {
	}
}
