package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.WorldLanguageContext;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.Map;

@Mixin(ClientLanguage.class)
public class ClientLanguageMixin {
	@ModifyVariable(
			method = "loadFrom",
			at = @At(value = "INVOKE", target = "Ljava/util/Map;copyOf(Ljava/util/Map;)Ljava/util/Map;", shift = At.Shift.BEFORE),
			ordinal = 0
	)
	private static Map<String, String> amagari_translation_tool$loadWorldLanguageFiles(
			Map<String, String> translations,
			net.minecraft.server.packs.resources.ResourceManager resourceManager,
			List<String> languageCodes,
			boolean defaultRightToLeft
	) {
		WorldLanguageContext.mergeTranslations(languageCodes, translations);
		return translations;
	}
}
