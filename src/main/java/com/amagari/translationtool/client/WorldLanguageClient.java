package com.amagari.translationtool.client;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.client.bilingual.BilingualLanguageController;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.atomic.AtomicLong;

public final class WorldLanguageClient {
	private static final AtomicLong LANGUAGE_RELOAD_VERSION = new AtomicLong();

	private WorldLanguageClient() {
	}

	public static long languageReloadVersion() {
		return LANGUAGE_RELOAD_VERSION.get();
	}

	public static void markLanguageReloaded() {
		LANGUAGE_RELOAD_VERSION.incrementAndGet();
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
