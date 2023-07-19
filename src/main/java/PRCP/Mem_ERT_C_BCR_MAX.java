package PRCP;

public class Mem_ERT_C_BCR_MAX {
    private int mem;
    private double rt;
    private double cost;
    private double BCR;

    public int getMem() {
        return mem;
    }

    public void setMem(int mem) {
        this.mem = mem;
    }

    public double getRt() {
        return rt;
    }

    public void setRt(double rt) {
        this.rt = rt;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getBCR() {
        return BCR;
    }

    public Mem_ERT_C_BCR_MAX(int mem, double rt, double cost, double BCR) {
        this.mem = mem;
        this.rt = rt;
        this.cost = cost;
        this.BCR = BCR;
    }
}
