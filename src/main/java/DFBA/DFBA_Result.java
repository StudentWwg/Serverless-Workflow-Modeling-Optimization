package DFBA;

import serverlessWorkflow.graph.WVertex;

import java.util.TreeMap;

/**
 * @ClassName DBFAVertex
 * @Description TODO
 * @Author wwg
 * @Date 2023/2/20 20:50
 * @Version 1.0
 */
public class DFBA_Result {
    private double constraint;
    private double rt;
    private double cost;
    private TreeMap<WVertex, Integer> memConfig;

    public double getConstraint() {
        return constraint;
    }

    public double getRt() {
        return rt;
    }

    public double getCost() {
        return cost;
    }

    public TreeMap<WVertex, Integer> getMemConfig() {
        return memConfig;
    }

    public DFBA_Result(double constraint,double rt,double cost,TreeMap<WVertex,Integer> memConfig){
        this.constraint = constraint;
        this.rt = rt;
        this.cost = cost;
        this.memConfig = memConfig;
    }
}
