package com.amagari.translationtool.translation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class WorldLanguageTransfer {
	public static final int MAX_ENTRIES_PER_LANGUAGE = 16384;
	public static final int MAX_UNCOMPRESSED_BYTES = 16 * 1024 * 1024;
	private static final int MAX_STRING_BYTES = 32767;

	private WorldLanguageTransfer() {
	}

	public static PreparedLanguage prepare(String languageCode, Map<String, String> translations) throws IOException {
		byte[] uncompressedData = encodeTranslations(translations);
		byte[] compressedData = compress(uncompressedData);
		return new PreparedLanguage(languageCode, sha256Hex(uncompressedData), uncompressedData.length, compressedData, translations.size());
	}

	public static Map<String, String> decodeCompressed(byte[] compressedData, int expectedUncompressedBytes, String expectedHash) throws IOException {
		byte[] uncompressedData = decompress(compressedData, expectedUncompressedBytes);
		String actualHash = sha256Hex(uncompressedData);
		if (!actualHash.equals(expectedHash)) {
			throw new IOException("World language hash mismatch: expected " + expectedHash + ", got " + actualHash);
		}
		return decodeTranslations(uncompressedData);
	}

	private static byte[] encodeTranslations(Map<String, String> translations) throws IOException {
		if (translations.size() > MAX_ENTRIES_PER_LANGUAGE) {
			throw new IOException("Too many world language entries: " + translations.size());
		}

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream output = new DataOutputStream(bytes)) {
			Map<String, String> sortedTranslations = new TreeMap<>(translations);
			output.writeInt(sortedTranslations.size());
			for (Map.Entry<String, String> translation : sortedTranslations.entrySet()) {
				writeUtf8(output, translation.getKey());
				writeUtf8(output, translation.getValue());
			}
		}
		return bytes.toByteArray();
	}

	private static Map<String, String> decodeTranslations(byte[] data) throws IOException {
		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
			int entryCount = input.readInt();
			if (entryCount < 0 || entryCount > MAX_ENTRIES_PER_LANGUAGE) {
				throw new IOException("Invalid world language entry count: " + entryCount);
			}

			Map<String, String> translations = new LinkedHashMap<>();
			for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
				translations.put(readUtf8(input), readUtf8(input));
			}
			return translations;
		}
	}

	private static void writeUtf8(DataOutputStream output, String value) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		if (bytes.length > MAX_STRING_BYTES) {
			throw new IOException("World language string is too long: " + bytes.length);
		}
		output.writeInt(bytes.length);
		output.write(bytes);
	}

	private static String readUtf8(DataInputStream input) throws IOException {
		int length = input.readInt();
		if (length < 0 || length > MAX_STRING_BYTES) {
			throw new IOException("Invalid world language string length: " + length);
		}
		byte[] bytes = input.readNBytes(length);
		if (bytes.length != length) {
			throw new IOException("Unexpected end of world language data");
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static byte[] compress(byte[] data) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
			gzip.write(data);
		}
		return bytes.toByteArray();
	}

	private static byte[] decompress(byte[] compressedData, int expectedUncompressedBytes) throws IOException {
		if (expectedUncompressedBytes < 0 || expectedUncompressedBytes > MAX_UNCOMPRESSED_BYTES) {
			throw new IOException("Invalid world language size: " + expectedUncompressedBytes);
		}

		try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
			byte[] data = gzip.readNBytes(expectedUncompressedBytes + 1);
			if (data.length != expectedUncompressedBytes) {
				throw new IOException("Unexpected world language size: " + data.length);
			}
			return data;
		}
	}

	private static String sha256Hex(byte[] data) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(data);
			StringBuilder text = new StringBuilder(hash.length * 2);
			for (byte value : hash) {
				text.append(String.format("%02x", value));
			}
			return text.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	public record PreparedLanguage(
			String languageCode,
			String hash,
			int uncompressedBytes,
			byte[] compressedData,
			int entries
	) {
	}
}
