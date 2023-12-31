package PRCP;

import serverlessWorkflow.graph.WVertex;

import java.util.TreeMap;

public class ERT_C_MEM_Config_iter {
    private double constraint;
    private double current_avg_rt;
    private double current_cost;
    private TreeMap<WVertex, Integer> current_mem_configuration;
    private int iterations_count;
    private double BCRthreshold;

    public double getConstraint() {
        return constraint;
    }

    public double getCurrent_avg_rt() {
        return current_avg_rt;
    }

    public double getCurrent_cost() {
        return current_cost;
    }

    public TreeMap<WVertex, Integer> getCurrent_mem_configuration() {
        return current_mem_configuration;
    }

    public int getIterations_count() {
        return iterations_count;
    }

    public ERT_C_MEM_Config_iter(double constraint, double current_avg_rt, double current_cost, TreeMap<WVertex, Integer> current_mem_configuration,
                                 int iterations_count, double BCRthreshold) {
        this.constraint = constraint;
        this.current_avg_rt = current_avg_rt;
        this.current_cost = current_cost;
        this.current_mem_configuration = current_mem_configuration;
        this.iterations_count = iterations_count;
        this.BCRthreshold = BCRthreshold;
    }
}
