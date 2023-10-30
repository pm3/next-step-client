package io.aston;

import io.aston.nextstep.NextStepTask;

public class EchoTask {

    public static class EchoData {
        String a;
        int b;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }
    }

    @NextStepTask(name = "echo")
    public EchoData echo(EchoData data) {
        if (data != null) {
            System.out.println("echo " + data.getA() + " " + data.getB());
            if (data.getA() != null) data.setA(data.getA() + "a");
            data.setB(data.getB() + 1);
        }
        return data;
    }
}
