package com.amagari.translationtool.network;

import com.amagari.translationtool.AmagariTranslationTool;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ParaTranzCommandPayload(Action action, String argument) {
	public static final ResourceLocation TYPE = new ResourceLocation(AmagariTranslationTool.MOD_ID, "paratranz_command");

	public ParaTranzCommandPayload(Action action) {
		this(action, "");
	}

	public ParaTranzCommandPayload {
		argument = argument == null ? "" : argument;
	}

	public FriendlyByteBuf toBuffer() {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		write(buffer);
		return buffer;
	}

	private void write(FriendlyByteBuf buffer) {
		buffer.writeEnum(action);
		buffer.writeUtf(argument);
	}

	public static ParaTranzCommandPayload read(FriendlyByteBuf buffer) {
		return new ParaTranzCommandPayload(buffer.readEnum(Action.class), buffer.readUtf());
	}

	public enum Action {
		PROJECTS,
		CONFIG,
		PULL,
		HELP
	}
}
