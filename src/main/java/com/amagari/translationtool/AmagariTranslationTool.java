package com.amagari.translationtool;

import com.amagari.translationtool.network.WorldLanguageDataPayload;
import com.amagari.translationtool.network.WorldLanguageCommandPayload;
import com.amagari.translationtool.network.WorldLanguageManifestPayload;
import com.amagari.translationtool.network.WorldLanguageRequestPayload;
import com.amagari.translationtool.server.WorldLanguageServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmagariTranslationTool implements ModInitializer {
	public static final String MOD_ID = "amagari_translation_tool";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(WorldLanguageManifestPayload.TYPE, WorldLanguageManifestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(WorldLanguageCommandPayload.TYPE, WorldLanguageCommandPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(WorldLanguageRequestPayload.TYPE, WorldLanguageRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(WorldLanguageDataPayload.TYPE, WorldLanguageDataPayload.CODEC);
		WorldLanguageServer.register();
		LOGGER.info("Amagari Translation Tool initialized");
	}
}
