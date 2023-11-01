package io.aston.nextstep;

import java.util.Map;

public interface IWorkflow {
    Object exec(Map<String, Object> data) throws Exception;
}
