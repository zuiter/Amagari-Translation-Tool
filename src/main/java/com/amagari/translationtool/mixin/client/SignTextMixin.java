package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.WorldLanguageClient;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(SignText.class)
public abstract class SignTextMixin {
	@Shadow
	private FormattedCharSequence[] renderMessages;

	@Unique
	private long amagari_translation_tool$languageReloadVersion = Long.MIN_VALUE;

	@Inject(method = "getRenderMessages", at = @At("HEAD"))
	private void amagari_translation_tool$invalidateLanguageCache(
			boolean filtered,
			Function<Component, FormattedCharSequence> messageRenderer,
			CallbackInfoReturnable<FormattedCharSequence[]> callbackInfo
	) {
		long currentVersion = WorldLanguageClient.languageReloadVersion();
		if (amagari_translation_tool$languageReloadVersion != currentVersion) {
			renderMessages = null;
			amagari_translation_tool$languageReloadVersion = currentVersion;
		}
	}
}
