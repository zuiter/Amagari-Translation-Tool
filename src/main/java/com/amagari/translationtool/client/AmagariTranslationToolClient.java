package com.amagari.translationtool.client;

import com.amagari.translationtool.client.paratranz.ParaTranzClientCommands;
import com.amagari.translationtool.client.paratranz.ParaTranzContext;
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
		ParaTranzClientCommands.register();
		ClientPlayNetworking.registerGlobalReceiver(WorldLanguageManifestPayload.TYPE, (client, handler, buffer, responseSender) -> {
			WorldLanguageManifestPayload payload = WorldLanguageManifestPayload.read(buffer);
			client.execute(() -> {
				Map<String, String> missingLanguages = WorldLanguageContext.receiveRemoteManifest(client, payload.languages());
				if (!missingLanguages.isEmpty()) {
					List<WorldLanguageRequestPayload.LanguageRequest> requests = missingLanguages.entrySet()
							.stream()
							.map(entry -> new WorldLanguageRequestPayload.LanguageRequest(entry.getKey(), entry.getValue()))
							.toList();
					WorldLanguageRequestPayload requestPayload = new WorldLanguageRequestPayload(requests);
					ClientPlayNetworking.send(WorldLanguageRequestPayload.TYPE, requestPayload.toBuffer());
				}
				WorldLanguageClient.reloadLanguage(client);
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(WorldLanguageDataPayload.TYPE, (client, handler, buffer, responseSender) -> {
			WorldLanguageDataPayload payload = WorldLanguageDataPayload.read(buffer);
			client.execute(() -> {
				WorldLanguageContext.receiveRemoteLanguageData(client, payload);
				WorldLanguageClient.reloadLanguage(client);
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(WorldLanguageCommandPayload.TYPE, (client, handler, buffer, responseSender) -> {
			WorldLanguageCommandPayload payload = WorldLanguageCommandPayload.read(buffer);
			client.execute(() -> handleServerCommand(payload, client));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			WorldLanguageContext.leaveWorld();
			ParaTranzContext.resetSessionState();
		});
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
			client.player.displayClientMessage(Component.literal(ParaTranzContext.lastReport().describe(client.getLanguageManager().getSelected())), false);
		}
	}
}
