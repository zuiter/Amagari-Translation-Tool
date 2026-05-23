package com.amagari.translationtool.network;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.translation.WorldLanguageTransfer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

public record WorldLanguageDataPayload(
		String languageCode,
		String hash,
		int uncompressedBytes,
		int entries,
		byte[] compressedData
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WorldLanguageDataPayload> TYPE = new CustomPacketPayload.Type<>(
			new ResourceLocation(AmagariTranslationTool.MOD_ID, "world_language_data")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, WorldLanguageDataPayload> CODEC = StreamCodec.ofMember(
			WorldLanguageDataPayload::write,
			WorldLanguageDataPayload::read
	);

	private static final int MAX_HASH_LENGTH = 128;
	private static final int MAX_COMPRESSED_BYTES = 4 * 1024 * 1024;

	public WorldLanguageDataPayload {
		compressedData = Arrays.copyOf(compressedData, compressedData.length);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	@Override
	public byte[] compressedData() {
		return Arrays.copyOf(compressedData, compressedData.length);
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeUtf(languageCode);
		buffer.writeUtf(hash);
		buffer.writeVarInt(uncompressedBytes);
		buffer.writeVarInt(entries);
		buffer.writeByteArray(compressedData);
	}

	private static WorldLanguageDataPayload read(RegistryFriendlyByteBuf buffer) {
		WorldLanguageDataPayload payload = new WorldLanguageDataPayload(
				buffer.readUtf(),
				buffer.readUtf(MAX_HASH_LENGTH),
				buffer.readVarInt(),
				buffer.readVarInt(),
				buffer.readByteArray(MAX_COMPRESSED_BYTES)
		);
		if (payload.uncompressedBytes() < 0 || payload.uncompressedBytes() > WorldLanguageTransfer.MAX_UNCOMPRESSED_BYTES) {
			throw new IllegalArgumentException("Invalid world language uncompressed size: " + payload.uncompressedBytes());
		}
		if (payload.entries() < 0 || payload.entries() > WorldLanguageTransfer.MAX_ENTRIES_PER_LANGUAGE) {
			throw new IllegalArgumentException("Invalid world language entry count: " + payload.entries());
		}
		return payload;
	}
}
