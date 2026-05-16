package com.amagari.translationtool.network;

import com.amagari.translationtool.AmagariTranslationTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WorldLanguageCommandPayload(Action action) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WorldLanguageCommandPayload> TYPE = new CustomPacketPayload.Type<>(
			Identifier.fromNamespaceAndPath(AmagariTranslationTool.MOD_ID, "world_language_command")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, WorldLanguageCommandPayload> CODEC = StreamCodec.ofMember(
			WorldLanguageCommandPayload::write,
			WorldLanguageCommandPayload::read
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeEnum(action);
	}

	private static WorldLanguageCommandPayload read(RegistryFriendlyByteBuf buffer) {
		return new WorldLanguageCommandPayload(buffer.readEnum(Action.class));
	}

	public enum Action {
		RELOAD,
		STATUS
	}
}
