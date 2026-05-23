package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.paratranz.ParaTranzSignText;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public class AbstractSignRendererMixin {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
			at = @At("TAIL")
	)
	private void amagari_translation_tool$translateLiteralSignText(
			SignBlockEntity signBlockEntity,
			SignRenderState renderState,
			float tickProgress,
			Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
			CallbackInfo callbackInfo
	) {
		renderState.frontText = ParaTranzSignText.translate(renderState.frontText);
		renderState.backText = ParaTranzSignText.translate(renderState.backText);
	}
}
