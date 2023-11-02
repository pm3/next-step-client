package io.aston;

import io.aston.nextstep.IWorkflow;
import io.aston.nextstep.service.WorkflowThread;

import java.lang.reflect.Type;
import java.util.Map;

public class Workflow1 implements IWorkflow<Map<String, Object>, Map<String, Object>> {

    private Map<String, Object> tpl;

    @Override
    public Map<String, Object> exec(Map<String, Object> data) throws Exception {
        Type mapType = this.getClass().getDeclaredField("tpl").getGenericType();
        System.out.println(data);
        Map<String, Object> echo1 = WorkflowThread.callTask("echo", data, mapType);
        Map<String, Object> echo2 = WorkflowThread.callTask("echo2", echo1, mapType);
        Map<String, Object> echo3 = WorkflowThread.callTask("echo3", echo2, mapType);
        System.out.println(echo3);
        return echo3;
    }
}
