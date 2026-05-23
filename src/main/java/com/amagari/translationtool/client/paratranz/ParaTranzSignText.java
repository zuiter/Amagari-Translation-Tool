package com.amagari.translationtool.client.paratranz;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.world.level.block.entity.SignText;

public final class ParaTranzSignText {
	private ParaTranzSignText() {
	}

	public static SignText translate(SignText signText) {
		SignText translated = signText;
		for (int line = 0; line < SignText.LINES; line++) {
			Component message = signText.getMessage(line, false);
			Component filteredMessage = signText.getMessage(line, true);
			Component translatedMessage = translate(message);
			Component translatedFilteredMessage = translate(filteredMessage);
			if (translatedMessage != message || translatedFilteredMessage != filteredMessage) {
				translated = translated.setMessage(line, translatedMessage, translatedFilteredMessage);
			}
		}
		return translated;
	}

	private static Component translate(Component component) {
		if (!(component.getContents() instanceof PlainTextContents plainText) || !component.getSiblings().isEmpty()) {
			return component;
		}

		return ParaTranzContext.translateLiteralWorldText(plainText.text())
				.<Component>map(text -> Component.literal(text).setStyle(component.getStyle()))
				.orElse(component);
	}
}
