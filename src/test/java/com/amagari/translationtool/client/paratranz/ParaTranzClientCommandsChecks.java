package com.amagari.translationtool.client.paratranz;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class ParaTranzClientCommandsChecks {
	private ParaTranzClientCommandsChecks() {
	}

	public static void run() {
		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		dispatcher.register(ParaTranzClientCommands.commandTree());

		checkServerFallback(dispatcher, "amagari_lang pull");
		checkServerFallback(dispatcher, "amagari_lang push");
	}

	private static void checkServerFallback(
			CommandDispatcher<FabricClientCommandSource> dispatcher,
			String command
	) {
		try {
			dispatcher.execute(command, null);
			throw new AssertionError("expected server-owned command to fall through: " + command);
		} catch (CommandSyntaxException exception) {
			if (exception.getType() != CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand()) {
				throw new AssertionError("expected Fabric server fallback for " + command, exception);
			}
		}
	}
}
