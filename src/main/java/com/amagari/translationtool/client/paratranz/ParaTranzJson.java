package com.amagari.translationtool.client.paratranz;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class ParaTranzJson {
	private ParaTranzJson() {
	}

	public static int parseUserId(String json) {
		JsonObject user = JsonParser.parseString(json).getAsJsonObject();
		return intValue(user, "id", 0);
	}

	public static List<ParaTranzProject> parseProjects(String json) {
		List<ParaTranzProject> projects = new ArrayList<>();
		for (JsonElement element : arrayFrom(JsonParser.parseString(json))) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonObject membership = element.getAsJsonObject();
			JsonObject project = objectValue(membership, "project");
			if (project == null) {
				project = membership;
			}

			int id = intValue(project, "id", 0);
			String name = stringValue(project, "name", "");
			if (id <= 0 || name.isBlank()) {
				continue;
			}

			int permission = intValue(membership, "permission", intValue(project, "permission", 0));
			int privacy = intValue(project, "privacy", 0);
			String game = stringValue(project, "game", "");
			projects.add(new ParaTranzProject(id, name, permission, privacy, game));
		}
		return List.copyOf(projects);
	}

	public static List<ParaTranzArtifact> parseArtifacts(String json) {
		List<ParaTranzArtifact> artifacts = new ArrayList<>();
		for (JsonElement element : artifactElements(JsonParser.parseString(json))) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonObject artifact = element.getAsJsonObject();
			int id = intValue(artifact, "id", 0);
			if (id <= 0) {
				continue;
			}

			artifacts.add(new ParaTranzArtifact(
					id,
					projectId(artifact),
					instantValue(artifact, "createdAt"),
					intValue(artifact, "total", 0),
					intValue(artifact, "translated", 0),
					intValue(artifact, "size", 0)
			));
		}
		return List.copyOf(artifacts);
	}

	private static List<JsonElement> artifactElements(JsonElement root) {
		if (root == null || root.isJsonNull()) {
			return List.of();
		}
		if (root.isJsonObject() && root.getAsJsonObject().has("id")) {
			return List.of(root);
		}
		return arrayFrom(root).asList();
	}

	private static JsonArray arrayFrom(JsonElement root) {
		if (root == null || root.isJsonNull()) {
			return new JsonArray();
		}
		if (root.isJsonArray()) {
			return root.getAsJsonArray();
		}
		if (!root.isJsonObject()) {
			return new JsonArray();
		}

		JsonObject object = root.getAsJsonObject();
		for (String key : List.of("data", "results", "items")) {
			JsonElement value = object.get(key);
			if (value != null && value.isJsonArray()) {
				return value.getAsJsonArray();
			}
		}
		return new JsonArray();
	}

	private static int projectId(JsonObject artifact) {
		JsonElement project = artifact.get("project");
		if (project != null && project.isJsonPrimitive() && project.getAsJsonPrimitive().isNumber()) {
			return project.getAsInt();
		}
		if (project != null && project.isJsonObject()) {
			return intValue(project.getAsJsonObject(), "id", 0);
		}
		return intValue(artifact, "projectId", 0);
	}

	private static JsonObject objectValue(JsonObject object, String key) {
		JsonElement value = object.get(key);
		return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
	}

	private static int intValue(JsonObject object, String key, int fallback) {
		JsonElement value = object.get(key);
		if (value == null || !value.isJsonPrimitive()) {
			return fallback;
		}
		try {
			return value.getAsInt();
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private static String stringValue(JsonObject object, String key, String fallback) {
		JsonElement value = object.get(key);
		if (value == null || !value.isJsonPrimitive()) {
			return fallback;
		}
		try {
			return value.getAsString();
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private static Instant instantValue(JsonObject object, String key) {
		String value = stringValue(object, key, "");
		if (value.isBlank()) {
			return Instant.EPOCH;
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException exception) {
			return Instant.EPOCH;
		}
	}
}
