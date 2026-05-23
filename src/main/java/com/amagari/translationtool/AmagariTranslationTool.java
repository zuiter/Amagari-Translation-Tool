package com.amagari.translationtool;

import com.amagari.translationtool.server.WorldLanguageServer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmagariTranslationTool implements ModInitializer {
	public static final String MOD_ID = "amagari_translation_tool";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		WorldLanguageServer.register();
		LOGGER.info("Amagari Translation Tool initialized");
	}
}
