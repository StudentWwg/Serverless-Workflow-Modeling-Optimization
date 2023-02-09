package serverlessWorkflow.graph;

import java.util.Objects;
import java.util.TreeMap;

public class WVertex implements Comparable {
    private String vertexInfo;
    private double rt; // response time
    private TreeMap<Integer, Double> perf_profile;
    private int mem; // memory configuration
    private int[] available_mem_list;
    private double cost;
    private double node_delay;
    private double BCR;  //BCR属性只用于对比算法

    public String getVertexInfo() {
        return vertexInfo;
    }

    public void setVertexInfo(String vertexInfo) {
        this.vertexInfo = vertexInfo;
    }

    public double getRt() {
        return rt;
    }

    public void setRt(double rt) {
        this.rt = rt;
    }

    public TreeMap<Integer, Double> getPerf_profile() {
        return perf_profile;
    }

    public void setPerf_profile(TreeMap<Integer, Double> perf_profile) {
        this.perf_profile = perf_profile;
    }

    public int getMem() {
        return mem;
    }

    public void setMem(int mem) {
        this.mem = mem;
    }

    public int[] getAvailable_mem_list() {
        return available_mem_list;
    }

    public void setAvailable_mem_list(int[] available_mem_list) {
        this.available_mem_list = available_mem_list;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getNode_delay() {
        return node_delay;
    }

    public void setNode_delay(double node_delay) {
        this.node_delay = node_delay;
    }

    public double getBCR() {
        return BCR;
    }

    public void setBCR(double BCR) {
        this.BCR = BCR;
    }

    public WVertex(String vertexInfo) {
        this.vertexInfo = vertexInfo;
        rt = 0;
        perf_profile = new TreeMap<Integer, Double>();
        mem = 128;
        available_mem_list = null;
        cost = 0;
        node_delay = 0;
        BCR = 0;
    }
    public WVertex(WVertex vertex){  // copy constructor
        this.vertexInfo  = vertex.vertexInfo;;
        this.rt = vertex.rt;
        this.cost = vertex.cost;
        this.node_delay = vertex.node_delay;
        this.BCR = vertex.BCR;
        this.mem = vertex.mem;
        this.perf_profile = new TreeMap<Integer, Double>();
        for(Integer key : vertex.perf_profile.keySet())
            this.perf_profile.put(key,vertex.perf_profile.get(key));
        this.available_mem_list = new int[vertex.available_mem_list.length];
        for(int i=0;i< this.available_mem_list.length;i++){
            this.available_mem_list[i] = vertex.available_mem_list[i];
        }
    }

    @Override
    public String toString() {  //toString的返回值会显示在绘制的图中
        return this.vertexInfo;
    }

    @Override
    public int compareTo(Object o) {
        WVertex temp = (WVertex) o;
        if (this.vertexInfo.length() != temp.vertexInfo.length())
            return this.vertexInfo.length() - temp.vertexInfo.length();
        else
            return this.vertexInfo.compareTo(temp.vertexInfo);
    }
}
