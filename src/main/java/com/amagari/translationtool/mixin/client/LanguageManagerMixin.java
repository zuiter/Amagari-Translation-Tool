package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.WorldLanguageClient;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LanguageManager.class)
public abstract class LanguageManagerMixin {
	@Inject(method = "onResourceManagerReload", at = @At("TAIL"))
	private void amagari_translation_tool$markLanguageReloaded(ResourceManager resourceManager, CallbackInfo callbackInfo) {
		WorldLanguageClient.markLanguageReloaded();
	}
}
