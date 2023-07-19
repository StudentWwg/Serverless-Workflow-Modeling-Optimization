package UWC;

import serverlessWorkflow.graph.WVertex;

public class UWCVertex {
    private WVertex vertex;
    private double probabilityOfAllFunInEdges;
    private int numOfAllFunInEdges;
    private double urgency;

    public WVertex getVertex() { return vertex; }

    public double getProbabilityOfAllFunInEdges() {
        return probabilityOfAllFunInEdges;
    }

    public void setProbabilityOfAllFunInEdges(double probabilityOfAllFunInEdges) { this.probabilityOfAllFunInEdges = probabilityOfAllFunInEdges; }

    public int getNumOfAllFunInEdges() {
        return numOfAllFunInEdges;
    }

    public void setNumOfAllFunInEdges(int numOfAllFunInEdges) {
        this.numOfAllFunInEdges = numOfAllFunInEdges;
    }

    public double getUrgency() {
        return urgency;
    }

    public void setUrgency(double urgency) {
        this.urgency = urgency;
    }

    public UWCVertex(WVertex vertex){
        this.vertex = vertex;
        this.probabilityOfAllFunInEdges = 0;
        this.numOfAllFunInEdges = 0;
        this.urgency = 0;
    }
}
