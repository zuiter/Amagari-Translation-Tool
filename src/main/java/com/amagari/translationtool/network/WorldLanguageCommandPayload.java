package com.amagari.translationtool.network;

import com.amagari.translationtool.AmagariTranslationTool;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record WorldLanguageCommandPayload(Action action) {
	public static final ResourceLocation TYPE = new ResourceLocation(AmagariTranslationTool.MOD_ID, "world_language_command");

	public FriendlyByteBuf toBuffer() {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		write(buffer);
		return buffer;
	}

	private void write(FriendlyByteBuf buffer) {
		buffer.writeEnum(action);
	}

	public static WorldLanguageCommandPayload read(FriendlyByteBuf buffer) {
		return new WorldLanguageCommandPayload(buffer.readEnum(Action.class));
	}

	public enum Action {
		RELOAD,
		STATUS
	}
}
