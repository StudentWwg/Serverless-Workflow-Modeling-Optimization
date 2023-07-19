package serverlessWorkflow.PerformanceAndCostModel;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import serverlessWorkflow.graph.WVertex;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class PerfOpt {
    private ServerlessAppWorkflow App;
    private int[] avalable_mem_list;
    private double cost_under_maximal_mem_configuration, cost_under_minimal_mem_configuration, rt_under_maximal_mem_configuration, rt_under_minimal_mem_configuration;
    private TreeMap<WVertex, Integer> minimumCostConfiguration = new TreeMap<>();
    private TreeMap<WVertex, Integer> bestPerformanceConfiguration = new TreeMap<>();
    private TreeMap<WVertex, Integer> maximumCostConfiguration = new TreeMap<>();
    private TreeMap<WVertex, Integer> worstPerformanceConfiguration = new TreeMap<>();
    private TreeMap<WVertex, HashMap<Integer, Double>> avgCostMap = new TreeMap<>();
    private TreeMap<WVertex, Integer> minimal_mem_configuration, maximal_mem_configuration;

    public ServerlessAppWorkflow getApp() {
        return App;
    }

    public int[] getAvalable_mem_list() {
        return avalable_mem_list;
    }

    public double getCost_under_maximal_mem_configuration() {
        return cost_under_maximal_mem_configuration;
    }

    public double getCost_under_minimal_mem_configuration() {
        return cost_under_minimal_mem_configuration;
    }

    public double getRT_under_maximal_mem_configuration() {
        return rt_under_maximal_mem_configuration;
    }

    public double getRT_under_minimal_mem_configuration() {
        return rt_under_minimal_mem_configuration;
    }

    public TreeMap<WVertex, Integer> getMinimal_mem_configuration() {
        return minimal_mem_configuration;
    }

    public TreeMap<WVertex, Integer> getMaximal_mem_configuration() {
        return maximal_mem_configuration;
    }
    public TreeMap<WVertex, Integer> getMinimumCostConfiguration() {
        return minimumCostConfiguration;
    }

    public TreeMap<WVertex, Integer> getBestPerformanceConfiguration() {
        return bestPerformanceConfiguration;
    }

    public PerfOpt(PerfOpt opt){
        this.App = new ServerlessAppWorkflow(opt.App);
        this.avalable_mem_list = opt.avalable_mem_list;
        this.cost_under_maximal_mem_configuration = opt.cost_under_maximal_mem_configuration;
        this.cost_under_minimal_mem_configuration = opt.cost_under_minimal_mem_configuration;
        this.rt_under_maximal_mem_configuration = opt.rt_under_maximal_mem_configuration;
        this.rt_under_minimal_mem_configuration = opt.rt_under_minimal_mem_configuration;
        this.maximal_mem_configuration = opt.maximal_mem_configuration;
        this.minimal_mem_configuration = opt.minimal_mem_configuration;
        this.minimumCostConfiguration = opt.minimumCostConfiguration;
        this.bestPerformanceConfiguration = opt.bestPerformanceConfiguration;
        this.maximumCostConfiguration = opt.maximumCostConfiguration;
        this.worstPerformanceConfiguration = opt.worstPerformanceConfiguration;
        this.avgCostMap = opt.avgCostMap;
        this.generate_perf_profile();
        this.get_optimization_boundary();
    }

    public PerfOpt(ServerlessAppWorkflow Appworkflow, boolean generate_perf_profile, int[] mem_list) {
        this.App = Appworkflow;
        this.avalable_mem_list = new int[10240 - 191];
        if (mem_list == null) {
            for (int i = 192; i <= 10240; i++)
                avalable_mem_list[i-192] = i;
        } else
            this.avalable_mem_list = mem_list;
        if (generate_perf_profile == true)
            this.generate_perf_profile();
        this.get_optimization_boundary();
    }

    private void generate_perf_profile() {
        WVertex[] node_list = this.App.getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for (int i = 0; i < node_list.length; i++) {
            String perf_profile_path = null;
            String jsonContent = null;
            try {
                perf_profile_path = new File("").getCanonicalPath() + "/src/main/resources/AWSLambda_functions_perf_profile/f" +
                        (i + 1) + "_perf_profile.json";
                jsonContent = FileUtils.readFileToString(new File(perf_profile_path), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            JSONObject jsonObject = new JSONObject(jsonContent);

            for (int j : this.avalable_mem_list) {
                int mem1 = 0, mem2 = 0;
                if (j >= 192 && j < 1024) {
                    if (j % 64 == 0)
                        node_list[i].getPerf_profile().put(j, Double.valueOf(jsonObject.getInt(String.valueOf(j))));
                    else {
                        mem1 = (int) (j / 64) * 64;
                        mem2 = mem1 + 64;
                    }
                } else if (j >= 1024 && j < 2048) {
                    if (j % 128 == 0)
                        node_list[i].getPerf_profile().put(j, Double.valueOf(jsonObject.getInt(String.valueOf(j))));
                    else {
                        mem1 = (int) (j / 128) * 128;
                        mem2 = mem1 + 128;
                    }
                } else if (j >= 2048 && j < 4096) {
                    if (j % 256 == 0)
                        node_list[i].getPerf_profile().put(j, Double.valueOf(jsonObject.getInt(String.valueOf(j))));
                    else {
                        mem1 = (int) (j / 256) * 256;
                        mem2 = mem1 + 256;
                    }
                } else {
                    if (j % 512 == 0)
                        node_list[i].getPerf_profile().put(j, Double.valueOf(jsonObject.getInt(String.valueOf(j))));
                    else {
                        mem1 = (int) (j / 512) * 512;
                        mem2 = mem1 + 512;
                    }
                }
                if ((j >= 192 && j < 1024 && j % 64 != 0) || (j >= 1024 && j < 2048 && j % 128 != 0) || (j >= 2048 && j < 4096 && j % 256 != 0) ||
                        (j >= 4096 && j <= 10240 && j % 512 != 0)) {
                    double rt1 = jsonObject.getInt(String.valueOf(mem1)), rt2 = jsonObject.getInt(String.valueOf(mem2));
                    node_list[i].getPerf_profile().put(j, rt1 + (double) (j - mem1) / (mem2 - mem1) * (rt2 - rt1));
                }
            }
        }
//        this.appgenerator.get_rt_mem_data(this.App.G.node_num, node_list);
    }

    private void get_optimization_boundary(){
        Set<WVertex> verticesSet = this.App.getGraph().getDirectedGraph().vertexSet();
        this.minimal_mem_configuration = new TreeMap<WVertex, Integer>();
        this.maximal_mem_configuration = new TreeMap<WVertex, Integer>();
        for (WVertex aVertex : verticesSet) {
            this.minimal_mem_configuration.put(aVertex, aVertex.getPerf_profile().firstKey());
            this.maximal_mem_configuration.put(aVertex, aVertex.getPerf_profile().lastKey());

            HashMap<Integer, Double> memCostMap = new HashMap<>();
            double maxRT = Double.MIN_VALUE;
            double maxCost = Double.MIN_VALUE, minCost = Double.MAX_VALUE, minRT = Double.MAX_VALUE;
            int memOfMaxRt = 0, memOfMaxCost = 0, memOfMinRT = 0, memOfMinCost = 0;
            for (Map.Entry<Integer, Double> item : aVertex.getPerf_profile().entrySet()) {
                int mem = item.getKey();
                double rt = item.getValue();
                double cost = this.App.GetVertexCostInMem(aVertex, mem);
                memCostMap.put(mem, cost);
                if (rt > maxRT) {
                    maxRT = rt;
                    memOfMaxRt = mem;
                }
                if (rt < minRT) {
                    minRT = rt;
                    memOfMinRT = mem;
                }
                if (cost > maxCost) {
                    maxCost = cost;
                    memOfMaxCost = mem;
                }
                if (cost < minCost) {
                    minCost = cost;
                    memOfMinCost = mem;
                }
            }
            avgCostMap.put(aVertex, memCostMap);
            this.minimumCostConfiguration.put(aVertex, memOfMinCost);
            this.bestPerformanceConfiguration.put(aVertex, memOfMinRT);
            this.maximumCostConfiguration.put(aVertex, memOfMaxCost);
            this.worstPerformanceConfiguration.put(aVertex, memOfMaxRt);

            this.update_App_workflow_mem_rt_cost(this.minimumCostConfiguration);
            this.cost_under_minimal_mem_configuration = this.App.GetAverageCost();
            this.update_App_workflow_mem_rt_cost(this.maximumCostConfiguration);
            this.cost_under_maximal_mem_configuration = this.App.GetAverageCost();
            this.update_App_workflow_mem_rt_cost(this.bestPerformanceConfiguration);
            this.rt_under_maximal_mem_configuration = this.App.GetAverageRT();
            this.update_App_workflow_mem_rt_cost(this.worstPerformanceConfiguration);
            this.rt_under_minimal_mem_configuration = this.App.GetAverageRT();
        }
    }

    public void update_App_workflow_mem_rt_cost(TreeMap<WVertex, Integer> mem_dict) {
        Set<WVertex> vertexArr = mem_dict.keySet();
        for (WVertex aVertex : vertexArr) {
            aVertex.setMem(mem_dict.get(aVertex));
            aVertex.setRt(aVertex.getPerf_profile().get(aVertex.getMem()));
            this.App.UpdateVertexCost(aVertex);
        }
    }
}