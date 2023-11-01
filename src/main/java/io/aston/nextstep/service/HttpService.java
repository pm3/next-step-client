package io.aston.nextstep.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aston.nextstep.model.Task;
import io.aston.nextstep.model.TaskOutput;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class HttpService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public <T> T get(URI uri, Class<T> type) throws Exception {
        System.out.println(uri);
        HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();
        return callJsonResponse(request, objectMapper.constructType(type));
    }

    public <T> List<T> getList(URI uri, Class<T> type) throws Exception {
        System.out.println(uri);
        HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();
        return callJsonResponse(request, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    public <T> T post(URI uri, Object body, Class<T> type) throws Exception {
        System.out.println(uri);
        String json = objectMapper.writeValueAsString(body);
        System.out.println("body " + json);
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .uri(uri)
                .header("Content-Type", "application/json")
                .build();
        return callJsonResponse(request, objectMapper.constructType(type));
    }

    public <T> T put(URI uri, Object body, Class<T> type) throws Exception {
        System.out.println(uri);
        String json = objectMapper.writeValueAsString(body);
        System.out.println("body " + json);
        HttpRequest request = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .uri(uri)
                .header("Content-Type", "application/json")
                .build();
        return callJsonResponse(request, objectMapper.constructType(type));
    }

    private <T> T callJsonResponse(HttpRequest request, JavaType type) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 204) {
            return null;
        }
        if (response.statusCode() != 200) {
            System.out.println("error: " + response.statusCode());
            System.out.println(response.body());
            throw new HttpException(response.statusCode(), response.body());
        }
        System.out.println("---" + request);
        System.out.println("---" + response.body());
        return objectMapper.readValue(response.body(), type);
    }

    public Task putTaskOutput(URI uri, TaskOutput taskOutput) throws Exception {
        System.out.println(uri);
        String json = objectMapper.writeValueAsString(taskOutput);
        HttpRequest request = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .uri(uri)
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.out.println("error: " + response.statusCode());
            System.out.println(response.body());
            return null;
        }
        return objectMapper.readValue(response.body(), Task.class);
    }

}
