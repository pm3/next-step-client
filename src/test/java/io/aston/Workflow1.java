package io.aston;

import io.aston.nextstep.IWorkflow;

import java.lang.reflect.Type;
import java.util.Map;

public class Workflow1 implements IWorkflow<EchoData, EchoData> {

    private Map<String, Object> tpl;

    private final IEchoTask echoTask;

    public Workflow1(IEchoTask echoTask) {
        this.echoTask = echoTask;
    }

    @Override
    public EchoData exec(EchoData data) throws Exception {
        System.out.println("start workflow " + data);
        Type mapType = this.getClass().getDeclaredField("tpl").getGenericType();
        System.out.println(data);
        EchoData echo1 = echoTask.echo(data);
        EchoData echo2 = echoTask.echo2(echo1);
        EchoData echo3 = echoTask.echo3(echo2);
        System.out.println(echo3);
        return echo3;
    }
}
