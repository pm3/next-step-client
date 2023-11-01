package io.aston.nextstep.service;

import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import io.aston.nextstep.IWorkflow;
import io.aston.nextstep.NextStepClient;
import io.aston.nextstep.model.Task;
import io.aston.nextstep.model.TaskCreate;
import io.aston.nextstep.model.TaskFinish;
import io.aston.nextstep.model.Workflow;
import io.aston.nextstep.utils.SimpleUriBuilder;

import java.net.ConnectException;
import java.net.URI;
import java.util.Date;
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

    private final AtomicInteger runningWorkflowCount = new AtomicInteger();

    public void startWorkflow(Task task) {

        IWorkflow exec = workflowRunnerMap.get(task.getWorkflowName());
        if (exec != null) {
            client.getWorkerExecutor().execute(() -> {
                runningWorkflowCount.incrementAndGet();
                Workflow workflow = fromInitTask(task);
                WorkflowThread workflowThread = new WorkflowThread(workflow, this);
                try {
                    exec.exec(workflow.getParams());
                } catch (Exception e) {
                    System.out.println("error running workflow " + task.getWorkflowId() + " " + e.getMessage());
                } finally {
                    runningWorkflowCount.decrementAndGet();
                    workflowThread.finish();
                }
            });
        }
    }

    private Workflow fromInitTask(Task task) {
        Workflow workflow = new Workflow();
        workflow.setId(task.getWorkflowId());
        workflow.setUniqueCode(task.getTaskName());
        workflow.setWorkflowName(task.getWorkflowName());
        workflow.setCreated(task.getCreated());
        workflow.setModified(task.getModified());
        workflow.setState(task.getState());
        if (task.getParams() != null) {
            try {
                workflow.setParams(client.getObjectMapper().readValue(new TreeTraversingParser(task.getParams()), Map.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        TaskFinish taskFinish = fetchNextTaskFinish();
        if (taskFinish == null) {
            System.out.println("empty body taskFinish " + new Date());
            return;
        }
        System.out.println("++callTaskFinish " + taskFinish.getTaskId() + " " + new Date());
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
        client.getTaskNames().add("wf:" + name);
    }

    void callTask(TaskCreate taskCreate, WorkflowThread workflowThread) throws Exception {
        taskCreate.setWorkerId(client.getWorkerId());
        Task runningTask = fetchRunTask(taskCreate);
        if (runningTask.getId() != null) {
            waitingThreads.put(runningTask.getId(), workflowThread);
        }
    }
}
