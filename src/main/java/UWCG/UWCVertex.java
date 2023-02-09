package UWCG;

import serverlessWorkflow.graph.WVertex;

public class UWCVertex extends WVertex {
    private double probabilityOfAllFunInEdges;  //所有扇入边的概率之和
    private int numOfAllFunInEdges;  //扇入边个数
    private double urgency;

    public double getProbabilityOfAllFunInEdges() {
        return probabilityOfAllFunInEdges;
    }

    public void setProbabilityOfAllFunInEdges(double probabilityOfAllFunInEdges) {
        this.probabilityOfAllFunInEdges = probabilityOfAllFunInEdges;
    }

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
        super(vertex);
        this.probabilityOfAllFunInEdges = 0;
        this.numOfAllFunInEdges = 0;
        this.urgency = 0;
    }
}
