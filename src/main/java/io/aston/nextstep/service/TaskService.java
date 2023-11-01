package io.aston.nextstep.service;

import io.aston.nextstep.NextStepClient;
import io.aston.nextstep.NextStepTask;
import io.aston.nextstep.model.State;
import io.aston.nextstep.model.Task;
import io.aston.nextstep.model.TaskOutput;
import io.aston.nextstep.utils.SimpleUriBuilder;
import io.aston.nextstep.utils.TaskRunner;

import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskService extends HttpService {

    private final NextStepClient client;
    private final Map<String, TaskRunner> taskRunnerMap = new ConcurrentHashMap<>();

    public TaskService(NextStepClient client) {
        super(client.getHttpClient(), client.getObjectMapper());
        this.client = client;
    }

    public Task fetchNextTask() throws Exception {
        SimpleUriBuilder b = new SimpleUriBuilder(client.getBasePath() + "/v1/runtime/queues/new-tasks");
        b.param("workerId", client.getWorkerId());
        b.param("timeout", "10");
        taskRunnerMap.keySet().forEach((k) -> b.param("taskName", k));
        return get(b.build(), Task.class);
    }

    public Task putTaskOutput(TaskOutput taskOutput) throws Exception {
        String path2 = client.getBasePath() + "/v1/runtime/tasks/" + taskOutput.getTaskId();
        return put(new URI(path2), taskOutput, Task.class);
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
            }
        }
    }

    private void runStep() throws Exception {
        if (taskRunnerMap.size() == 0) {
            Thread.sleep(1000);
            return;
        }
        int max = 200;
        while (runningCount.get() > client.getTaskThreadCount() && max-- > 0) {
            Thread.sleep(50);
        }
        checkTask();
    }

    private final AtomicInteger runningCount = new AtomicInteger();

    private void checkTask() throws Exception {
        Task task = fetchNextTask();
        if (task == null) {
            System.out.println("empty body task " + runningCount.get());
            return;
        }
        TaskRunner runner = taskRunnerMap.get(task.getTaskName());
        if (runner != null) {
            client.getTaskExecutor().execute(() -> {
                execTask(task, runner);
            });
        }
    }

    private void execTask(Task task, TaskRunner runner) {
        System.out.println("running task " + task.getTaskName() + " concurrency " + runningCount.get());
        try {
            runningCount.incrementAndGet();
            execTask0(task, runner);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            runningCount.decrementAndGet();
        }
    }

    private void execTask0(Task task, TaskRunner runner) throws Exception {
        TaskOutput taskOutput = new TaskOutput();
        taskOutput.setTaskId(task.getId());
        try {
            Object value = runner.exec(task);
            taskOutput.setState(State.COMPLETED);
            taskOutput.setOutput(value);
        } catch (Exception e) {
            taskOutput.setState(State.FAILED);
            Map<String, String> err = Map.of(
                    "type", e.getClass().getSimpleName(),
                    "message", e.getMessage()
            );
            taskOutput.setOutput(err);
        }
        putTaskOutput(taskOutput);
    }

    public void addTaskClass(Object instance) {
        for (Method method : instance.getClass().getMethods()) {
            NextStepTask task = method.getAnnotation(NextStepTask.class);
            if (task != null) {
                if (method.getParameterCount() == 1) {
                    String name = task.name().length() > 0 ? task.name() : instance.getClass().getSimpleName() + "." + method.getName();
                    taskRunnerMap.put(name, new TaskRunner(method, instance, client.getObjectMapper()));
                } else {
                    System.out.println("error NextStepTask method params " + method);
                }
            }
        }
    }

}
