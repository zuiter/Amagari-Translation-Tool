package com.amagari.translationtool.network;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.translation.WorldLanguageTransfer;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

public record WorldLanguageDataPayload(
		String languageCode,
		String hash,
		int uncompressedBytes,
		int entries,
		byte[] compressedData
) {
	public static final ResourceLocation TYPE = new ResourceLocation(AmagariTranslationTool.MOD_ID, "world_language_data");
	private static final int MAX_HASH_LENGTH = 128;
	private static final int MAX_COMPRESSED_BYTES = 4 * 1024 * 1024;

	public WorldLanguageDataPayload {
		compressedData = Arrays.copyOf(compressedData, compressedData.length);
	}

	public FriendlyByteBuf toBuffer() {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		write(buffer);
		return buffer;
	}

	@Override
	public byte[] compressedData() {
		return Arrays.copyOf(compressedData, compressedData.length);
	}

	private void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(languageCode);
		buffer.writeUtf(hash);
		buffer.writeVarInt(uncompressedBytes);
		buffer.writeVarInt(entries);
		buffer.writeByteArray(compressedData);
	}

	public static WorldLanguageDataPayload read(FriendlyByteBuf buffer) {
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
