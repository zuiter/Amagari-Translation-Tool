package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.bilingual.BilingualItemTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("TAIL"))
	private void amagari_translation_tool$renderSourceItemTooltip(Font font, ItemStack stack, int mouseX, int mouseY, CallbackInfo callbackInfo) {
		BilingualItemTooltip.render((GuiGraphics) (Object) this, font, stack, mouseX, mouseY);
	}
}
