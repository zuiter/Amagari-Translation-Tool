package com.amagari.translationtool.client.paratranz;

import com.amagari.translationtool.client.WorldLanguageClient;
import com.amagari.translationtool.client.WorldLanguageContext;
import com.amagari.translationtool.translation.WorldLanguageMessages;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class ParaTranzClientCommands {
	private static int pendingConfigOpenTicks = -1;

	private ParaTranzClientCommands() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ParaTranzClientCommands::openPendingConfigScreen);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommands.literal("amagari_lang")
						.then(ClientCommands.literal("paratranz")
								.executes(context -> showHelp(context.getSource().getClient()))
								.then(ClientCommands.literal("projects")
										.executes(context -> listProjects(context.getSource().getClient())))
								.then(ClientCommands.literal("config")
										.executes(context -> openConfig(context.getSource().getClient())))
								.then(ClientCommands.literal("pull")
										.executes(context -> listProjects(context.getSource().getClient()))
										.then(ClientCommands.argument("projectName", StringArgumentType.greedyString())
												.suggests(ParaTranzClientCommands::suggestProjects)
												.executes(context -> {
													String projectName = StringArgumentType.getString(context, "projectName");
													return pullProject(context.getSource().getClient(), projectName);
												}))))
						.then(ClientCommands.literal("status")
								.executes(context -> {
									String languageCode = context.getSource().getClient().getLanguageManager().getSelected();
									context.getSource().sendFeedback(Component.literal(WorldLanguageContext.describeLastReport(languageCode)));
									context.getSource().sendFeedback(Component.literal(ParaTranzContext.lastReport().describe(languageCode)));
									return 1;
								}))
						.then(ClientCommands.literal("help")
								.executes(context -> {
									String languageCode = context.getSource().getClient().getLanguageManager().getSelected();
									WorldLanguageMessages.help(languageCode)
											.forEach(line -> context.getSource().sendFeedback(Component.literal(line)));
									return 1;
								}))
						.then(ClientCommands.literal("reload")
								.executes(context -> {
									Minecraft client = context.getSource().getClient();
									WorldLanguageClient.reloadLanguage(client, () -> {
										String languageCode = client.getLanguageManager().getSelected();
										context.getSource().sendFeedback(Component.literal(WorldLanguageContext.describeLastReport(languageCode)));
										context.getSource().sendFeedback(Component.literal(ParaTranzContext.lastReport().describe(languageCode)));
									});
									return 1;
								}))
						.then(ClientCommands.literal("pull")
								.executes(context -> forwardToServer(context.getSource().getClient(), "amagari_lang pull")))
						.then(ClientCommands.literal("push")
								.executes(context -> forwardToServer(context.getSource().getClient(), "amagari_lang push")))
		));
	}

	public static int listProjects(Minecraft client) {
		ParaTranzContext.listProjects(client);
		return 1;
	}

	public static int showHelp(Minecraft client) {
		String languageCode = client.getLanguageManager().getSelected();
		WorldLanguageMessages.paraTranzHelp(languageCode)
				.forEach(line -> {
					if (client.player != null) {
						client.player.sendSystemMessage(Component.literal(line));
					}
				});
		return 1;
	}

	public static int openConfig(Minecraft client) {
		pendingConfigOpenTicks = 1;
		return 1;
	}

	public static int pullProject(Minecraft client, String projectName) {
		ParaTranzContext.applyProject(client, projectName);
		return 1;
	}

	private static int forwardToServer(Minecraft client, String command) {
		if (client.getConnection() == null) {
			return 0;
		}
		client.getConnection().sendCommand(command);
		return 1;
	}

	private static void openPendingConfigScreen(Minecraft client) {
		if (pendingConfigOpenTicks < 0) {
			return;
		}
		if (pendingConfigOpenTicks > 0) {
			pendingConfigOpenTicks--;
			return;
		}
		pendingConfigOpenTicks = -1;
		client.setScreen(new ParaTranzConfigScreen(client.screen, client));
	}

	private static CompletableFuture<Suggestions> suggestProjects(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
		Path gameDirectory = context.getSource().getClient().gameDirectory.toPath();
		String input = builder.getRemaining();
		return ParaTranzContext.suggestProjectNames(gameDirectory, input)
				.thenApply(projectNames -> {
					projectNames.forEach(builder::suggest);
					return builder.build();
				});
	}
}
