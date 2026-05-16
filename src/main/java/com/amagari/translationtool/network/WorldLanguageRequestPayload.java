package com.amagari.translationtool.network;

import com.amagari.translationtool.AmagariTranslationTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record WorldLanguageRequestPayload(List<LanguageRequest> languages) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WorldLanguageRequestPayload> TYPE = new CustomPacketPayload.Type<>(
			Identifier.fromNamespaceAndPath(AmagariTranslationTool.MOD_ID, "world_language_request")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, WorldLanguageRequestPayload> CODEC = StreamCodec.ofMember(
			WorldLanguageRequestPayload::write,
			WorldLanguageRequestPayload::read
	);

	private static final int MAX_LANGUAGES = 128;
	private static final int MAX_HASH_LENGTH = 128;

	public WorldLanguageRequestPayload {
		languages = List.copyOf(languages);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(languages.size());
		for (LanguageRequest language : languages) {
			buffer.writeUtf(language.languageCode());
			buffer.writeUtf(language.hash());
		}
	}

	private static WorldLanguageRequestPayload read(RegistryFriendlyByteBuf buffer) {
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
