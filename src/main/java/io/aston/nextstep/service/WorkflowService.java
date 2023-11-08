package io.aston.nextstep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import io.aston.nextstep.IWorkflow;
import io.aston.nextstep.NextStepClient;
import io.aston.nextstep.model.State;
import io.aston.nextstep.model.Task;
import io.aston.nextstep.model.Workflow;
import io.aston.nextstep.model.WorkflowCreate;
import io.aston.nextstep.utils.SimpleUriBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkflowService extends HttpService {
    private final NextStepClient client;
    private final Map<String, IWorkflow<?, ?>> workflowRunnerMap = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Task>> waitingTasks = new ConcurrentHashMap<>();

    public WorkflowService(NextStepClient client) {
        super(client.getHttpClient(), client.getObjectMapper());
        this.client = client;
    }

    public Task fetchNextTaskFinish() throws Exception {
        SimpleUriBuilder b = new SimpleUriBuilder(client.getBasePath() + "/v1/runtime/queues/finished-tasks");
        b.param("workerId", client.getWorkerId());
        b.param("timeout", "10");
        return get(b.build(), Task.class);
    }

    public Task createTask(Task taskCreate) throws Exception {
        taskCreate.setWorkerId(client.getWorkerId());
        String path = client.getBasePath() + "/v1/tasks/";
        return post(new URI(path), taskCreate, Task.class);
    }

    public Workflow finishWorkflow(Workflow workflow) throws Exception {
        String path = client.getBasePath() + "/v1/workflows/" + workflow.getId();
        return put(new URI(path), workflow, Workflow.class);
    }

    private final AtomicInteger runningWorkflowCount = new AtomicInteger();

    @SuppressWarnings("unchecked")
    public void startWorkflow(Task task) {

        IWorkflow<Object, Object> exec = (IWorkflow<Object, Object>) workflowRunnerMap.get(task.getWorkflowName());
        if (exec != null) {
            client.getWorkerExecutor().execute(() -> {
                runningWorkflowCount.incrementAndGet();
                Workflow workflow = fromInitTask(task);
                WorkflowThread workflowThread = new WorkflowThread(workflow, this);
                try {
                    Object params = null;
                    Type paramsType = paramsType(exec);
                    if (workflow.getParams() instanceof ObjectNode obj) {
                        JsonNode paramsNode = obj.get("params");
                        if (paramsNode != null) {
                            params = client.getObjectMapper().treeToValue(paramsNode, client.getObjectMapper().constructType(paramsType));
                        }
                        workflow.setUniqueCode(obj.get("uniqueCode").asText());
                    }
                    Object output = exec.exec(params);
                    workflow.setState(State.COMPLETED);
                    workflow.setOutput(toJsonNode(output));
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    System.out.println("error running workflow " + task.getWorkflowId() + " " + e.getMessage());
                    workflow.setState(State.FATAL_ERROR);
                    workflow.setOutput(toJsonNode(Map.of(
                            "type", e.getClass().getSimpleName(),
                            "message", e.getMessage())));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("error running workflow " + task.getWorkflowId() + " " + e.getMessage());
                    workflow.setState(State.FAILED);
                    workflow.setOutput(toJsonNode(Map.of(
                            "type", e.getClass().getSimpleName(),
                            "message", e.getMessage())));
                } finally {
                    runningWorkflowCount.decrementAndGet();
                    workflowThread.finish();
                    try {
                        finishWorkflow(workflow);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private Type paramsType(IWorkflow<?, ?> exec) {
        for (Method m : exec.getClass().getMethods()) {
            if (m.getName().equals("exec"))
                return m.getGenericParameterTypes()[0];
        }
        return null;
    }

    private JsonNode toJsonNode(Object val) {
        if (val == null) return null;
        return client.getObjectMapper().valueToTree(val);
    }

    private Workflow fromInitTask(Task task) {
        Workflow workflow = new Workflow();
        workflow.setId(task.getWorkflowId());
        workflow.setUniqueCode(task.getTaskName());
        workflow.setWorkflowName(task.getWorkflowName());
        workflow.setCreated(task.getCreated());
        workflow.setModified(task.getModified());
        workflow.setState(task.getState());
        workflow.setParams(task.getParams());
        return workflow;
    }

    //////
    public void runTaskFinish() {
        while (!Thread.interrupted()) {
            try {
                runTaskFinishStep();
            } catch (ConnectException e) {
                System.out.println("offline mode task-finish");
                try {
                    Thread.sleep(5000);
                } catch (Exception ignore) {
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    private void runTaskFinishStep() throws Exception {
        if (workflowRunnerMap.isEmpty()) {
            Thread.sleep(1000);
            return;
        }
        Task taskFinish = fetchNextTaskFinish();
        if (taskFinish == null) {
            //System.out.println("empty body taskFinish " + new Date());
            return;
        }
        //System.out.println("++callTaskFinish " + taskFinish.getId() + " " + new Date());
        CompletableFuture<Task> tr = waitingTasks.remove(taskFinish.getId());
        if (tr != null) {
            tr.complete(taskFinish);
        } else {
            System.out.println("output without thread " + taskFinish.getId());
        }
    }

    public void addWorkflow(IWorkflow<?, ?> workflow, String name) {
        if (name == null) name = workflow.getClass().getSimpleName();
        workflowRunnerMap.put(name, workflow);
        client.getTaskNames().add("wf:" + name);
    }

    public <T> CompletableFuture<T> callTask(Task taskCreate, Object params, Type responseType) throws Exception {
        CompletableFuture<Task> future = new CompletableFuture<>();
        taskCreate.setParams(client.getObjectMapper().valueToTree(params));
        taskCreate.setWorkerId(client.getWorkerId());
        try {
            Task runningTask = createTask(taskCreate);
            waitingTasks.put(runningTask.getId(), future);
        } catch (Exception e) {
            future.completeExceptionally(new Exception("error call task " + e.getMessage()));
        }
        return future.thenApply(t -> {
            if (t.getState().equals(State.COMPLETED)) {
                if (t.getOutput() != null) {
                    try {
                        return client.getObjectMapper().readValue(new TreeTraversingParser(t.getOutput()), client.getObjectMapper().constructType(responseType));
                    } catch (Exception e) {
                        throw new RuntimeException("parse output error " + t.getOutput());
                    }
                } else {
                    return null;
                }
            } else if (t.getState() == State.FAILED) {
                throw new RuntimeException("failed response " + t.getOutput());
            } else if (t.getState() == State.FATAL_ERROR) {
                throw new RuntimeException("fatal error");
            }
            throw new RuntimeException("call task error " + t);
        });
    }

    public Workflow createWorkflow(WorkflowCreate create, int timeout) throws Exception {
        String path = client.getBasePath() + "/v1/workflows/?timeout=" + timeout;
        return post(new URI(path), create, Workflow.class);
    }

    public Workflow fetchWorkflow(String workflowId) throws Exception {
        String path = client.getBasePath() + "/v1/workflows/" + workflowId;
        return get(new URI(path), Workflow.class);
    }
}
