package com.amagari.translationtool.network;

import com.amagari.translationtool.AmagariTranslationTool;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record WorldLanguageRequestPayload(List<LanguageRequest> languages) {
	public static final ResourceLocation TYPE = new ResourceLocation(AmagariTranslationTool.MOD_ID, "world_language_request");

	private static final int MAX_LANGUAGES = 128;
	private static final int MAX_HASH_LENGTH = 128;

	public WorldLanguageRequestPayload {
		languages = List.copyOf(languages);
	}

	public FriendlyByteBuf toBuffer() {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		write(buffer);
		return buffer;
	}

	private void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(languages.size());
		for (LanguageRequest language : languages) {
			buffer.writeUtf(language.languageCode());
			buffer.writeUtf(language.hash());
		}
	}

	public static WorldLanguageRequestPayload read(FriendlyByteBuf buffer) {
		int languageCount = buffer.readVarInt();
		validateCount(languageCount, MAX_LANGUAGES, "languages");

		List<LanguageRequest> languages = new ArrayList<>(languageCount);
		for (int languageIndex = 0; languageIndex < languageCount; languageIndex++) {
			languages.add(new LanguageRequest(buffer.readUtf(), buffer.readUtf(MAX_HASH_LENGTH)));
		}
		return new WorldLanguageRequestPayload(languages);
	}

	private static void validateCount(int count, int maxCount, String kind) {
		if (count < 0 || count > maxCount) {
			throw new IllegalArgumentException("Invalid world language " + kind + " count: " + count);
		}
	}

	public record LanguageRequest(String languageCode, String hash) {
	}
}
