package com.amagari.translationtool.network;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.translation.WorldLanguageTransfer;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public record WorldLanguageManifestPayload(Map<String, LanguageManifestEntry> languages) {
	public static final ResourceLocation TYPE = new ResourceLocation(AmagariTranslationTool.MOD_ID, "world_language_manifest");

	private static final int MAX_LANGUAGES = 128;
	private static final int MAX_HASH_LENGTH = 128;

	public WorldLanguageManifestPayload {
		languages = Map.copyOf(languages);
	}

	public FriendlyByteBuf toBuffer() {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		write(buffer);
		return buffer;
	}

	private void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(languages.size());
		for (Map.Entry<String, LanguageManifestEntry> language : languages.entrySet()) {
			buffer.writeUtf(language.getKey());
			language.getValue().write(buffer);
		}
	}

	public static WorldLanguageManifestPayload read(FriendlyByteBuf buffer) {
		int languageCount = buffer.readVarInt();
		validateCount(languageCount, MAX_LANGUAGES, "languages");

		Map<String, LanguageManifestEntry> languages = new LinkedHashMap<>();
		for (int languageIndex = 0; languageIndex < languageCount; languageIndex++) {
			languages.put(buffer.readUtf(), LanguageManifestEntry.read(buffer));
		}
		return new WorldLanguageManifestPayload(languages);
	}

	private static void validateCount(int count, int maxCount, String kind) {
		if (count < 0 || count > maxCount) {
			throw new IllegalArgumentException("Invalid world language " + kind + " count: " + count);
		}
	}

	public record LanguageManifestEntry(
			String hash,
			int uncompressedBytes,
			int compressedBytes,
			int entries
	) {
		private void write(FriendlyByteBuf buffer) {
			buffer.writeUtf(hash);
			buffer.writeVarInt(uncompressedBytes);
			buffer.writeVarInt(compressedBytes);
			buffer.writeVarInt(entries);
		}

		private static LanguageManifestEntry read(FriendlyByteBuf buffer) {
			LanguageManifestEntry entry = new LanguageManifestEntry(
					buffer.readUtf(MAX_HASH_LENGTH),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt()
			);
			if (entry.uncompressedBytes() < 0 || entry.uncompressedBytes() > WorldLanguageTransfer.MAX_UNCOMPRESSED_BYTES) {
				throw new IllegalArgumentException("Invalid world language uncompressed size: " + entry.uncompressedBytes());
			}
			if (entry.compressedBytes() < 0) {
				throw new IllegalArgumentException("Invalid world language compressed size: " + entry.compressedBytes());
			}
			if (entry.entries() < 0 || entry.entries() > WorldLanguageTransfer.MAX_ENTRIES_PER_LANGUAGE) {
				throw new IllegalArgumentException("Invalid world language entry count: " + entry.entries());
			}
			return entry;
		}
	}
}
