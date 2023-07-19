package EASW;

import serverlessWorkflow.graph.WVertex;

import java.util.TreeMap;

public class EASW_Result {
    private int numOfGenes;
    private double crossRate;
    private double mutationRate;
    private int numOfGenerations;
    private double etaM;
    private double constraint;
    private double rt;
    private double cost;
    private double fitness;
    private TreeMap<WVertex, Integer> memConfig;

    public int getNumOfGenes() {
        return numOfGenes;
    }

    public double getCrossRate() {
        return crossRate;
    }

    public double getMutateRate() {
        return mutationRate;
    }

    public int getNumOfGenerations() {
        return numOfGenerations;
    }

    public double getEtaM() {
        return etaM;
    }

    public double getConstraint() {
        return constraint;
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

    public double getFitness() {
        return fitness;
    }

    public TreeMap<WVertex, Integer> getMemConfig() {
        return memConfig;
    }

    public EASW_Result(int numOfGenes, double crossRate, double mutationRate, int numOfGenerations, double etaM, double constraint, double rt, double cost, double fitness, TreeMap<WVertex, Integer> memList) {
        this.numOfGenes = numOfGenes;
        this.crossRate = crossRate;
        this.mutationRate = mutationRate;
        this.numOfGenerations = numOfGenerations;
        this.etaM = etaM;
        this.constraint = constraint;
        this.rt = rt;
        this.cost = cost;
        this.fitness = fitness;
        this.memConfig = new TreeMap<WVertex, Integer>(memList);
    }
}
