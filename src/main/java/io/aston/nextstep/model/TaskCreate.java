package io.aston.nextstep.model;

public class TaskCreate {
    private String workflowId;
    private int ref;
    private String taskName;
    private Object params;
    private long runningTimeout;
    private int maxRetryCount;
    private long retryWait;
    private String workerId;

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public int getRef() {
        return ref;
    }

    public void setRef(int ref) {
        this.ref = ref;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public long getRunningTimeout() {
        return runningTimeout;
    }

    public void setRunningTimeout(long runningTimeout) {
        this.runningTimeout = runningTimeout;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public long getRetryWait() {
        return retryWait;
    }

    public void setRetryWait(long retryWait) {
        this.retryWait = retryWait;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
}
