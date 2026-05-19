package com.amagari.translationtool.network;

import com.amagari.translationtool.AmagariTranslationTool;
import com.amagari.translationtool.translation.WorldLanguageTransfer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record WorldLanguageDataPayload(
		String languageCode,
		String hash,
		int uncompressedBytes,
		int entries,
		int totalCompressedBytes,
		int chunkIndex,
		int chunkCount,
		byte[] chunkData
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WorldLanguageDataPayload> TYPE = new CustomPacketPayload.Type<>(
			Identifier.fromNamespaceAndPath(AmagariTranslationTool.MOD_ID, "world_language_data")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, WorldLanguageDataPayload> CODEC = StreamCodec.ofMember(
			WorldLanguageDataPayload::write,
			WorldLanguageDataPayload::read
	);

	private static final int MAX_HASH_LENGTH = 128;
	public static final int MAX_TOTAL_COMPRESSED_BYTES = 4 * 1024 * 1024;
	public static final int MAX_CHUNK_BYTES = 512 * 1024;
	private static final int MAX_CHUNKS = (MAX_TOTAL_COMPRESSED_BYTES + MAX_CHUNK_BYTES - 1) / MAX_CHUNK_BYTES;

	public WorldLanguageDataPayload {
		chunkData = Arrays.copyOf(chunkData, chunkData.length);
		validate(uncompressedBytes, entries, totalCompressedBytes, chunkIndex, chunkCount, chunkData.length);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	@Override
	public byte[] chunkData() {
		return Arrays.copyOf(chunkData, chunkData.length);
	}

	public static List<WorldLanguageDataPayload> chunks(WorldLanguageTransfer.PreparedLanguage language) {
		byte[] compressedData = language.compressedData();
		if (compressedData.length > MAX_TOTAL_COMPRESSED_BYTES) {
			throw new IllegalArgumentException("World language compressed data is too large: " + compressedData.length);
		}

		int chunkCount = Math.max(1, (compressedData.length + MAX_CHUNK_BYTES - 1) / MAX_CHUNK_BYTES);
		List<WorldLanguageDataPayload> payloads = new ArrayList<>(chunkCount);
		for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
			int start = chunkIndex * MAX_CHUNK_BYTES;
			int end = Math.min(compressedData.length, start + MAX_CHUNK_BYTES);
			payloads.add(new WorldLanguageDataPayload(
					language.languageCode(),
					language.hash(),
					language.uncompressedBytes(),
					language.entries(),
					compressedData.length,
					chunkIndex,
					chunkCount,
					Arrays.copyOfRange(compressedData, start, end)
			));
		}
		return payloads;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeUtf(languageCode);
		buffer.writeUtf(hash);
		buffer.writeVarInt(uncompressedBytes);
		buffer.writeVarInt(entries);
		buffer.writeVarInt(totalCompressedBytes);
		buffer.writeVarInt(chunkIndex);
		buffer.writeVarInt(chunkCount);
		buffer.writeByteArray(chunkData);
	}

	private static WorldLanguageDataPayload read(RegistryFriendlyByteBuf buffer) {
		return new WorldLanguageDataPayload(
				buffer.readUtf(),
				buffer.readUtf(MAX_HASH_LENGTH),
				buffer.readVarInt(),
				buffer.readVarInt(),
				buffer.readVarInt(),
				buffer.readVarInt(),
				buffer.readVarInt(),
				buffer.readByteArray(MAX_CHUNK_BYTES)
		);
	}

	private static void validate(int uncompressedBytes, int entries, int totalCompressedBytes, int chunkIndex, int chunkCount, int chunkBytes) {
		if (uncompressedBytes < 0 || uncompressedBytes > WorldLanguageTransfer.MAX_UNCOMPRESSED_BYTES) {
			throw new IllegalArgumentException("Invalid world language uncompressed size: " + uncompressedBytes);
		}
		if (entries < 0 || entries > WorldLanguageTransfer.MAX_ENTRIES_PER_LANGUAGE) {
			throw new IllegalArgumentException("Invalid world language entry count: " + entries);
		}
		if (totalCompressedBytes < 0 || totalCompressedBytes > MAX_TOTAL_COMPRESSED_BYTES) {
			throw new IllegalArgumentException("Invalid world language compressed size: " + totalCompressedBytes);
		}
		if (chunkCount < 1 || chunkCount > MAX_CHUNKS) {
			throw new IllegalArgumentException("Invalid world language chunk count: " + chunkCount);
		}
		if (chunkIndex < 0 || chunkIndex >= chunkCount) {
			throw new IllegalArgumentException("Invalid world language chunk index: " + chunkIndex);
		}

		int chunkStart = chunkIndex * MAX_CHUNK_BYTES;
		int expectedChunkBytes = Math.min(MAX_CHUNK_BYTES, totalCompressedBytes - chunkStart);
		if (expectedChunkBytes < 0 || chunkBytes != expectedChunkBytes) {
			throw new IllegalArgumentException("Invalid world language chunk size: " + chunkBytes);
		}
	}
}
