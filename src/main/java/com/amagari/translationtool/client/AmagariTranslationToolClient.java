package com.amagari.translationtool.client;

import com.amagari.translationtool.client.bilingual.BilingualLanguageController;
import com.amagari.translationtool.client.paratranz.ParaTranzClientCommands;
import com.amagari.translationtool.client.paratranz.ParaTranzContext;
import com.amagari.translationtool.network.ParaTranzCommandPayload;
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
		BilingualLanguageController.register();
		ParaTranzClientCommands.register();
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
		ClientPlayNetworking.registerGlobalReceiver(WorldLanguageCommandPayload.TYPE, (payload, context) -> context.client().execute(() -> handleServerCommand(payload, context.client())));
		ClientPlayNetworking.registerGlobalReceiver(ParaTranzCommandPayload.TYPE, (payload, context) -> context.client().execute(() -> handleParaTranzCommand(payload, context.client())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			WorldLanguageContext.leaveWorld();
			BilingualLanguageController.resetSessionState(client);
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

	private static void handleParaTranzCommand(ParaTranzCommandPayload payload, Minecraft client) {
		if (payload.action() == ParaTranzCommandPayload.Action.HELP) {
			ParaTranzClientCommands.showHelp(client);
			return;
		}
		if (payload.action() == ParaTranzCommandPayload.Action.PROJECTS) {
			ParaTranzClientCommands.listProjects(client);
			return;
		}
		if (payload.action() == ParaTranzCommandPayload.Action.CONFIG) {
			ParaTranzClientCommands.openConfig(client);
			return;
		}
		if (payload.action() == ParaTranzCommandPayload.Action.PULL) {
			ParaTranzClientCommands.pullProject(client, payload.argument());
		}
	}

	private static void sendCommandFeedback(Minecraft client) {
		if (client.player != null) {
			client.player.sendSystemMessage(Component.literal(WorldLanguageContext.describeLastReport(client.getLanguageManager().getSelected())));
			client.player.sendSystemMessage(Component.literal(ParaTranzContext.lastReport().describe(client.getLanguageManager().getSelected())));
		}
	}
}
