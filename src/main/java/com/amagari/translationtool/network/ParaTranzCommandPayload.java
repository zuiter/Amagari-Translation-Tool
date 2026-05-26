package com.amagari.translationtool.network;

import com.amagari.translationtool.AmagariTranslationTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ParaTranzCommandPayload(Action action, String argument) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ParaTranzCommandPayload> TYPE = new CustomPacketPayload.Type<>(
			Identifier.fromNamespaceAndPath(AmagariTranslationTool.MOD_ID, "paratranz_command")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, ParaTranzCommandPayload> CODEC = StreamCodec.ofMember(
			ParaTranzCommandPayload::write,
			ParaTranzCommandPayload::read
	);

	public ParaTranzCommandPayload(Action action) {
		this(action, "");
	}

	public ParaTranzCommandPayload {
		argument = argument == null ? "" : argument;
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeEnum(action);
		buffer.writeUtf(argument);
	}

	private static ParaTranzCommandPayload read(RegistryFriendlyByteBuf buffer) {
		return new ParaTranzCommandPayload(buffer.readEnum(Action.class), buffer.readUtf());
	}

	public enum Action {
		PROJECTS,
		CONFIG,
		PULL,
		HELP
	}
}
