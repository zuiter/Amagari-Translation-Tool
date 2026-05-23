package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.paratranz.ParaTranzSignText;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SignRenderer.class)
public class AbstractSignRendererMixin {
	@ModifyVariable(method = "renderSignText", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private SignText amagari_translation_tool$translateLiteralSignText(SignText signText) {
		return ParaTranzSignText.translate(signText);
	}
}
