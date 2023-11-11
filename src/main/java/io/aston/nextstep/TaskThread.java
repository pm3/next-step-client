package io.aston.nextstep;

import io.aston.nextstep.model.Task;

public class TaskThread {
    private final Task task;
    private static final ThreadLocal<TaskThread> _local = new ThreadLocal<>();

    public TaskThread(Task task) {
        this.task = task;
        _local.set(this);
    }

    public static Task task() {
        TaskThread tt = _local.get();
        if (tt == null) throw new RuntimeException("call only inside task");
        return tt.task;
    }

    void finish() {
        _local.remove();
    }

}
