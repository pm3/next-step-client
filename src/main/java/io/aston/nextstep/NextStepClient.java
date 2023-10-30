package io.aston.nextstep;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aston.nextstep.model.State;
import io.aston.nextstep.model.Task;
import io.aston.nextstep.model.TaskOutput;
import io.aston.nextstep.service.TaskRunner;
import io.aston.nextstep.service.TaskService;
import io.aston.nextstep.utils.SimpleUriBuilder;

import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class NextStepClient {

    private final int threadCount;
    private final Executor executor;
    private final String basePath;
    private final String workerId;
    private final ObjectMapper objectMapper;
    private final TaskService taskService;
    private final Map<String, TaskRunner> taskRunnerMap = new ConcurrentHashMap<>();

    public static NextStepBuilder newBuilder(String basePath) {
        return new NextStepBuilder(basePath);
    }

    public NextStepClient(int threadCount, String basePath, String workerId, HttpClient httpClient, ObjectMapper objectMapper) {
        this.threadCount = threadCount;
        this.basePath = basePath;
        this.workerId = workerId;
        this.objectMapper = objectMapper;
        this.taskService = new TaskService(httpClient, objectMapper);
        this.executor = Executors.newFixedThreadPool(threadCount + 1);
        this.executor.execute(this::run);
    }

    private void run() {
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
        while (runningCount.get() > threadCount && max-- > 0) {
            Thread.sleep(50);
        }
        checkTask();
    }

    private final AtomicInteger runningCount = new AtomicInteger();

    private void checkTask() throws Exception {
        List<String> taskNames = new ArrayList<>(taskRunnerMap.keySet());
        SimpleUriBuilder b = new SimpleUriBuilder(basePath + "/v1/runtime/queues/new-tasks");
        b.param("taskName", taskNames.get(0));
        b.param("workerId", workerId);
        b.param("timeout", "10");
        Task task = taskService.nextTask(b.build());
        if (task == null) {
            System.out.println("empty body " + runningCount.get());
            return;
        }
        TaskRunner runner = taskRunnerMap.get(task.getTaskName());
        if (runner != null) {
            executor.execute(() -> {
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
        String path2 = basePath + "/v1/runtime/tasks/" + task.getId();
        taskService.putTaskOutput(new URI(path2), taskOutput);
    }

    public void addTaskClass(Object instance) {
        for (Method method : instance.getClass().getMethods()) {
            NextStepTask task = method.getAnnotation(NextStepTask.class);
            if (task != null) {
                if (method.getParameterCount() == 1) {
                    String name = task.name().length() > 0 ? task.name() : instance.getClass().getSimpleName() + "." + method.getName();
                    taskRunnerMap.put(name, new TaskRunner(method, instance, objectMapper));
                } else {
                    System.out.println("error NextStepTask method params " + method);
                }
            }
        }
    }

}