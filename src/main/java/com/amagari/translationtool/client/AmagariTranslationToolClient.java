package com.amagari.translationtool.client;

import com.amagari.translationtool.network.WorldLanguageDataPayload;
import com.amagari.translationtool.network.WorldLanguageManifestPayload;
import com.amagari.translationtool.network.WorldLanguageRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
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
			WorldLanguageContext.receiveRemoteLanguageData(context.client(), payload);
			WorldLanguageClient.reloadLanguage(context.client());
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WorldLanguageContext.leaveWorld());
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommands.literal("amagari_lang")
						.then(ClientCommands.literal("reload").executes(context -> {
							Minecraft client = context.getSource().getClient();
							WorldLanguageClient.reloadLanguage(client);
							context.getSource().sendFeedback(Component.literal(WorldLanguageContext.describeLastReport()));
							return 1;
						}))
						.then(ClientCommands.literal("status").executes(context -> {
							context.getSource().sendFeedback(Component.literal(WorldLanguageContext.describeLastReport()));
							return 1;
						}))
						.then(ClientCommands.literal("push").executes(context -> {
							Minecraft client = context.getSource().getClient();
							if (client.getConnection() == null) {
								context.getSource().sendError(Component.literal("No remote server connection is active."));
								return 0;
							}
							client.getConnection().sendCommand("amagari_lang push");
							return 1;
						}))
						.then(ClientCommands.literal("pull").executes(context -> {
							Minecraft client = context.getSource().getClient();
							if (client.getConnection() == null) {
								context.getSource().sendError(Component.literal("No remote server connection is active."));
								return 0;
							}
							client.getConnection().sendCommand("amagari_lang pull");
							return 1;
						}))
		));
	}
}
