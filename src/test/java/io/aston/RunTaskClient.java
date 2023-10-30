package io.aston;

import io.aston.nextstep.NextStepClient;

public class RunTaskClient {

    public static void main(String[] args) {
        try {
            NextStepClient client = NextStepClient
                    .newBuilder("http://localhost:8080")
                    .setWorkerId("worker1")
                    .setThreadCount(10)
                    .build();
            client.addTaskClass(new EchoTask());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
