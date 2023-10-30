package io.aston.nextstep.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aston.nextstep.model.Task;
import io.aston.nextstep.model.TaskOutput;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TaskService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TaskService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public Task nextTask(URI uri) throws Exception {
        System.out.println(uri);
        HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 204) {
            return null;
        }
        if (response.statusCode() != 200) {
            System.out.println("error: " + response.statusCode());
            System.out.println(response.body());
            return null;
        }
        return objectMapper.readValue(response.body(), Task.class);
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
