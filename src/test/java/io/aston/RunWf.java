package io.aston;

import io.aston.nextstep.NextStepClient;
import io.aston.nextstep.model.State;
import io.aston.nextstep.model.Workflow;
import io.aston.nextstep.model.WorkflowCreate;

import java.util.HashMap;

public class RunWf {

    public static void main(String[] args) {
        try {
            NextStepClient client = NextStepClient
                    .newBuilder("http://localhost:8080")
                    .setWorkerId("worker1")
                    .build();

            WorkflowCreate create = new WorkflowCreate();
            create.setName("Workflow1");
            create.setParams(new HashMap<>());
            create.getParams().put("a", "a");
            create.getParams().put("b", 1);
            Workflow w1 = client.createWorkflow(create, 15);
            System.out.println(w1);
            String workflowId = w1.getId();
            while (w1.getState() != State.COMPLETED) {
                Thread.sleep(3000);
                w1 = client.fetchWorkflow(workflowId);
                System.out.println(w1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
