package io.aston.nextstep.utils;

import io.aston.nextstep.NextStepClient;
import io.aston.nextstep.model.Task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class TaskRunner {

    private final Method method;
    private final Object instance;
    private final Type argType;
    private final NextStepClient client;

    public TaskRunner(Method method, Object instance, NextStepClient client) {
        this.method = method;
        this.instance = instance;
        this.client = client;
        this.argType = method.getParameterCount() == 1
                ? method.getGenericParameterTypes()[0]
                : null;
    }
    
    public Object exec(Task task) throws Exception {
        try {
            if (argType != null && task.getParams() != null) {
                Object arg0 = client.parseJsonNode(task.getParams(), argType);
                return method.invoke(instance, arg0);
            } else {
                return method.invoke(instance);
            }
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof Exception ee)
                throw ee;
            throw e;
        }
    }
}
