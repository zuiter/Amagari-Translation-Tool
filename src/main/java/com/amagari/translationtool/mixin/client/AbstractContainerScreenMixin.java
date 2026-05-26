package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.bilingual.BilingualItemTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {
	@Shadow
	protected Slot hoveredSlot;

	@Shadow
	@Final
	protected T menu;

	@Shadow
	protected abstract boolean showTooltipWithItemInHand(ItemStack itemStack);

	@Inject(method = "extractTooltip(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V", at = @At("TAIL"))
	private void amagari_translation_tool$renderContainerSourceItemTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo callbackInfo) {
		if (hoveredSlot == null || !hoveredSlot.hasItem()) {
			return;
		}

		ItemStack stack = hoveredSlot.getItem();
		if (!menu.getCarried().isEmpty() && !showTooltipWithItemInHand(stack)) {
			return;
		}

		BilingualItemTooltip.render(graphics, Minecraft.getInstance().font, stack, mouseX, mouseY);
	}
}
