package com.amagari.translationtool.client.bilingual;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.client.paratranz.ParaTranzContext;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BilingualSourceDisplay {
	private static final ResourceLocation HUD_LAYER = ResourceLocation.fromNamespaceAndPath(AmagariTranslationTool.MOD_ID, "source_language_hud");
	private static final int HUD_TEXT_WIDTH = 180;
	private static final int HUD_LINE_HEIGHT = 10;
	private static final int HUD_PADDING = 5;

	private BilingualSourceDisplay() {
	}

	public static void register() {
		HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> layeredDrawer.attachLayerAfter(
				IdentifiedLayer.CROSSHAIR,
				HUD_LAYER,
				BilingualSourceDisplay::renderBlockHud
		));
	}

	private static void renderBlockHud(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
		if (!BilingualLanguageController.isSourceDisplayActive()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null || client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
			return;
		}

		BlockPos blockPos = ((BlockHitResult) client.hitResult).getBlockPos();
		List<Component> lines = sourceLinesForBlock(client, blockPos);
		if (lines.isEmpty()) {
			return;
		}

		renderHudBox(graphics, client.font, lines);
	}

	private static List<Component> sourceLinesForBlock(Minecraft client, BlockPos blockPos) {
		List<Component> sourceLines = new ArrayList<>();
		if (client.level.getBlockEntity(blockPos) instanceof SignBlockEntity signBlockEntity) {
			addSignSourceLines(signBlockEntity.getText(signBlockEntity.isFacingFrontText(client.player)), sourceLines);
		}
		return sourceLines.stream().limit(5).toList();
	}

	private static void addSignSourceLines(SignText signText, List<Component> sourceLines) {
		for (int line = 0; line < SignText.LINES; line++) {
			sourceTextForSignLine(signText.getMessage(line, false))
					.ifPresent(text -> sourceLines.add(Component.literal(text).withStyle(ChatFormatting.GRAY)));
		}
	}

	private static Optional<String> sourceTextForSignLine(Component component) {
		String text = component.getString();
		if (text.isBlank()) {
			return Optional.empty();
		}
		return ParaTranzContext.sourceLiteralWorldTextForDisplay(text);
	}

	private static void renderHudBox(GuiGraphics graphics, Font font, List<Component> components) {
		List<FormattedCharSequence> lines = components.stream()
				.flatMap(component -> font.split(component, HUD_TEXT_WIDTH).stream())
				.limit(6)
				.toList();
		if (lines.isEmpty()) {
			return;
		}

		int maxWidth = lines.stream().mapToInt(font::width).max().orElse(0);
		int boxWidth = maxWidth + HUD_PADDING * 2;
		int boxHeight = lines.size() * HUD_LINE_HEIGHT + HUD_PADDING * 2;
		int x = (graphics.guiWidth() - boxWidth) / 2;
		int y = graphics.guiHeight() / 2 + 14;

		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, 300.0F);
		graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xAA111111);
		for (int index = 0; index < lines.size(); index++) {
			graphics.drawString(font, lines.get(index), x + HUD_PADDING, y + HUD_PADDING + index * HUD_LINE_HEIGHT, 0xFFE0E0E0, false);
		}
		graphics.pose().popPose();
	}
}
