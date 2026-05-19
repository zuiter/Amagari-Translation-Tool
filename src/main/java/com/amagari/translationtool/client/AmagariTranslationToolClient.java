package com.amagari.translationtool.client;

import com.amagari.translationtool.network.WorldLanguageCommandPayload;
import com.amagari.translationtool.network.WorldLanguageDataPayload;
import com.amagari.translationtool.network.WorldLanguageManifestPayload;
import com.amagari.translationtool.network.WorldLanguageRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

public class AmagariTranslationToolClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(WorldLanguageManifestPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			Map<String, String> missingLanguages = WorldLanguageContext.receiveRemoteManifest(context.client(), payload.languages());
			if (!missingLanguages.isEmpty()) {
				List<WorldLanguageRequestPayload.LanguageRequest> requests = missingLanguages.entrySet()
						.stream()
						.map(entry -> new WorldLanguageRequestPayload.LanguageRequest(entry.getKey(), entry.getValue()))
						.toList();
				ClientPlayNetworking.send(new WorldLanguageRequestPayload(requests));
			}
			WorldLanguageClient.reloadLanguage(context.client());
		}));
		ClientPlayNetworking.registerGlobalReceiver(WorldLanguageDataPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			if (WorldLanguageContext.receiveRemoteLanguageData(context.client(), payload)) {
				WorldLanguageClient.reloadLanguage(context.client());
			}
		}));
		ClientPlayNetworking.registerGlobalReceiver(WorldLanguageCommandPayload.TYPE, (payload, context) -> context.client().execute(() -> handleServerCommand(payload, context.client())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WorldLanguageContext.leaveWorld());
	}

	private static void handleServerCommand(WorldLanguageCommandPayload payload, Minecraft client) {
		if (payload.action() == WorldLanguageCommandPayload.Action.RELOAD) {
			WorldLanguageClient.reloadLanguage(client, () -> sendCommandFeedback(client));
			return;
		}
		sendCommandFeedback(client);
	}

	private static void sendCommandFeedback(Minecraft client) {
		if (client.player != null) {
			client.player.displayClientMessage(Component.literal(WorldLanguageContext.describeLastReport(client.getLanguageManager().getSelected())), false);
		}
	}
}
