package com.amagari.translationtool.client.bilingual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class BilingualItemTooltip {
	private static final int MAX_SOURCE_WIDTH = 180;
	private static final int MAX_SOURCE_LINES = 12;
	private static final int LINE_HEIGHT = 10;
	private static final int PADDING = 4;
	private static final int GAP = 12;
	private static final int EDGE_MARGIN = 4;
	private static final int BACKGROUND = 0xF0100010;
	private static final int BORDER_TOP = 0xFF5050A0;
	private static final int BORDER_BOTTOM = 0xFF282850;

	private BilingualItemTooltip() {
	}

	public static void render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int mouseX, int mouseY) {
		if (!BilingualLanguageController.isSourceDisplayActive() || stack.isEmpty()) {
			return;
		}

		List<Component> tooltipLines = tooltipLines(stack);
		List<Component> sourceLines = sourceLines(tooltipLines, stack);
		if (!sourceLines.isEmpty()) {
			renderSourceTooltip(graphics, font, stack, tooltipLines, sourceLines, mouseX, mouseY);
		}
	}

	private static List<Component> tooltipLines(ItemStack stack) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return List.of(stack.getHoverName());
		}
		return Screen.getTooltipFromItem(client, stack);
	}

	private static List<Component> sourceLines(List<Component> tooltipLines, ItemStack stack) {
		List<Component> sourceLines = new ArrayList<>();
		for (Component tooltipLine : tooltipLines) {
			BilingualSourceText.sourceComponent(tooltipLine).ifPresent(sourceLines::add);
			if (sourceLines.size() >= MAX_SOURCE_LINES) {
				break;
			}
		}
		if (sourceLines.isEmpty()) {
			BilingualSourceTranslations.sourceText(stack.getItem().getDescriptionId())
					.filter(sourceText -> !sourceText.isBlank())
					.map(sourceText -> Component.literal(sourceText).withStyle(stack.getHoverName().getStyle()))
					.ifPresent(sourceLines::add);
		}
		return sourceLines;
	}

	private static void renderSourceTooltip(GuiGraphicsExtractor graphics, Font font, ItemStack stack, List<Component> tooltipLines, List<Component> sourceText, int mouseX, int mouseY) {
		List<FormattedCharSequence> lines = sourceText.stream()
				.flatMap(component -> font.split(component, MAX_SOURCE_WIDTH).stream())
				.limit(MAX_SOURCE_LINES)
				.toList();
		if (lines.isEmpty()) {
			return;
		}

		int sourceWidth = lines.stream().mapToInt(font::width).max().orElse(0);
		int originalWidth = originalTooltipWidth(font, stack, tooltipLines);
		int originalX = vanillaTooltipX(graphics, mouseX, originalWidth);
		int boxWidth = sourceWidth + PADDING * 2;
		int boxHeight = lines.size() * LINE_HEIGHT + PADDING * 2;
		int x = originalX + originalWidth + GAP;
		if (x + boxWidth > graphics.guiWidth() - EDGE_MARGIN) {
			x = originalX - boxWidth - GAP;
		}
		x = clamp(x, EDGE_MARGIN, Math.max(EDGE_MARGIN, graphics.guiWidth() - boxWidth - EDGE_MARGIN));

		int y = mouseY - 12;
		if (y + boxHeight > graphics.guiHeight() - EDGE_MARGIN) {
			y = graphics.guiHeight() - boxHeight - EDGE_MARGIN;
		}
		y = clamp(y, EDGE_MARGIN, Math.max(EDGE_MARGIN, graphics.guiHeight() - boxHeight - EDGE_MARGIN));

		renderBox(graphics, font, lines, x, y, sourceWidth, boxHeight);
	}

	private static int originalTooltipWidth(Font font, ItemStack stack, List<Component> tooltipLines) {
		return tooltipLines.stream()
				.mapToInt(font::width)
				.max()
				.orElseGet(() -> font.width(stack.getHoverName()));
	}

	private static int vanillaTooltipX(GuiGraphicsExtractor graphics, int mouseX, int tooltipWidth) {
		int x = mouseX + 12;
		if (x + tooltipWidth + EDGE_MARGIN > graphics.guiWidth()) {
			x = mouseX - 16 - tooltipWidth;
		}
		return clamp(x, EDGE_MARGIN, Math.max(EDGE_MARGIN, graphics.guiWidth() - tooltipWidth - EDGE_MARGIN));
	}

	private static void renderBox(GuiGraphicsExtractor graphics, Font font, List<FormattedCharSequence> lines, int x, int y, int textWidth, int boxHeight) {
		int right = x + textWidth + PADDING * 2;
		int bottom = y + boxHeight;

		graphics.nextStratum();
		graphics.fill(x, y, right, bottom, BACKGROUND);
		graphics.fill(x, y, right, y + 1, BORDER_TOP);
		graphics.fill(x, bottom - 1, right, bottom, BORDER_BOTTOM);
		graphics.fill(x, y, x + 1, bottom, BORDER_TOP);
		graphics.fill(right - 1, y, right, bottom, BORDER_BOTTOM);
		for (int index = 0; index < lines.size(); index++) {
			graphics.text(font, lines.get(index), x + PADDING, y + PADDING + index * LINE_HEIGHT, 0xFFE0E0E0, false);
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
