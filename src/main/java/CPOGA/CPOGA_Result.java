package CPOGA;

import serverlessWorkflow.graph.WVertex;

import java.util.TreeMap;

public class CPOGA_Result {
    private int numOfGenes;
    private double crossRate;
    private double mutateRate;
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

    public void setNumOfGenes(int numOfGenes) {
        this.numOfGenes = numOfGenes;
    }

    public double getCrossRate() {
        return crossRate;
    }

    public void setCrossRate(double crossRate) {
        this.crossRate = crossRate;
    }

    public double getMutateRate() {
        return mutateRate;
    }

    public void setMutateRate(double mutateRate) {
        this.mutateRate = mutateRate;
    }

    public int getNumOfGenerations() {
        return numOfGenerations;
    }

    public void setNumOfGenerations(int numOfGenerations) {
        this.numOfGenerations = numOfGenerations;
    }

    public double getEtaM() {
        return etaM;
    }

    public void setEtaM(double etaM) {
        this.etaM = etaM;
    }

    public double getConstraint() {
        return constraint;
    }

    public void setConstraint(double constraint) {
        this.constraint = constraint;
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

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public TreeMap<WVertex, Integer> getMemConfig() {
        return memConfig;
    }

    public void setMemConfig(TreeMap<WVertex, Integer> memConfig) {
        this.memConfig = memConfig;
    }

    public CPOGA_Result(int numOfGenes, double crossRate, double mutateRate, int numOfGenerations, double etaM, double constraint, double rt, double cost, double fitness, TreeMap<WVertex, Integer> memList) {
        this.numOfGenes = numOfGenes;
        this.crossRate = crossRate;
        this.mutateRate = mutateRate;
        this.numOfGenerations = numOfGenerations;
        this.etaM = etaM;
        this.constraint = constraint;
        this.rt = rt;
        this.cost = cost;
        this.fitness = fitness;
        this.memConfig = new TreeMap<WVertex, Integer>(memList);
    }
}
