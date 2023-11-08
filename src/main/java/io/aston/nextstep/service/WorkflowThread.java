package io.aston.nextstep.service;

import io.aston.nextstep.model.Task;
import io.aston.nextstep.model.Workflow;

import java.lang.reflect.Type;

public class WorkflowThread {
    private final Workflow workflow;
    private final WorkflowService workflowService;
    int ref;

    private static final ThreadLocal<WorkflowThread> _local = new ThreadLocal<>();

    public WorkflowThread(Workflow workflow, WorkflowService workflowService) {
        this.workflow = workflow;
        this.workflowService = workflowService;
        _local.set(this);
    }

    public static <T> T callTask(String name, Object params, Type responseType) throws Exception {
        WorkflowThread wt = _local.get();
        if (wt == null) throw new RuntimeException("call only inside workflow");
        return wt._callTask(name, params, responseType);
    }

    @SuppressWarnings("unchecked")
    private <T> T _callTask(String name, Object params, Type responseType) throws Exception {
        Task taskCreate = new Task();
        taskCreate.setWorkflowId(workflow.getId());
        taskCreate.setRef(++ref);
        taskCreate.setTaskName(name);
        taskCreate.setRunningTimeout(30);
        taskCreate.setMaxRetryCount(3);
        taskCreate.setRetryWait(45);
        return (T) workflowService.callTask(taskCreate, params, responseType).get();
    }

    void finish() {
        _local.remove();
    }
}
