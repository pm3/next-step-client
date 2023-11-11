package io.aston.nextstep;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.aston.nextstep.model.*;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkflowFactory {

    private final NextStepClient client;
    private final int maxThreads;
    private final AtomicInteger aktThreads = new AtomicInteger(0);
    private final Executor executor;
    private final List<String> workflowNames = new ArrayList<>();
    private final Map<String, IWorkflow<?, ?>> workflowRunnerMap = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Task>> waitingTasks = new ConcurrentHashMap<>();

    public WorkflowFactory(NextStepClient client, int maxThreads) {
        this.client = client;
        this.maxThreads = maxThreads;
        this.executor = new ThreadPerTaskExecutor();
        client.addHandler(EventType.NEW_WORKFLOW, this::handleNewWorkflow);
        client.addHandler(EventType.FINISHED_TASK, this::handleTaskFinish);
    }

    public boolean hasFreeThreads() {
        return aktThreads.get() < maxThreads;
    }

    public List<String> getWorkflowNames() {
        return workflowNames;
    }

    public void addWorkflow(IWorkflow<?, ?> workflow) {
        addWorkflow(workflow, workflow.getClass().getSimpleName());
    }

    public void addWorkflow(IWorkflow<?, ?> workflow, String name) {
        if (name == null) name = workflow.getClass().getSimpleName();
        workflowRunnerMap.put(name, workflow);
        workflowNames.add("wf_" + name);
    }

    @SuppressWarnings("unchecked")
    private void handleNewWorkflow(Event event) {
        Workflow workflow = event.workflow();
        IWorkflow<Object, Object> exec = (IWorkflow<Object, Object>) workflowRunnerMap.get(workflow.getWorkflowName());
        if (exec != null) {
            executor.execute(() -> {
                try {
                    aktThreads.incrementAndGet();
                    runWorkflow(workflow, exec);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    aktThreads.decrementAndGet();
                }
            });
        }
    }

    private void runWorkflow(Workflow workflow, IWorkflow<Object, Object> exec) {
        WorkflowThread workflowThread = new WorkflowThread(workflow, this);
        try {
            Object params = null;
            Type paramsType = paramsType(exec);
            if (workflow.getParams() != null) {
                params = client.parseJsonNode(workflow.getParams(), paramsType);
            }
            Object output = exec.exec(params);
            workflow.setState(State.COMPLETED);
            workflow.setOutput(client.toJsonNode(output));
        } catch (Exception e) {
            System.out.println("error running workflow " + workflow.getId() + " " + e.getMessage());
            workflow.setState(State.FAILED);
            workflow.setOutput(error(e));
        } finally {
            workflowThread.finish();
            try {
                client.finishWorkflow(workflow);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private JsonNode error(Exception e) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("type", e.getClass().getSimpleName());
        root.put("message", e.getMessage() != null ? e.getMessage() : "null");
        return root;
    }

    private Type paramsType(IWorkflow<?, ?> exec) {
        for (Method m : exec.getClass().getMethods()) {
            if (m.getName().equals("exec"))
                return m.getGenericParameterTypes()[0];
        }
        return null;
    }

    private void handleTaskFinish(Event event) {
        Task taskFinish = event.task();
        //System.out.println("++callTaskFinish " + taskFinish.getId() + " " + new Date());
        CompletableFuture<Task> tr = waitingTasks.remove(taskFinish.getId());
        if (tr != null) {
            tr.complete(taskFinish);
        } else {
            System.out.println("output without thread " + taskFinish.getId());
        }
    }

    public <T> CompletableFuture<T> callTask(Workflow workflow, TaskBuilder builder) {
        CompletableFuture<Task> future = new CompletableFuture<>();
        Task taskCreate = new Task();
        taskCreate.setWorkflowId(workflow.getId());
        taskCreate.setTaskName(builder.getName());
        taskCreate.setRunningTimeout(builder.getRunningTimeout());
        taskCreate.setMaxRetryCount(builder.getMaxRetryCount());
        taskCreate.setRetryWait(builder.getRetryWait());
        taskCreate.setParams(client.toJsonNode(builder.getParams()));
        taskCreate.setWorkerId(client.getWorkerId());
        try {
            Task runningTask = client.createTask(taskCreate);
            waitingTasks.put(runningTask.getId(), future);
        } catch (Exception e) {
            future.completeExceptionally(new Exception("error call task " + e.getMessage()));
        }
        return future.thenApply(t -> {
            if (t.getState().equals(State.COMPLETED)) {
                if (t.getOutput() != null) {
                    try {
                        return client.parseJsonNode(t.getOutput(), builder.getReturnType());
                    } catch (Exception e) {
                        throw new RuntimeException("parse output error " + t.getOutput());
                    }
                } else {
                    return null;
                }
            } else if (t.getState() == State.FAILED) {
                throw new RuntimeException("failed response " + t.getOutput());
            }
            throw new RuntimeException("call task error " + t);
        });
    }

    static final class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            Objects.requireNonNull(r);
            new Thread(r).start();
        }
    }
}
