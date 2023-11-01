package io.aston.nextstep.service;

import io.aston.nextstep.model.State;
import io.aston.nextstep.model.TaskCreate;
import io.aston.nextstep.model.TaskFinish;
import io.aston.nextstep.model.Workflow;

import java.util.Date;
import java.util.Map;

public class WorkflowThread {
    private final Workflow workflow;
    private final WorkflowService workflowService;
    private TaskFinish taskFinish;
    int ref;

    private static final ThreadLocal<WorkflowThread> _local = new ThreadLocal<>();

    public WorkflowThread(Workflow workflow, WorkflowService workflowService) {
        this.workflow = workflow;
        this.workflowService = workflowService;
        _local.set(this);
    }

    public static Map<String, Object> callTask(String name, Object params) throws Exception {
        WorkflowThread wt = _local.get();
        if (wt == null) throw new RuntimeException("call only inside workflow");
        System.out.println("++callTask " + name + " " + new Date());
        Map<String, Object> out = wt._callTask(name, params);
        System.out.println("++callTaskOut " + name + " " + new Date());
        return out;
    }

    private Map<String, Object> _callTask(String name, Object params) throws Exception {
        this.taskFinish = null;
        TaskCreate taskCreate = new TaskCreate();
        taskCreate.setWorkflowId(workflow.getId());
        taskCreate.setRef(++ref);
        taskCreate.setTaskName(name);
        taskCreate.setParams(params);
        taskCreate.setRunningTimeout(30);
        taskCreate.setMaxRetryCount(3);
        taskCreate.setRetryWait(45);
        workflowService.callTask(taskCreate, this);
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(25);
                if (taskFinish != null) break;
            } catch (Throwable ignore) {
            }
        }
        if (taskFinish.getState().equals(State.COMPLETED)) {
            Map<String, Object> resp = taskFinish.getOutput();
            this.taskFinish = null;
            return resp;
        }
        throw new Exception("call task error " + taskFinish);
    }

    void setTaskOutput(TaskFinish taskFinish) {
        this.taskFinish = taskFinish;
    }

    void finish() {
        _local.remove();
    }
}
