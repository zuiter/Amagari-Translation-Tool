package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.WorldLanguageClient;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Display.TextDisplay.class)
public abstract class TextDisplayMixin {
	@Shadow
	private Display.TextDisplay.CachedInfo clientDisplayCache;

	@Unique
	private long amagari_translation_tool$languageReloadVersion = Long.MIN_VALUE;

	@Inject(method = "cacheDisplay", at = @At("HEAD"))
	private void amagari_translation_tool$invalidateLanguageCache(
			Display.TextDisplay.LineSplitter lineSplitter,
			CallbackInfoReturnable<Display.TextDisplay.CachedInfo> callbackInfo
	) {
		long currentVersion = WorldLanguageClient.languageReloadVersion();
		if (amagari_translation_tool$languageReloadVersion != currentVersion) {
			clientDisplayCache = null;
			amagari_translation_tool$languageReloadVersion = currentVersion;
		}
	}
}
