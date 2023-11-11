package io.aston.nextstep;

import io.aston.nextstep.model.Workflow;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

public class WorkflowThread {
    private final Workflow workflow;
    private final WorkflowFactory workflowFactory;

    private static final ThreadLocal<WorkflowThread> _local = new ThreadLocal<>();

    public WorkflowThread(Workflow workflow, WorkflowFactory workflowFactory) {
        this.workflow = workflow;
        this.workflowFactory = workflowFactory;
        _local.set(this);
    }

    @SuppressWarnings("unchecked")
    public static <T> T callTask(String name, Object params, Type responseType) throws Exception {
        return (T) callTaskAsync(name, params, responseType).get();
    }

    public static <T> CompletableFuture<T> callTaskAsync(String name, Object params, Type responseType) throws Exception {
        WorkflowThread wt = _local.get();
        if (wt == null) throw new RuntimeException("call only inside workflow");
        return wt.workflowFactory.callTask(wt.workflow, TaskBuilder.task(name).params(params).returnType(responseType));
    }

    public static Workflow workflow() {
        WorkflowThread wt = _local.get();
        if (wt == null) throw new RuntimeException("call only inside workflow");
        return wt.workflow;
    }

    void finish() {
        _local.remove();
    }
}
