package io.aston;

import io.aston.nextstep.NextStepTask;
import io.aston.nextstep.service.AsyncException;
import io.aston.nextstep.service.RetryException;

public interface IEchoTask {
    @NextStepTask(name = "echo")
    public EchoData echo(EchoData data) throws AsyncException, RetryException;

    @NextStepTask(name = "echo2")
    public EchoData echo2(EchoData data);

    @NextStepTask(name = "echo3")
    public EchoData echo3(EchoData data);

    @NextStepTask(name = "echoFail")
    public void echoFail() throws Exception;
}
