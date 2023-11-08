package io.aston.nextstep;

import io.aston.nextstep.service.WorkflowThread;
import io.aston.nextstep.utils.TaskRunner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class TaskHandler implements InvocationHandler {

    private final NextStepClient client;

    public TaskHandler(NextStepClient client) {
        this.client = client;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object params = method.getParameterCount() < 1 ? null : args[0];
        TaskRunner runner = client.getTaskService().getTaskRunner(method.getName());
        if (runner != null) {
            try {
                Object o = runner.invoke(args);
                createCompletedTask(method, params, o);
                return o;
            } catch (Exception e) {
                createFailedTask(method, params, e);
                throw e;
            }
        }
        return WorkflowThread.callTask(method.getName(), params, method.getGenericReturnType());
    }

    private void createFailedTask(Method method, Object params, Exception e) {
    }

    private void createCompletedTask(Method method, Object params, Object o) {
    }

}
