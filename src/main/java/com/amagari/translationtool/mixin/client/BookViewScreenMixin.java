package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.bilingual.BilingualBookText;
import com.amagari.translationtool.client.bilingual.BilingualLanguageController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookViewScreen.class)
public class BookViewScreenMixin {
	@Shadow
	private int cachedPage;

	@Unique
	private int amagari_translation_tool$lastSourceHoverVersion = -1;

	@Unique
	private boolean amagari_translation_tool$lastSourceDisplayActive;

	@Inject(method = "render", at = @At("HEAD"))
	private void amagari_translation_tool$refreshSourceHoverCache(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
		int version = BilingualBookText.cacheVersion();
		boolean active = BilingualLanguageController.isSourceDisplayActive();
		if (version != amagari_translation_tool$lastSourceHoverVersion || active != amagari_translation_tool$lastSourceDisplayActive) {
			cachedPage = -1;
			amagari_translation_tool$lastSourceHoverVersion = version;
			amagari_translation_tool$lastSourceDisplayActive = active;
		}
	}

	@ModifyArg(
			method = "render",
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
}
