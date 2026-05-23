package com.amagari.translationtool.client.paratranz;

import java.time.Instant;

public record ParaTranzArtifact(
		int id,
		int projectId,
		Instant createdAt,
		int total,
		int translated,
		int size
) {
}
