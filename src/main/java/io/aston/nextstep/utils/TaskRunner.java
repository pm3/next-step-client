package io.aston.nextstep.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import io.aston.nextstep.model.Task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TaskRunner {

    private final Method method;
    private final Object instance;
    private final JavaType argType;
    private final ObjectMapper objectMapper;

    public TaskRunner(Method method, Object instance, ObjectMapper objectMapper) {
        this.method = method;
        this.instance = instance;
        this.objectMapper = objectMapper;
        this.argType = method.getParameterCount() == 1
                ? objectMapper.constructType(method.getGenericParameterTypes()[0])
                : null;
    }

    public Object exec(Task task) throws Exception {
        try {
            if (argType != null && task.getParams() != null) {
                Object arg0 = objectMapper.readValue(new TreeTraversingParser(task.getParams()), argType);
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
