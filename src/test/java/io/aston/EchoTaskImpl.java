package io.aston;

import io.aston.nextstep.NextStepTask;

public class EchoTaskImpl implements IEchoTask {

    @Override
    @NextStepTask(name = "echo")
    public EchoData echo(EchoData data) {
        if (data != null) {
            //System.out.println("echo " + data.getA() + " " + data.getB());
            if (data.getA() != null) data.setA(data.getA() + "a");
            data.setB(data.getB() + 1);
        }
        return data;
    }

    @Override
    @NextStepTask(name = "echo2")
    public EchoData echo2(EchoData data) {
        if (data != null) {
            //System.out.println("echo " + data.getA() + " " + data.getB());
            if (data.getA() != null) data.setA(data.getA() + "b");
            data.setB(data.getB() + 1);
        }
        return data;
    }

    @Override
    @NextStepTask(name = "echo3")
    public EchoData echo3(EchoData data) {
        if (data != null) {
            //System.out.println("echo " + data.getA() + " " + data.getB());
            if (data.getA() != null) data.setA(data.getA() + "c");
            data.setB(data.getB() + 1);
        }
        return data;
    }

    @Override
    @NextStepTask(name = "echoFail")
    public void echoFail() throws Exception {
        throw new Exception("error echoFail");
    }
}
