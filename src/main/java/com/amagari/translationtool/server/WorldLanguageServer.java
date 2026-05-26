package com.amagari.translationtool.server;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.network.ParaTranzCommandPayload;
import com.amagari.translationtool.network.WorldLanguageCommandPayload;
import com.amagari.translationtool.network.WorldLanguageDataPayload;
import com.amagari.translationtool.network.WorldLanguageManifestPayload;
import com.amagari.translationtool.network.WorldLanguageRequestPayload;
import com.amagari.translationtool.translation.WorldLanguageFiles;
import com.amagari.translationtool.translation.WorldLanguageMessages;
import com.amagari.translationtool.translation.WorldLanguageTransfer;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WorldLanguageServer {
	private WorldLanguageServer() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendManifest(handler.player, server));
		ServerPlayNetworking.registerGlobalReceiver(WorldLanguageRequestPayload.TYPE, (payload, context) -> sendRequestedLanguages(context.player(), context.server(), payload));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("amagari_lang")
						.then(Commands.literal("help").executes(context -> {
							WorldLanguageMessages.help(language(context.getSource().getPlayer()))
									.forEach(line -> context.getSource().sendSystemMessage(Component.literal(line)));
							return 1;
						}))
						.then(Commands.literal("reload").executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							if (!ServerPlayNetworking.canSend(player, WorldLanguageCommandPayload.TYPE)) {
								context.getSource().sendFailure(Component.literal(WorldLanguageMessages.unsupportedClient(language(player))));
								return 0;
							}
							ServerPlayNetworking.send(player, new WorldLanguageCommandPayload(WorldLanguageCommandPayload.Action.RELOAD));
							return 1;
						}))
						.then(Commands.literal("status").executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							if (!ServerPlayNetworking.canSend(player, WorldLanguageCommandPayload.TYPE)) {
								context.getSource().sendFailure(Component.literal(WorldLanguageMessages.unsupportedClient(language(player))));
								return 0;
							}
							ServerPlayNetworking.send(player, new WorldLanguageCommandPayload(WorldLanguageCommandPayload.Action.STATUS));
							return 1;
						}))
						.then(Commands.literal("pull").executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							boolean sent = sendManifest(player, context.getSource().getServer());
							context.getSource().sendSuccess(() -> Component.literal(WorldLanguageMessages.requestedManifest(sent, language(player))), false);
							return sent ? 1 : 0;
						}))
						.then(Commands.literal("push").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(context -> {
							int playerCount = sendManifestToAll(context.getSource().getServer());
							context.getSource().sendSuccess(() -> Component.literal(WorldLanguageMessages.publishedManifest(playerCount, language(context.getSource().getPlayer()))), false);
							return playerCount;
						}))
						.then(Commands.literal("paratranz")
								.executes(context -> sendParaTranzCommand(context.getSource().getPlayerOrException(), ParaTranzCommandPayload.Action.HELP))
								.then(Commands.literal("projects")
										.executes(context -> sendParaTranzCommand(context.getSource().getPlayerOrException(), ParaTranzCommandPayload.Action.PROJECTS)))
								.then(Commands.literal("config")
										.executes(context -> sendParaTranzCommand(context.getSource().getPlayerOrException(), ParaTranzCommandPayload.Action.CONFIG)))
								.then(Commands.literal("pull")
										.executes(context -> sendParaTranzCommand(context.getSource().getPlayerOrException(), ParaTranzCommandPayload.Action.PROJECTS))
										.then(Commands.argument("projectName", StringArgumentType.greedyString())
												.executes(context -> sendParaTranzCommand(
														context.getSource().getPlayerOrException(),
														ParaTranzCommandPayload.Action.PULL,
														StringArgumentType.getString(context, "projectName")
												)))))
		));
	}

	private static int sendParaTranzCommand(ServerPlayer player, ParaTranzCommandPayload.Action action) {
		return sendParaTranzCommand(player, action, "");
	}

	private static int sendParaTranzCommand(ServerPlayer player, ParaTranzCommandPayload.Action action, String argument) {
		if (ServerPlayNetworking.canSend(player, ParaTranzCommandPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ParaTranzCommandPayload(action, argument));
			return 1;
		}
		player.sendSystemMessage(Component.literal(WorldLanguageMessages.unsupportedClient(language(player))));
		return 0;
	}

	private static int sendManifestToAll(MinecraftServer server) {
		int playerCount = 0;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (sendManifest(player, server)) {
				playerCount++;
			}
		}
		return playerCount;
	}

	private static boolean sendManifest(ServerPlayer player, MinecraftServer server) {
		if (!canSync(player) || shouldUseLocalWorldFiles(player, server)) {
			return false;
		}

		PreparedWorldLanguages languages = prepareWorldLanguages(server, player);
		if (languages == null) {
			return false;
		}

		ServerPlayNetworking.send(player, new WorldLanguageManifestPayload(languages.manifest()));
		if (!languages.manifest().isEmpty()) {
			AmagariTranslationTool.LOGGER.info("Published {} world language manifest entries to {}", languages.manifest().size(), player.getName().getString());
		}
		return true;
	}

	private static void sendRequestedLanguages(ServerPlayer player, MinecraftServer server, WorldLanguageRequestPayload payload) {
		if (!canSync(player) || shouldUseLocalWorldFiles(player, server)) {
			return;
		}

		PreparedWorldLanguages languages = prepareWorldLanguages(server, player);
		if (languages == null) {
			return;
		}

		int sentLanguages = 0;
		for (WorldLanguageRequestPayload.LanguageRequest request : payload.languages()) {
			WorldLanguageTransfer.PreparedLanguage language = languages.preparedLanguages().get(request.languageCode());
			if (language == null || !language.hash().equals(request.hash())) {
				continue;
			}

			ServerPlayNetworking.send(player, new WorldLanguageDataPayload(
					language.languageCode(),
					language.hash(),
					language.uncompressedBytes(),
					language.entries(),
					language.compressedData()
			));
			sentLanguages++;
		}

		if (sentLanguages > 0) {
			AmagariTranslationTool.LOGGER.info("Sent {} requested world language data payload(s) to {}", sentLanguages, player.getName().getString());
		}
	}

	private static boolean canSync(ServerPlayer player) {
		return ServerPlayNetworking.canSend(player, WorldLanguageManifestPayload.TYPE)
				&& ServerPlayNetworking.canSend(player, WorldLanguageDataPayload.TYPE);
	}

	private static boolean shouldUseLocalWorldFiles(ServerPlayer player, MinecraftServer server) {
		return server.isSingleplayer() && server.isSingleplayerOwner(player.nameAndId());
	}

	private static String language(ServerPlayer player) {
		return player == null ? "en_us" : player.clientInformation().language();
	}

	private static PreparedWorldLanguages prepareWorldLanguages(MinecraftServer server, ServerPlayer player) {
		WorldLanguageFiles.WorldLanguageCollection collection = loadWorldLanguages(server);
		if (collection.failed()) {
			AmagariTranslationTool.LOGGER.warn("Could not load world language files from {}: {}", collection.languageDirectory(), collection.error());
			return null;
		}

		WorldLanguageFiles.WorldLanguageCollection filteredCollection = collection.filterFor(player.clientInformation().language());
		Map<String, WorldLanguageTransfer.PreparedLanguage> preparedLanguages = new LinkedHashMap<>();
		Map<String, WorldLanguageManifestPayload.LanguageManifestEntry> manifest = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, String>> language : filteredCollection.translationsByLanguage().entrySet()) {
			try {
				WorldLanguageTransfer.PreparedLanguage preparedLanguage = WorldLanguageTransfer.prepare(language.getKey(), language.getValue());
				preparedLanguages.put(language.getKey(), preparedLanguage);
				manifest.put(language.getKey(), new WorldLanguageManifestPayload.LanguageManifestEntry(
						preparedLanguage.hash(),
						preparedLanguage.uncompressedBytes(),
						preparedLanguage.compressedData().length,
						preparedLanguage.entries()
				));
			} catch (Exception exception) {
				AmagariTranslationTool.LOGGER.warn("Could not prepare world language {} for {}", language.getKey(), player.getName().getString(), exception);
			}
		}
		return new PreparedWorldLanguages(preparedLanguages, manifest);
	}

	private static WorldLanguageFiles.WorldLanguageCollection loadWorldLanguages(MinecraftServer server) {
		Path worldDirectory = server.getWorldPath(LevelResource.ROOT);
		return WorldLanguageFiles.loadAll(worldDirectory);
	}

	private record PreparedWorldLanguages(
			Map<String, WorldLanguageTransfer.PreparedLanguage> preparedLanguages,
			Map<String, WorldLanguageManifestPayload.LanguageManifestEntry> manifest
	) {
	}
}
