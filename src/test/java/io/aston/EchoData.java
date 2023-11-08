package io.aston;

public class EchoData {
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

    @Override
    public String toString() {
        return "EchoData{" +
                "a='" + a + '\'' +
                ", b=" + b +
                '}';
    }
}
