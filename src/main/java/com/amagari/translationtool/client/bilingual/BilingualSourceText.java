package com.amagari.translationtool.client.bilingual;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BilingualSourceText {
	private static final Pattern FORMAT_TOKEN = Pattern.compile("%(?:(\\d+)\\$)?[-#+ 0,(<]*\\d*(?:\\.\\d+)?([A-Za-z%])");

	private BilingualSourceText() {
	}

	public static Optional<String> sourceText(Component component) {
		StringBuilder text = new StringBuilder();
		boolean hasSource = appendSourceText(component, text);
		String sourceText = text.toString().trim();
		return hasSource && !sourceText.isBlank() ? Optional.of(sourceText) : Optional.empty();
	}

	public static Optional<String> ownSourceText(Component component) {
		StringBuilder text = new StringBuilder();
		boolean hasSource = appendOwnSourceText(component, text);
		String sourceText = text.toString().trim();
		return hasSource && !sourceText.isBlank() ? Optional.of(sourceText) : Optional.empty();
	}

	private static boolean appendSourceText(Component component, StringBuilder text) {
		boolean hasSource = appendOwnSourceText(component, text);
		for (Component sibling : component.getSiblings()) {
			hasSource |= appendSourceText(sibling, text);
		}
		return hasSource;
	}

	private static boolean appendOwnSourceText(Component component, StringBuilder text) {
		ComponentContents contents = component.getContents();
		if (!(contents instanceof TranslatableContents translatableContents)) {
			return false;
		}

		Optional<String> sourcePattern = BilingualSourceTranslations.sourceText(translatableContents.getKey())
				.filter(pattern -> !pattern.isBlank());
		if (sourcePattern.isEmpty()) {
			return false;
		}

		text.append(applyArgs(sourcePattern.get(), translatableContents.getArgs()));
		return true;
	}

	private static String applyArgs(String pattern, Object[] args) {
		Matcher matcher = FORMAT_TOKEN.matcher(pattern);
		StringBuilder result = new StringBuilder();
		int nextArg = 0;
		while (matcher.find()) {
			String conversion = matcher.group(2);
			if ("%".equals(conversion)) {
				matcher.appendReplacement(result, Matcher.quoteReplacement("%"));
				continue;
			}

			int argIndex = matcher.group(1) == null ? nextArg++ : Integer.parseInt(matcher.group(1)) - 1;
			matcher.appendReplacement(result, Matcher.quoteReplacement(argumentText(args, argIndex)));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private static String argumentText(Object[] args, int index) {
		if (index < 0 || index >= args.length) {
			return "";
		}
		Object argument = args[index];
		if (argument instanceof Component component) {
			return sourceText(component).orElseGet(component::getString);
		}
		return String.valueOf(argument);
	}
}
