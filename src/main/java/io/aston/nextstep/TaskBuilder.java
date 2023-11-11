package io.aston.nextstep;

import java.lang.reflect.Type;

public class TaskBuilder {
    private String name;
    private Object params;
    private Type returnType;
    private long runningTimeout;
    private int maxRetryCount;
    private long retryWait;

    public static TaskBuilder task(String name) {
        return new TaskBuilder(name);
    }

    private TaskBuilder(String name) {
        this.name = name;
    }

    public TaskBuilder params(Object params) {
        this.params = params;
        return this;
    }

    public TaskBuilder returnType(Type returnType) {
        this.returnType = returnType;
        return this;
    }

    public TaskBuilder runningTimeout(long runningTimeout) {
        this.runningTimeout = runningTimeout;
        return this;
    }

    public TaskBuilder maxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    public TaskBuilder retryWait(long retryWait) {
        this.retryWait = retryWait;
        return this;
    }

    public TaskBuilder map(NextStepTask nextStepTask) {
        this.name = nextStepTask.name();
        return this;
    }

    public String getName() {
        return name;
    }

    public Object getParams() {
        return params;
    }

    public Type getReturnType() {
        return returnType;
    }

    public long getRunningTimeout() {
        return runningTimeout;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public long getRetryWait() {
        return retryWait;
    }
}
