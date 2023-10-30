package io.aston.nextstep.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import io.aston.nextstep.model.Task;

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
        this.argType = objectMapper.constructType(method.getGenericParameterTypes()[0]);
    }

    public Object exec(Task task) throws Exception {
        Object arg0 = null;
        if (task.getParams() != null) {
            arg0 = objectMapper.readValue(new TreeTraversingParser(task.getParams()), argType);
        }
        return method.invoke(instance, arg0);
    }
}
