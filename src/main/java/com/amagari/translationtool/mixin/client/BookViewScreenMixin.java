package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.bilingual.BilingualBookText;
import com.amagari.translationtool.client.bilingual.BilingualLanguageController;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BookViewScreen.class)
public class BookViewScreenMixin {
	@Unique
	private static final int amagari_translation_tool$SOURCE_TOOLTIP_WIDTH = 128;
	@Unique
	private static final int amagari_translation_tool$SOURCE_TOOLTIP_MAX_LINES = 12;
	@Unique
	private static final int amagari_translation_tool$SOURCE_TOOLTIP_LINE_HEIGHT = 10;
	@Unique
	private static final int amagari_translation_tool$SOURCE_TOOLTIP_PADDING = 7;
	@Unique
	private static final int amagari_translation_tool$SOURCE_TOOLTIP_EDGE_MARGIN = 6;
	@Unique
	private static final int amagari_translation_tool$SOURCE_TOOLTIP_BACKGROUND = 0xFFF8EFCF;
	@Unique
	private static final int amagari_translation_tool$SOURCE_TOOLTIP_INNER_BACKGROUND = 0xFFFFF8DC;
	@Unique
	private static final int amagari_translation_tool$SOURCE_TOOLTIP_BORDER_DARK = 0xFF8A4F2A;
	@Unique
	private static final int amagari_translation_tool$SOURCE_TOOLTIP_BORDER_LIGHT = 0xFFD9B987;
	@Unique
	private static final int amagari_translation_tool$SOURCE_TOOLTIP_TEXT = 0xFF1F1308;

	@Shadow
	protected Font font;

	@Shadow
	private void visitText(ActiveTextCollector textCollector, boolean includePageNumber) {
		throw new AssertionError();
	}

	@Shadow
	private int cachedPage;

	@Unique
	private int amagari_translation_tool$lastSourceHoverVersion = -1;

	@Unique
	private boolean amagari_translation_tool$lastSourceDisplayActive;

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void amagari_translation_tool$refreshSourceHoverCache(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
		int version = BilingualBookText.cacheVersion();
		boolean active = BilingualLanguageController.isSourceDisplayActive();
		if (version != amagari_translation_tool$lastSourceHoverVersion || active != amagari_translation_tool$lastSourceDisplayActive) {
			cachedPage = -1;
			amagari_translation_tool$lastSourceHoverVersion = version;
			amagari_translation_tool$lastSourceDisplayActive = active;
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void amagari_translation_tool$renderBookSourceTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
		if (!BilingualLanguageController.isSourceDisplayActive()) {
			return;
		}

		ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(font, mouseX, mouseY).includeInsertions(true);
		visitText(finder, false);
		Style hoveredStyle = finder.result();
		BilingualBookText.sourceFromStyle(hoveredStyle).ifPresent(sourceText -> amagari_translation_tool$renderSourceTooltip(graphics, sourceText, mouseX, mouseY));
	}

	@ModifyArg(
			method = "visitText",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"
			),
			index = 0
	)
	private FormattedText amagari_translation_tool$addSourceHoverToBookText(FormattedText pageText) {
		if (pageText instanceof net.minecraft.network.chat.Component component) {
			return BilingualBookText.withSourceHover(component);
		}
		return pageText;
	}

	@Unique
	private void amagari_translation_tool$renderSourceTooltip(GuiGraphicsExtractor graphics, Component sourceText, int mouseX, int mouseY) {
		List<FormattedCharSequence> lines = font.split(sourceText, amagari_translation_tool$SOURCE_TOOLTIP_WIDTH).stream()
				.limit(amagari_translation_tool$SOURCE_TOOLTIP_MAX_LINES)
				.toList();
		if (lines.isEmpty()) {
			return;
		}

		int textWidth = lines.stream().mapToInt(font::width).max().orElse(0);
		int boxWidth = textWidth + amagari_translation_tool$SOURCE_TOOLTIP_PADDING * 2;
		int boxHeight = lines.size() * amagari_translation_tool$SOURCE_TOOLTIP_LINE_HEIGHT + amagari_translation_tool$SOURCE_TOOLTIP_PADDING * 2;
		int x = mouseX + 12;
		if (x + boxWidth > graphics.guiWidth() - amagari_translation_tool$SOURCE_TOOLTIP_EDGE_MARGIN) {
			x = mouseX - boxWidth - 12;
		}
		int maxX = Math.max(amagari_translation_tool$SOURCE_TOOLTIP_EDGE_MARGIN, graphics.guiWidth() - boxWidth - amagari_translation_tool$SOURCE_TOOLTIP_EDGE_MARGIN);
		x = amagari_translation_tool$clamp(x, amagari_translation_tool$SOURCE_TOOLTIP_EDGE_MARGIN, maxX);
		int y = mouseY - 12;
		if (y + boxHeight > graphics.guiHeight() - amagari_translation_tool$SOURCE_TOOLTIP_EDGE_MARGIN) {
			y = graphics.guiHeight() - boxHeight - amagari_translation_tool$SOURCE_TOOLTIP_EDGE_MARGIN;
		}
		int maxY = Math.max(amagari_translation_tool$SOURCE_TOOLTIP_EDGE_MARGIN, graphics.guiHeight() - boxHeight - amagari_translation_tool$SOURCE_TOOLTIP_EDGE_MARGIN);
		y = amagari_translation_tool$clamp(y, amagari_translation_tool$SOURCE_TOOLTIP_EDGE_MARGIN, maxY);

		graphics.nextStratum();
		amagari_translation_tool$renderPaperBox(graphics, x, y, boxWidth, boxHeight);
		for (int index = 0; index < lines.size(); index++) {
			int textY = y + amagari_translation_tool$SOURCE_TOOLTIP_PADDING + index * amagari_translation_tool$SOURCE_TOOLTIP_LINE_HEIGHT;
			graphics.text(font, lines.get(index), x + amagari_translation_tool$SOURCE_TOOLTIP_PADDING, textY, amagari_translation_tool$SOURCE_TOOLTIP_TEXT, false);
		}
	}

	@Unique
	private static void amagari_translation_tool$renderPaperBox(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		int right = x + width;
		int bottom = y + height;
		graphics.fill(x, y, right, bottom, amagari_translation_tool$SOURCE_TOOLTIP_BACKGROUND);
		graphics.fill(x + 2, y + 2, right - 2, bottom - 2, amagari_translation_tool$SOURCE_TOOLTIP_INNER_BACKGROUND);
		graphics.fill(x, y, right, y + 1, amagari_translation_tool$SOURCE_TOOLTIP_BORDER_DARK);
		graphics.fill(x, bottom - 1, right, bottom, amagari_translation_tool$SOURCE_TOOLTIP_BORDER_DARK);
		graphics.fill(x, y, x + 1, bottom, amagari_translation_tool$SOURCE_TOOLTIP_BORDER_DARK);
		graphics.fill(right - 1, y, right, bottom, amagari_translation_tool$SOURCE_TOOLTIP_BORDER_DARK);
		graphics.fill(x + 1, y + 1, right - 1, y + 2, amagari_translation_tool$SOURCE_TOOLTIP_BORDER_LIGHT);
		graphics.fill(x + 1, y + 1, x + 2, bottom - 1, amagari_translation_tool$SOURCE_TOOLTIP_BORDER_LIGHT);
	}

	@Unique
	private static int amagari_translation_tool$clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
