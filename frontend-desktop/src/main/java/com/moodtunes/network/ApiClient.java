package com.moodtunes.network;

import java.io.IOException;
import java.net.URI; //URL-like address (https://api.example.com/users).
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ApiClient {
    private final HttpClient client; //http engine
    private final String baseUrl; //the root URL for your backend (e.g., http://localhost:8080 in dev

    public ApiClient() {
        //Figures out which server to talk to
        this.baseUrl = Optional.ofNullable(System.getProperty("api.base.url"))
            .orElse(Optional.ofNullable(System.getenv("MOODTUNES_API_URL"))
                .orElse("http://localhost:5000"));
        //HTTP client with a 5-second connect timeout
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    //It returns an HttpResponse<String> (status code + headers + body as text).
    public HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()///Creates an immutable HTTP reques
            .uri(URI.create(resolve(path))) 
            .GET()
            .header("Accept", "application/json") //request json repsone 
            .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public HttpResponse<String> post(String path, String json) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(resolve(path)))
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .header("Content-Type", "application/json")
            .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    //If you pass "/tracks", it becomes baseUrl + "/tracks".
    private String resolve(String path) { 
        if (path.startsWith("/")) return baseUrl + path;
        return baseUrl + "/" + path;
    }

    public String getBaseUrl() { return baseUrl; }
}