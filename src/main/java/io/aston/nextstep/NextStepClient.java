package io.aston.nextstep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aston.nextstep.model.*;
import io.aston.nextstep.service.HttpService;
import io.aston.nextstep.utils.SimpleUriBuilder;

import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NextStepClient {
    private final String basePath;
    private final String workerId;
    private final HttpService httpService;
    private final ObjectMapper objectMapper;
    private Executor eventExecutor;
    private WorkflowFactory workflowFactory;
    private TaskFactory taskFactory;
    private final Map<EventType, List<Consumer<Event>>> handlerMap = new ConcurrentHashMap<>();

    public NextStepClient(String basePath,
                          String workerId,
                          HttpClient httpClient,
                          ObjectMapper objectMapper) {
        this.basePath = basePath;
        this.workerId = workerId;
        this.httpService = new HttpService(httpClient, objectMapper);
        this.objectMapper = objectMapper;
    }

    public static NextStepBuilder newBuilder(String basePath) {
        return new NextStepBuilder(basePath);
    }

    public void addHandler(EventType eventType, Consumer<Event> handler) {
        List<Consumer<Event>> l = handlerMap.computeIfAbsent(eventType, (k) -> new ArrayList<>());
        l.add(handler);
        if (eventExecutor == null) {
            eventExecutor = Executors.newSingleThreadExecutor();
            eventExecutor.execute(this::run);
        }
    }

    public String getBasePath() {
        return basePath;
    }

    public String getWorkerId() {
        return workerId;
    }

    public <T> T parseJsonNode(JsonNode node, Type type) throws JsonProcessingException {
        return objectMapper.treeToValue(node, objectMapper.constructType(type));
    }

    public JsonNode toJsonNode(Object val) {
        return objectMapper.valueToTree(val);
    }

    @SuppressWarnings("unchecked")
    public <T> T workflowTask(Class<T> type) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{type}, new TaskHandler());
    }

    public WorkflowFactory createWorkflowFactory(int maxThreads) {
        if (workflowFactory == null) {
            workflowFactory = new WorkflowFactory(this, maxThreads);
        }
        return workflowFactory;
    }

    public TaskFactory createTaskFactory(int maxThreads) {
        if (taskFactory == null) {
            taskFactory = new TaskFactory(this, maxThreads);
        }
        return taskFactory;
    }

    public Workflow fetchWorkflow(String workflowId) throws Exception {
        String path = basePath + "/v1/workflows/" + workflowId;
        return httpService.get(new URI(path), Workflow.class);
    }

    public Workflow createWorkflow(WorkflowCreate create, int timeout) throws Exception {
        String path = basePath + "/v1/workflows/?timeout=" + timeout;
        return httpService.post(new URI(path), create, Workflow.class);
    }

    public Workflow finishWorkflow(Workflow workflow) throws Exception {
        String path = basePath + "/v1/workflows/" + workflow.getId();
        return httpService.put(new URI(path), workflow, Workflow.class);
    }

    public Task createTask(Task taskCreate) throws Exception {
        taskCreate.setWorkerId(workerId);
        String path = basePath + "/v1/tasks/";
        return httpService.post(new URI(path), taskCreate, Task.class);
    }

    public Task fetchTask(String taskId) throws Exception {
        String path = basePath + "/v1/task/" + taskId;
        return httpService.get(new URI(path), Task.class);
    }

    public Task finishTask(Task task) throws Exception {
        String path = basePath + "/v1/tasks/" + task.getId();
        return httpService.put(new URI(path), task, Task.class);
    }

    public Event fetchNextEvent() throws Exception {
        SimpleUriBuilder b = new SimpleUriBuilder(basePath + "/v1/runtime/queues/events");
        b.param("workerId", workerId);
        b.param("timeout", "10");
        if (taskFactory != null && taskFactory.hasFreeThreads())
            taskFactory.getTaskNames().forEach((k) -> b.param("q", k));
        if (workflowFactory != null && workflowFactory.hasFreeThreads())
            workflowFactory.getWorkflowNames().forEach((k) -> b.param("q", k));
        b.param("q", workerId);
        return httpService.get(b.build(), Event.class);
    }

    public void run() {
        while (!Thread.interrupted()) {
            try {
                runStep();
            } catch (ConnectException e) {
                System.out.println("offline mode");
                try {
                    Thread.sleep(5000);
                } catch (Exception ignore) {
                }
            } catch (Throwable th) {
                th.printStackTrace();
                try {
                    Thread.sleep(2000);
                } catch (Exception ignore) {
                }
            }
        }
    }

    private void runStep() throws Exception {
        Event event = fetchNextEvent();
        if (event != null) {
            List<Consumer<Event>> l = handlerMap.get(event.type());
            if (l != null) {
                l.forEach(c -> c.accept(event));
            }
        }
    }

    public void taskCompleted(String taskId, Object data) throws Exception {
        Task task = new Task();
        task.setId(taskId);
        task.setState(State.COMPLETED);
        task.setOutput(toJsonNode(data));
        finishTask(task);
    }
}