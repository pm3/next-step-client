package io.aston;

import io.aston.nextstep.NextStepClient;
import io.aston.nextstep.WorkflowFactory;

public class RunTaskClient2 {

    public static void main(String[] args) {
        try {
            NextStepClient client = NextStepClient
                    .newBuilder("http://localhost:8080")
                    .setWorkerId("worker1")
                    .build();

            IEchoTask echoTask = client.workflowTask(IEchoTask.class);
            WorkflowFactory workflowFactory = client.createWorkflowFactory(10);
            workflowFactory.addWorkflow(new Workflow1(echoTask));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
