package com.amagari.translationtool.client.paratranz;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ParaTranzApiClient {
	private static final URI API_BASE = URI.create("https://paratranz.cn/api");
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
	private static final Duration EXPORT_TIMEOUT = Duration.ofSeconds(20);
	private static final Duration POLL_DELAY = Duration.ofMillis(1500);

	private final HttpClient httpClient;

	public ParaTranzApiClient() {
		this(HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(15))
				.build());
	}

	ParaTranzApiClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public List<ParaTranzProject> listProjects(String token) throws IOException, InterruptedException {
		int userId = ParaTranzJson.parseUserId(get("/users/my", token));
		if (userId <= 0) {
			throw new ParaTranzApiException(0, "token owner was not returned by ParaTranz");
		}
		return ParaTranzJson.parseProjects(get("/users/" + userId + "/projects", token));
	}

	public DownloadedArtifact exportAndDownload(ParaTranzProject project, String token) throws IOException, InterruptedException {
		Optional<ParaTranzArtifact> baseline = latestArtifact(project.id(), token);
		boolean exportRequested = false;
		try {
			post("/projects/" + project.id() + "/artifacts", token);
			exportRequested = true;
		} catch (ParaTranzApiException exception) {
			if (!exception.permissionDenied()) {
				throw exception;
			}
		}

		ParaTranzArtifact artifact = pollLatestArtifact(project.id(), token, baseline.orElse(null), exportRequested)
				.or(() -> baseline)
				.orElseThrow(() -> new ParaTranzApiException(404, "no ParaTranz artifact is available"));
		byte[] zipData = getBytes("/projects/" + project.id() + "/artifacts/download", token);
		return new DownloadedArtifact(artifact, zipData);
	}

	private Optional<ParaTranzArtifact> pollLatestArtifact(int projectId, String token, ParaTranzArtifact baseline, boolean waitForNewArtifact) throws IOException, InterruptedException {
		Instant deadline = Instant.now().plus(waitForNewArtifact ? EXPORT_TIMEOUT : Duration.ZERO);
		Optional<ParaTranzArtifact> latest = latestArtifact(projectId, token);
		if (!waitForNewArtifact || latest.filter(artifact -> isNewer(artifact, baseline)).isPresent()) {
			return latest;
		}

		while (Instant.now().isBefore(deadline)) {
			Thread.sleep(POLL_DELAY.toMillis());
			latest = latestArtifact(projectId, token);
			if (latest.filter(artifact -> isNewer(artifact, baseline)).isPresent()) {
				return latest;
			}
		}
		return latest;
	}

	private Optional<ParaTranzArtifact> latestArtifact(int projectId, String token) throws IOException, InterruptedException {
		return ParaTranzJson.parseArtifacts(get("/projects/" + projectId + "/artifacts", token))
				.stream()
				.max(Comparator.comparing(ParaTranzArtifact::createdAt).thenComparingInt(ParaTranzArtifact::id));
	}

	private boolean isNewer(ParaTranzArtifact artifact, ParaTranzArtifact baseline) {
		if (baseline == null) {
			return true;
		}
		if (artifact.createdAt().isAfter(baseline.createdAt())) {
			return true;
		}
		return artifact.createdAt().equals(baseline.createdAt()) && artifact.id() > baseline.id();
	}

	private String get(String path, String token) throws IOException, InterruptedException {
		HttpRequest request = request(path, token).GET().build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		ensureSuccess(response.statusCode());
		return response.body();
	}

	private byte[] getBytes(String path, String token) throws IOException, InterruptedException {
		HttpRequest request = request(path, token).GET().build();
		HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
		ensureSuccess(response.statusCode());
		return response.body();
	}

	private String post(String path, String token) throws IOException, InterruptedException {
		HttpRequest request = request(path, token)
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		ensureSuccess(response.statusCode());
		return response.body();
	}

	private HttpRequest.Builder request(String path, String token) {
		return HttpRequest.newBuilder(API_BASE.resolve(API_BASE.getPath() + path))
				.timeout(REQUEST_TIMEOUT)
				.header("Authorization", "Bearer " + token)
				.header("Accept", "application/json");
	}

	private void ensureSuccess(int statusCode) throws ParaTranzApiException {
		if (statusCode >= 200 && statusCode < 300) {
			return;
		}
		if (statusCode == 401) {
			throw new ParaTranzApiException(statusCode, "token rejected");
		}
		if (statusCode == 403) {
			throw new ParaTranzApiException(statusCode, "permission denied");
		}
		throw new ParaTranzApiException(statusCode, "ParaTranz API request failed with HTTP " + statusCode);
	}

	public record DownloadedArtifact(ParaTranzArtifact artifact, byte[] zipData) {
	}
}
