package io.aston;

import io.aston.nextstep.IWorkflow;
import io.aston.nextstep.service.WorkflowThread;

import java.util.Map;

public class Workflow1 implements IWorkflow {
    @Override
    public void exec(Map<String, Object> data) {
        try {
            System.out.println(data);
            Map<String, Object> echo1 = WorkflowThread.callTask("echo", data);
            Map<String, Object> echo2 = WorkflowThread.callTask("echo2", echo1);
            Map<String, Object> echo3 = WorkflowThread.callTask("echo3", echo2);
            System.out.println(echo3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
