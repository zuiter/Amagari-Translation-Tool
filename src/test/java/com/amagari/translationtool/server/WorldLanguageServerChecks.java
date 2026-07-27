package com.amagari.translationtool.server;

public final class WorldLanguageServerChecks {
	private WorldLanguageServerChecks() {
	}

	public static void run() {
		check(WorldLanguageServer.canPublish(true, false), "expected administrators to publish manifests");
		check(WorldLanguageServer.canPublish(false, true), "expected the local singleplayer owner to publish without cheats");
		check(!WorldLanguageServer.canPublish(false, false), "expected non-owner non-administrators to remain denied");
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
