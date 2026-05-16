package com.amagari.translationtool.client;

import com.amagari.translationtool.AmagariTranslationTool;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;

public final class WorldLanguageClient {
	private WorldLanguageClient() {
	}

	public static void reloadLanguage(Minecraft client) {
		ResourceManager resourceManager = client.getResourceManager();
		client.execute(() -> {
			try {
				client.getLanguageManager().onResourceManagerReload(resourceManager);
			} catch (RuntimeException exception) {
				AmagariTranslationTool.LOGGER.warn("Failed to reload world language files", exception);
			}
		});
	}
}
