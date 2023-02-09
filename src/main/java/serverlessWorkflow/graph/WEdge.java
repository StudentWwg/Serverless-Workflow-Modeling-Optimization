package serverlessWorkflow.graph;

public class WEdge {
    private double weight;
    private double edge_delay;
    private WVertex v1;
    private WVertex v2;

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getEdge_delay() {
        return edge_delay;
    }

    public void setEdge_delay(double edge_delay) {
        this.edge_delay = edge_delay;
    }

    public WVertex getV1() {
        return v1;
    }

    public void setV1(WVertex v1) {
        this.v1 = v1;
    }

    public WVertex getV2() {
        return v2;
    }

    public void setV2(WVertex v2) {
        this.v2 = v2;
    }

    WEdge(WVertex v1, WVertex v2, double weight) {
        this.weight = weight;
        edge_delay = 0;
        this.v1 = v1;
        this.v2 = v2;
    }

    @Override
    public String toString() {
        return String.valueOf(this.weight);
    }
}
