package io.aston.nextstep.service;

import io.aston.nextstep.IWorkflow;
import io.aston.nextstep.NextStepClient;
import io.aston.nextstep.model.Task;
import io.aston.nextstep.model.TaskCreate;
import io.aston.nextstep.model.TaskFinish;
import io.aston.nextstep.model.Workflow;
import io.aston.nextstep.utils.SimpleUriBuilder;

import java.net.ConnectException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkflowService extends HttpService {
    private final NextStepClient client;
    private final Map<String, IWorkflow> workflowRunnerMap = new ConcurrentHashMap<>();
    private final Map<String, WorkflowThread> waitingThreads = new ConcurrentHashMap<>();

    public WorkflowService(NextStepClient client) {
        super(client.getHttpClient(), client.getObjectMapper());
        this.client = client;
    }

    public Workflow fetchNextWorkflow() throws Exception {
        SimpleUriBuilder b = new SimpleUriBuilder(client.getBasePath() + "/v1/runtime/queues/free-workflows");
        b.param("workerId", client.getWorkerId());
        b.param("timeout", "10");
        workflowRunnerMap.keySet().forEach((k) -> b.param("workflowName", k));
        return get(b.build(), Workflow.class);
    }

    public TaskFinish fetchNextTaskFinish() throws Exception {
        SimpleUriBuilder b = new SimpleUriBuilder(client.getBasePath() + "/v1/runtime/queues/finished-tasks");
        b.param("workerId", client.getWorkerId());
        b.param("timeout", "10");
        return get(b.build(), TaskFinish.class);
    }

    public Task fetchRunTask(TaskCreate taskCreate) throws Exception {
        taskCreate.setWorkerId(client.getWorkerId());
        String path2 = client.getBasePath() + "/v1/runtime/tasks/";
        return post(new URI(path2), taskCreate, Task.class);
    }

    public void runWorkflow() {
        while (!Thread.interrupted()) {
            try {
                runWorkflowStep();
            } catch (ConnectException e) {
                System.out.println("offline mode");
                try {
                    Thread.sleep(5000);
                } catch (Exception ignore) {
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    private void runWorkflowStep() throws Exception {
        if (workflowRunnerMap.size() == 0) {
            Thread.sleep(1000);
            return;
        }
        int max = 20000;
        while (runningWorkflowCount.get() > client.getWorkerThreadCount() && max-- > 0) {
            Thread.sleep(50);
        }
        checkWorkflow();
    }

    private final AtomicInteger runningWorkflowCount = new AtomicInteger();

    private void checkWorkflow() throws Exception {
        Workflow workflow = fetchNextWorkflow();
        if (workflow == null) {
            System.out.println("empty body workflow " + runningWorkflowCount.get());
            return;
        }
        IWorkflow exec = workflowRunnerMap.get(workflow.getWorkflowName());
        if (exec != null) {
            client.getWorkerExecutor().execute(() -> {
                runningWorkflowCount.incrementAndGet();
                WorkflowThread workflowThread = new WorkflowThread(workflow, this);
                try {
                    exec.exec(workflow.getParams());
                } catch (Exception e) {
                    System.out.println("error running workflow " + workflow.getId() + " " + e.getMessage());
                } finally {
                    runningWorkflowCount.decrementAndGet();
                    workflowThread.finish();
                }
            });
        }
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
        if (workflowRunnerMap.size() == 0) {
            Thread.sleep(1000);
            return;
        }
        TaskFinish taskFinish = fetchNextTaskFinish();
        if (taskFinish == null) {
            System.out.println("empty body taskFinish");
            return;
        }
        WorkflowThread tr = waitingThreads.get(taskFinish.getTaskId());
        if (tr != null) {
            tr.setTaskOutput(taskFinish);
        } else {
            System.out.println("output without thread " + taskFinish.getTaskId());
        }
    }

    public void addWorkflow(IWorkflow workflow, String name) {
        if (name == null) name = workflow.getClass().getSimpleName();
        workflowRunnerMap.put(name, workflow);
    }

    void callTask(TaskCreate taskCreate, WorkflowThread workflowThread) throws Exception {
        taskCreate.setWorkerId(client.getWorkerId());
        Task runningTask = fetchRunTask(taskCreate);
        waitingThreads.put(runningTask.getId(), workflowThread);
    }
}
