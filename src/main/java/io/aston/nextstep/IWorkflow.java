package io.aston.nextstep;

public interface IWorkflow<T, R> {
    R exec(T data) throws Exception;
}
