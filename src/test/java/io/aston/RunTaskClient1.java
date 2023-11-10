package io.aston;

import io.aston.nextstep.NextStepClient;
import io.aston.nextstep.TaskFactory;

public class RunTaskClient1 {

    public static void main(String[] args) {
        try {
            NextStepClient client = NextStepClient
                    .newBuilder("http://localhost:8080")
                    .setWorkerId("worker1")
                    .build();
            TaskFactory taskFactory = client.createTaskFactory(10);
            taskFactory.addLocalTasks(new EchoTaskImpl());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
