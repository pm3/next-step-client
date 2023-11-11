package io.aston;

import io.aston.nextstep.NextStepClient;
import io.aston.nextstep.NextStepTask;
import io.aston.nextstep.TaskThread;
import io.aston.nextstep.service.AsyncException;
import io.aston.nextstep.service.RetryException;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class EchoTaskImpl implements IEchoTask {

    Timer timer = new Timer();
    private final NextStepClient client;

    public EchoTaskImpl(NextStepClient client) {
        this.client = client;
    }

    @Override
    @NextStepTask(name = "echo")
    public EchoData echo(EchoData data) throws AsyncException, RetryException {
        if (data != null) {
            int state = Math.abs(new Random().nextInt() % 4);
            if (state == 1) {
                System.out.println("await " + TaskThread.task().getId());
                String taskId = TaskThread.task().getId();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            client.taskCompleted(taskId, new EchoData());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 10_000L);
                throw new AsyncException("task echo");
            } else if (state == 2) {
                System.out.println("retry " + TaskThread.task().getId());
                throw new RetryException("retry");
            } else if (state == 3) {
                System.out.println("fatal " + TaskThread.task().getId());
                throw new RuntimeException("fatal");
            }
            System.out.println("echo success" + data);
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
