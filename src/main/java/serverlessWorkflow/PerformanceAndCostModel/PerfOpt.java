package serverlessWorkflow.PerformanceAndCostModel;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import serverlessWorkflow.graph.WVertex;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class PerfOpt {
    private ServerlessAppWorkflow App;
//    public APPGenerator appgenerator;
    private int[] avalable_mem_list;
    private double maximal_cost, minimal_cost, minimal_avg_rt, maximal_avg_rt;
    private TreeMap<WVertex, Integer> minimal_mem_configuration, maximal_mem_configuration;

    public ServerlessAppWorkflow getApp() {
        return App;
    }

    public void setApp(ServerlessAppWorkflow app) {
        App = app;
    }

    public int[] getAvalable_mem_list() {
        return avalable_mem_list;
    }

    public void setAvalable_mem_list(int[] avalable_mem_list) {
        this.avalable_mem_list = avalable_mem_list;
    }

    public double getMaximal_cost() {
        return maximal_cost;
    }

    public void setMaximal_cost(double maximal_cost) {
        this.maximal_cost = maximal_cost;
    }

    public double getMinimal_cost() {
        return minimal_cost;
    }

    public void setMinimal_cost(double minimal_cost) {
        this.minimal_cost = minimal_cost;
    }

    public double getMinimal_avg_rt() {
        return minimal_avg_rt;
    }

    public void setMinimal_avg_rt(double minimal_avg_rt) {
        this.minimal_avg_rt = minimal_avg_rt;
    }

    public double getMaximal_avg_rt() {
        return maximal_avg_rt;
    }

    public void setMaximal_avg_rt(double maximal_avg_rt) {
        this.maximal_avg_rt = maximal_avg_rt;
    }

    public TreeMap<WVertex, Integer> getMinimal_mem_configuration() {
        return minimal_mem_configuration;
    }

    public void setMinimal_mem_configuration(TreeMap<WVertex, Integer> minimal_mem_configuration) {
        this.minimal_mem_configuration = minimal_mem_configuration;
    }

    public TreeMap<WVertex, Integer> getMaximal_mem_configuration() {
        return maximal_mem_configuration;
    }

    public void setMaximal_mem_configuration(TreeMap<WVertex, Integer> maximal_mem_configuration) {
        this.maximal_mem_configuration = maximal_mem_configuration;
    }

    public PerfOpt(ServerlessAppWorkflow Appworkflow, boolean generate_perf_profile, int[] mem_list) {
        this.App = Appworkflow;
        if (mem_list == null) {
            this.avalable_mem_list = new int[2817];
            for (int i = 0; i < avalable_mem_list.length; i++) {//初始化的可用内存大小  192MB~3008MB
                avalable_mem_list[i] = i + 192;
            }
        } else
            this.avalable_mem_list = mem_list;
        Random rand = new Random();
//        int seed = rand.nextInt(100);
//        this.appgenerator = new APPGenerator(seed, "4PL", this.avalable_mem_list);
        if (generate_perf_profile == true)
            this.generate_perf_profile();
        this.get_optimization_boundary();
    }

    public void generate_perf_profile() {  //生成mem-rt对应表
        WVertex[] node_list = this.App.getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for (int i = 0; i < node_list.length; i++) {
            String perf_profile_path = null;
            String jsonContent = null;
            try {
                perf_profile_path = new File("").getCanonicalPath() + "/src/main/resources/AWSLambda_functions_perf_profile/f" + (i + 1) + "_perf_profile.json";
                jsonContent = FileUtils.readFileToString(new File(perf_profile_path), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            JSONObject jsonObject = new JSONObject(jsonContent);

            for(int j=192;j<=3008;j++){
                if(j%64==0){
                    node_list[i].getPerf_profile().put(j,Double.valueOf(jsonObject.getInt(String.valueOf(j))));
                }else {
                    int left = (int)(j/64) * 64;
                    int right = left + 64;
                    int rLeft = jsonObject.getInt(String.valueOf(left));
                    int rRight = jsonObject.getInt(String.valueOf(right));
                    double linearFittingTime = (double)(j-left) / (right-left) * (rRight-rLeft) + rLeft;
                    node_list[i].getPerf_profile().put(j,linearFittingTime);
                }
            }
        }
//        this.appgenerator.get_rt_mem_data(this.App.G.node_num, node_list);
    }

    public void get_optimization_boundary() {
        Set<WVertex> node_list = this.App.getGraph().getDirectedGraph().vertexSet();
        this.minimal_mem_configuration = new TreeMap<WVertex, Integer>();
        this.maximal_mem_configuration = new TreeMap<WVertex, Integer>();
        for(WVertex aVertex : node_list){
            this.minimal_mem_configuration.put(aVertex, aVertex.getPerf_profile().firstKey());
            this.maximal_mem_configuration.put(aVertex, aVertex.getPerf_profile().lastKey());
        }

        this.update_App_workflow_mem_rt_cost(this.maximal_mem_configuration);
        this.maximal_cost = this.App.GetAverageCost();
        this.minimal_avg_rt = this.App.GetAverageRT();

        this.update_App_workflow_mem_rt_cost(this.minimal_mem_configuration);
        this.minimal_cost = this.App.GetAverageCost();
        this.maximal_avg_rt = this.App.GetAverageRT();
    }

    public void update_App_workflow_mem_rt_cost(TreeMap<WVertex, Integer> mem_dict) {
        // 更新函数内存时，同时更新每个结点的rt和cost
        Set<WVertex> vertexArr = mem_dict.keySet();
        for(WVertex aVertex : vertexArr){
            aVertex.setMem(mem_dict.get(aVertex));
            aVertex.setRt(aVertex.getPerf_profile().get(aVertex.getMem()));
            this.App.UpdateVertexCost(aVertex);
        }
    }
}
