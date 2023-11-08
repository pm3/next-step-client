package io.aston.nextstep;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aston.nextstep.model.Workflow;
import io.aston.nextstep.model.WorkflowCreate;
import io.aston.nextstep.service.TaskService;
import io.aston.nextstep.service.WorkflowService;

import java.lang.reflect.Proxy;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NextStepClient {

    private final int taskThreadCount;
    private final Executor taskExecutor;
    private final int workerThreadCount;
    private final Executor workerExecutor;
    private final String basePath;
    private final String workerId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TaskService taskService;
    private final WorkflowService workflowService;
    private final List<String> taskNames = new ArrayList<>();

    public static NextStepBuilder newBuilder(String basePath) {
        return new NextStepBuilder(basePath);
    }

    public NextStepClient(int taskThreadCount,
                          int workerThreadCount,
                          String basePath,
                          String workerId,
                          HttpClient httpClient,
                          ObjectMapper objectMapper) {
        this.taskThreadCount = taskThreadCount;
        this.workerThreadCount = workerThreadCount;
        this.basePath = basePath;
        this.workerId = workerId;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.taskExecutor = Executors.newFixedThreadPool(taskThreadCount + 1);
        this.taskService = new TaskService(this);
        this.taskExecutor.execute(taskService::run);
        this.workerExecutor = new ThreadPerTaskExecutor();
        this.workflowService = new WorkflowService(this);
        this.workerExecutor.execute(workflowService::runTaskFinish);
    }

    static final class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            Objects.requireNonNull(r);
            new Thread(r).start();
        }
    }

    public String getBasePath() {
        return basePath;
    }

    public String getWorkerId() {
        return workerId;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public int getTaskThreadCount() {
        return taskThreadCount;
    }

    public Executor getTaskExecutor() {
        return taskExecutor;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    public Executor getWorkerExecutor() {
        return workerExecutor;
    }

    public List<String> getTaskNames() {
        return taskNames;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public WorkflowService getWorkflowService() {
        return workflowService;
    }

    public void addLocalTask(Object instance) {
        taskService.addTaskClass(instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T workflowTask(Class<T> type) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{type}, new TaskHandler(this));
    }

    public void addWorkflow(IWorkflow<?, ?> workflow) {
        workflowService.addWorkflow(workflow, null);
    }

    public void addWorkflow(IWorkflow<?, ?> workflow, String name) {
        workflowService.addWorkflow(workflow, name);
    }

    public Workflow startWorkflow(WorkflowCreate create, int timeout) throws Exception {
        return workflowService.createWorkflow(create, timeout);
    }

    public Workflow fetchWorkflow(String workflowId) throws Exception {
        return workflowService.fetchWorkflow(workflowId);
    }
}