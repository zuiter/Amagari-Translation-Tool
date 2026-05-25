package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.bilingual.BilingualBookText;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BookViewScreen.class)
public class BookViewScreenMixin {
	@ModifyVariable(method = "render", at = @At(value = "STORE"), ordinal = 0)
	private FormattedText amagari_translation_tool$addSourceHoverToBookText(FormattedText pageText) {
		if (pageText instanceof net.minecraft.network.chat.Component component) {
			return BilingualBookText.withSourceHover(component);
		}
		return pageText;
	}
}
