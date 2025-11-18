package com.moodtunes.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ApiClient {
    private final HttpClient client;
    private final String baseUrl;

    public ApiClient() {
        this.baseUrl = Optional.ofNullable(System.getProperty("api.base.url"))
            .orElse(Optional.ofNullable(System.getenv("MOODTUNES_API_URL"))
                .orElse("http://localhost:5000"));

        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            // Redirects are fine for normal API calls; JavaFX Media follows its own redirects separately.
            .followRedirects(Redirect.NORMAL)
            .build();
    }

    public HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(resolve(path)))
            .timeout(Duration.ofSeconds(15)) // per-request timeout (connect+read)
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public HttpResponse<String> post(String path, String json) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(resolve(path)))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String resolve(String path) {
        // If caller passed a full URL, use it as-is
        if (path.startsWith("http://") || path.startsWith("https://")) return path;

        // Normalize slashes to avoid "//"
        if (path.startsWith("/")) {
            return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + path : baseUrl + path;
        } else {
            return baseUrl.endsWith("/") ? baseUrl + path : baseUrl + "/" + path;
        }
    }

    public String getBaseUrl() { return baseUrl; }

    // Optional helpers if you want to throw on non-2xx:
    public String postJsonOrThrow(String path, String json) throws IOException, InterruptedException {
        var resp = post(path, json);
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("POST " + resolve(path) + " failed: " + resp.statusCode() + " -> " + resp.body());
        }
        return resp.body();
    }

    public String getJsonOrThrow(String path) throws IOException, InterruptedException {
        var resp = get(path);
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("GET " + resolve(path) + " failed: " + resp.statusCode() + " -> " + resp.body());
        }
        return resp.body();
    }
}
