package io.aston.nextstep;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.http.HttpClient;
import java.time.Duration;

public class NextStepBuilder {
    private final String basePath;
    private String workerId;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    NextStepBuilder(String basePath) {
        if (basePath == null) throw new IllegalArgumentException("basePath required");
        this.basePath = basePath;
    }

    public NextStepBuilder setWorkerId(String workerId) {
        this.workerId = workerId;
        return this;
    }

    public NextStepBuilder setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public NextStepBuilder setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public NextStepClient build() throws Exception {
        if (workerId == null) throw new Exception("workerId required");
        workerId = workerId + "-" + ProcessHandle.current().pid();
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .version(basePath.startsWith("https://") ? HttpClient.Version.HTTP_2 : HttpClient.Version.HTTP_1_1)
                    .build();
        }
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return new NextStepClient(basePath, workerId, httpClient, objectMapper);
    }
}
