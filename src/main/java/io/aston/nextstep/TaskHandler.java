package io.aston.nextstep;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class TaskHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object params = method.getParameterCount() < 1 ? null : args[0];
        return WorkflowThread.callTask(method.getName(), params, method.getGenericReturnType());
    }
}
