package PRCP;

public class ERT_C_BCR_MAX {
    private double rt;
    private double cost;
    private double BCR;

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


    public ERT_C_BCR_MAX(double rt, double cost, double BCR) {
        this.rt = rt;
        this.cost = cost;
        this.BCR = BCR;
    }
}
