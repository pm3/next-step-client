package io.aston.nextstep.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public class Task {
    private String id;
    private String workflowId;
    private String workerId;
    private int ref;
    private String taskName;
    private String workflowName;
    private JsonNode params;
    private JsonNode output;
    private State state;
    private Instant created;
    private Instant modified;
    private int retries;
    private long runningTimeout;
    private int maxRetryCount;
    private long retryWait;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
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

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public JsonNode getParams() {
        return params;
    }

    public void setParams(JsonNode params) {
        this.params = params;
    }

    public JsonNode getOutput() {
        return output;
    }

    public void setOutput(JsonNode output) {
        this.output = output;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getModified() {
        return modified;
    }

    public void setModified(Instant modified) {
        this.modified = modified;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
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
}
