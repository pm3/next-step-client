package io.aston;

import io.aston.nextstep.NextStepClient;

public class RunTaskClient1 {

    public static void main(String[] args) {
        try {
            NextStepClient client = NextStepClient
                    .newBuilder("http://localhost:8080")
                    .setWorkerId("worker1")
                    .setTaskThreadCount(10)
                    .setWorkerThreadCount(20)
                    .build();
            client.addLocalTask(new EchoTaskImpl());
            //client.addWorkflow(new Workflow1());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
