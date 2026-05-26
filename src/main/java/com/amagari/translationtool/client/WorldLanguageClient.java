package com.amagari.translationtool.client;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.client.bilingual.BilingualLanguageController;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;

public final class WorldLanguageClient {
	private WorldLanguageClient() {
	}

	public static void reloadLanguage(Minecraft client) {
		reloadLanguage(client, () -> {
		});
	}

	public static void reloadLanguage(Minecraft client, Runnable afterReload) {
		ResourceManager resourceManager = client.getResourceManager();
		client.execute(() -> {
			try {
				BilingualLanguageController.reloadSourceTranslations();
				client.getLanguageManager().onResourceManagerReload(resourceManager);
			} catch (RuntimeException exception) {
				AmagariTranslationTool.LOGGER.warn("Failed to reload world language files", exception);
			} finally {
				afterReload.run();
			}
		});
	}
}
