package io.aston.nextstep;

import io.aston.nextstep.model.Event;
import io.aston.nextstep.model.EventType;
import io.aston.nextstep.model.State;
import io.aston.nextstep.model.Task;
import io.aston.nextstep.utils.TaskRunner;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TaskFactory {

    private final NextStepClient client;
    private final Executor executor;
    private final Map<String, TaskRunner> taskRunnerMap = new ConcurrentHashMap<>();

    public TaskFactory(NextStepClient client, int maxThreads) {
        this.client = client;
        this.executor = Executors.newFixedThreadPool(maxThreads);
        this.client.addHandler(EventType.NEW_TASK, this::handleNewTask);
    }

    public void addLocalTasks(Object instance) {
        for (Method method : instance.getClass().getMethods()) {
            NextStepTask task = method.getAnnotation(NextStepTask.class);
            if (task != null) {
                if (method.getParameterCount() <= 1) {
                    String name = !task.name().isEmpty() ? task.name() : instance.getClass().getSimpleName() + "." + method.getName();
                    taskRunnerMap.put(name, new TaskRunner(method, instance, client));
                    client.addTaskName(name);
                } else {
                    System.out.println("error NextStepTask method has more params " + method);
                }
            }
        }
    }

    private void handleNewTask(Event event) {
        TaskRunner runner = taskRunnerMap.get(event.task().getTaskName());
        if (runner != null) {
            executor.execute(() -> {
                try {
                    execTask(event.task(), runner);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void execTask(Task task, TaskRunner runner) throws Exception {
        Task taskOutput = new Task();
        taskOutput.setId(task.getId());
        taskOutput.setWorkflowId(task.getWorkflowId());
        taskOutput.setWorkerId(client.getWorkerId());
        taskOutput.setTaskName(task.getTaskName());
        try {
            Object value = runner.exec(task);
            taskOutput.setState(State.COMPLETED);
            taskOutput.setOutput(client.toJsonNode(value));
        } catch (Exception e) {
            taskOutput.setState(State.FAILED);
            Map<String, String> err = Map.of(
                    "type", e.getClass().getSimpleName(),
                    "message", e.getMessage() != null ? e.getMessage() : "null"
            );
            taskOutput.setOutput(client.toJsonNode(err));
        }
        //System.out.println("++taskOutput " + taskOutput.getId() + " " + new Date());
        client.finishTask(taskOutput);
    }

}
