package io.aston;

import io.aston.nextstep.NextStepTask;

public interface IEchoTask {
    @NextStepTask(name = "echo")
    public EchoData echo(EchoData data);

    @NextStepTask(name = "echo2")
    public EchoData echo2(EchoData data);

    @NextStepTask(name = "echo3")
    public EchoData echo3(EchoData data);

    @NextStepTask(name = "echoFail")
    public void echoFail() throws Exception;
}
