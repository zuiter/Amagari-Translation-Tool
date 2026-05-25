package com.amagari.translationtool.client.bilingual;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.client.WorldLanguageClient;
import com.amagari.translationtool.client.paratranz.ParaTranzConfig;
import com.amagari.translationtool.client.paratranz.ParaTranzContext;
import com.amagari.translationtool.translation.WorldLanguageMessages;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;

public final class BilingualLanguageController {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(AmagariTranslationTool.MOD_ID, "keybindings"));
	private static final String TOGGLE_SOURCE_LANGUAGE_KEY = "key.amagari_translation_tool.toggle_source_language";
	private static final String TOGGLE_SOURCE_DISPLAY_KEY = "key.amagari_translation_tool.toggle_source_display";
	private static final AtomicBoolean SOURCE_LANGUAGE_ACTIVE = new AtomicBoolean(false);
	private static final AtomicBoolean SOURCE_DISPLAY_ACTIVE = new AtomicBoolean(false);
	private static KeyMapping toggleSourceLanguage;
	private static KeyMapping toggleSourceDisplay;

	private BilingualLanguageController() {
	}

	public static void register() {
		toggleSourceLanguage = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				TOGGLE_SOURCE_LANGUAGE_KEY,
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				CATEGORY
		));
		toggleSourceDisplay = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				TOGGLE_SOURCE_DISPLAY_KEY,
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				CATEGORY
		));
		ClientTickEvents.END_CLIENT_TICK.register(BilingualLanguageController::handleKeybinds);
		BilingualSourceDisplay.register();
	}

	public static void resetSessionState() {
		SOURCE_LANGUAGE_ACTIVE.set(false);
		SOURCE_DISPLAY_ACTIVE.set(false);
		reloadSourceTranslations();
	}

	public static void resetSessionState(Minecraft client) {
		boolean wasSourceLanguageActive = SOURCE_LANGUAGE_ACTIVE.getAndSet(false);
		SOURCE_DISPLAY_ACTIVE.set(false);
		reloadSourceTranslations();
		if (wasSourceLanguageActive && client != null && client.getLanguageManager() != null) {
			String targetLanguage = ParaTranzContext.targetLanguage();
			client.options.languageCode = targetLanguage;
			client.getLanguageManager().setSelected(targetLanguage);
			WorldLanguageClient.reloadLanguage(client);
		}
	}

	public static boolean isSourceLanguageActive() {
		return SOURCE_LANGUAGE_ACTIVE.get();
	}

	public static boolean isSourceDisplayActive() {
		return SOURCE_DISPLAY_ACTIVE.get();
	}

	private static void handleKeybinds(Minecraft client) {
		if (toggleSourceLanguage == null || toggleSourceDisplay == null) {
			return;
		}
		while (toggleSourceLanguage.consumeClick()) {
			toggleSourceLanguage(client);
		}
		while (toggleSourceDisplay.consumeClick()) {
			toggleSourceDisplay(client);
		}
	}

	private static void toggleSourceLanguage(Minecraft client) {
		boolean enabled = !SOURCE_LANGUAGE_ACTIVE.get();
		SOURCE_LANGUAGE_ACTIVE.set(enabled);
		reloadLanguage(client, enabled);
	}

	private static void toggleSourceDisplay(Minecraft client) {
		boolean enabled = !SOURCE_DISPLAY_ACTIVE.get();
		String languageCode = selectedLanguage(client);
		ParaTranzConfig config = currentConfig(client);
		SOURCE_DISPLAY_ACTIVE.set(enabled);
		BilingualBookText.invalidateCache();
		send(client, WorldLanguageMessages.bilingualSourceDisplayToggled(enabled, config.sourceLanguage(), languageCode));
	}

	private static String selectedLanguage(Minecraft client) {
		if (client == null || client.getLanguageManager() == null) {
			return ParaTranzConfig.DEFAULT_TARGET_LANGUAGE;
		}
		return client.getLanguageManager().getSelected();
	}

	private static void reloadLanguage(Minecraft client, boolean sourceLanguageEnabled) {
		ParaTranzConfig config = currentConfig(client);
		String nextLanguage = sourceLanguageEnabled ? config.sourceLanguage() : config.targetLanguage();
		client.options.languageCode = nextLanguage;
		client.getLanguageManager().setSelected(nextLanguage);
		Runnable afterReload = () -> send(client, WorldLanguageMessages.bilingualSourceLanguageToggled(
				sourceLanguageEnabled,
				config.sourceLanguage(),
				config.targetLanguage(),
				selectedLanguage(client)
		));
		WorldLanguageClient.reloadLanguage(client, afterReload);
	}

	private static ParaTranzConfig currentConfig(Minecraft client) {
		if (client == null || client.gameDirectory == null) {
			return ParaTranzConfig.defaultConfig();
		}
		return ParaTranzContext.refreshConfig(client.gameDirectory.toPath());
	}

	public static void reloadSourceTranslations() {
		BilingualSourceTranslations.clearCache();
		BilingualBookText.invalidateCache();
	}

	private static void send(Minecraft client, String message) {
		if (client.player != null) {
			client.player.displayClientMessage(Component.literal(message), false);
		}
	}
}
