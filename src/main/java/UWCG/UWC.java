package UWCG;

import PRCPG.*;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import serverlessWorkflow.graph.WEdge;
import serverlessWorkflow.graph.WVertex;
import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;
import util.DataStoreTools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class UWC {
    private PerfOpt perfOpt;
    private ArrayList<UWCVertex> vertices = new ArrayList<UWCVertex>();

    public UWC(PerfOpt perfOpt) {
        this.perfOpt = perfOpt;
    }

    public void SortBasedOnUrgency() {
        Collections.sort(this.vertices, new Comparator<UWCVertex>() {
            @Override
            public int compare(UWCVertex o1, UWCVertex o2) {
                if (o1.getUrgency() > o2.getUrgency()) return -1;
                else if (o1.getUrgency() == o2.getUrgency()) return 0;
                else return 1;
            }
        });
    }

    public void updateUrgency(String type) {
        GetTotalProbabilityAndNumOfFunInEdgesOfAllVerticesByTopology();
        if (type.equals("BCPO")) {
            for (int i = 0; i < this.vertices.size(); i++)
                vertices.get(i).setUrgency(vertices.get(i).getRt() * vertices.get(i).getProbabilityOfAllFunInEdges() / vertices.get(i).getNumOfAllFunInEdges());
        } else if (type.equals("PCCO")) {
            for (int i = 0; i < this.vertices.size(); i++)
                vertices.get(i).setUrgency(vertices.get(i).getCost() * vertices.get(i).getProbabilityOfAllFunInEdges() / vertices.get(i).getNumOfAllFunInEdges());
        }
    }

    public void GetTotalProbabilityAndNumOfFunInEdgesOfAllVerticesByTopology() {
        DefaultDirectedWeightedGraph graph = this.perfOpt.getApp().getGraph().getDirectedGraph();
        WVertex[] wVertices = (WVertex[]) graph.vertexSet().toArray(new WVertex[0]);

        for (int i = 0; i < wVertices.length; i++) {
            UWCVertex aVertex = new UWCVertex(wVertices[i]);
            if (wVertices[i].equals(this.perfOpt.getApp().getGraph().getStart()) || wVertices[i].equals(this.perfOpt.getApp().getGraph().getEnd())) {  //起始顶点和最终顶点的urgency为1
                aVertex.setProbabilityOfAllFunInEdges(1);
                aVertex.setNumOfAllFunInEdges(1);
            } else {
                Set<WEdge> incomingEdges = graph.incomingEdgesOf(wVertices[i]);
                aVertex.setNumOfAllFunInEdges(incomingEdges.size());
                for (WEdge edge : incomingEdges)
                    aVertex.setProbabilityOfAllFunInEdges(aVertex.getProbabilityOfAllFunInEdges() + edge.getWeight());
            }
            this.vertices.add(aVertex);
        }
    }

    public void update_available_mem_list() {
        WVertex[] node_list = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for (int i = 0; i < node_list.length; i++) {
            WVertex node_i = node_list[i];
            Vector<Integer> available_mem_list = new Vector<Integer>();
            Integer[] mem_list = node_list[i].getPerf_profile().keySet().toArray(new Integer[0]);
            Arrays.sort(mem_list);  //将可用内存升序排序
            node_i.setAvailable_mem_list(Arrays.stream(mem_list).mapToInt(Integer::valueOf).toArray());
        }//for
    }

    public ERT_C_MEM_Config_iter UWC_BCPO(double budget) {
        long startTime = System.currentTimeMillis();
        update_available_mem_list();
        this.perfOpt.update_App_workflow_mem_rt_cost(this.perfOpt.getMinimal_mem_configuration()); //函数都更新为最小内存配置
        double cost = this.perfOpt.getMinimal_cost();
        double surplus = budget - cost; //剩余cost可增长空间
        double current_avg_rt = this.perfOpt.getMaximal_avg_rt();
        double current_cost = this.perfOpt.getMinimal_cost();
        updateUrgency("BCPO");
        SortBasedOnUrgency();
        WVertex[] verticesInGraph = new WVertex[this.vertices.size()];
        for(int i=0;i<this.vertices.size();i++){
            WVertex[] temp= this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
            for(int j=0;j< temp.length;j++)
                if(temp[j].getVertexInfo().equals(this.vertices.get(i).getVertexInfo())){
                    verticesInGraph[i] = temp[j];
                    break;
                }
        }
        int iterations_count = 0; //迭代次数
        while (new BigDecimal(surplus).setScale(4, RoundingMode.HALF_UP).doubleValue() >= 0 && iterations_count<=15) {
            iterations_count += 1;
            // key：node  value:(使得BCR,ΔERT,ΔC都最优的mem，最大ΔERT，最小ΔC)
            TreeMap<WVertex, Mem_ERT_C> rt_m_ert_c_avg_rt_reduction_of_each_node = new TreeMap<WVertex, Mem_ERT_C>();

            for (int i = 0; i < verticesInGraph.length; i++) {  // 根据urgency排序后的结点进行mem优化
                WVertex node = verticesInGraph[i];
                //avg_rt_reduction_of_each_mem_config是字典    key：mem   value：元组(rt_reduction, increased_cost)
                TreeMap<Integer, ERT_C_RT_M> avg_rt_reduction_of_each_mem_config = new TreeMap<Integer, ERT_C_RT_M>();   //保存每种mem下的 ΔERT 和 ΔC
                int[] mem_list_reversed = new int[node.getAvailable_mem_list().length];
                for (int j = 0; j < mem_list_reversed.length; j++)
                    mem_list_reversed[j] = node.getAvailable_mem_list()[mem_list_reversed.length - 1 - j];  //将可用mem逆转
                int primary_mem = node.getMem();  //更换结点mem配置前的mem
                for (int j = 0; j < mem_list_reversed.length; j++) {
                    if (mem_list_reversed[j] <= primary_mem) break;
                    TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();  //保存每个结点和对应的mem大小
                    mem_dict.put(node, mem_list_reversed[j]);
                    this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);
                    double temp_cost = this.perfOpt.getApp().GetAverageCost();
                    double increased_cost = this.perfOpt.getApp().GetAverageCost() - current_cost;  //计算更换了mem后的cost增量
                    if (increased_cost < surplus) {
                        double rt_reduction = current_avg_rt - this.perfOpt.getApp().GetAverageRT();
                        if (rt_reduction > 0)
                            avg_rt_reduction_of_each_mem_config.put(mem_list_reversed[j], new ERT_C_RT_M(rt_reduction, increased_cost));
                    }
                }
                TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();
                mem_dict.put(node, Integer.valueOf(primary_mem));
                this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict); //还原内存配置

                if (avg_rt_reduction_of_each_mem_config.size() != 0) {
                    Vector<Double> v = new Vector<Double>();
                    ERT_C_RT_M[] values = avg_rt_reduction_of_each_mem_config.values().toArray(new ERT_C_RT_M[0]);
                    for (int j = 0; j < values.length; j++)
                        v.add(values[j].getRt());
                    double max_rt_reduction = Collections.max(v);
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getRt() == max_rt_reduction)
                            v.add(values[j].getCost());
                    double min_increased_cost_under_MAX_rt_reduction = Collections.min(v);
                    Integer[] mems = avg_rt_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);
                    for (int j = 0; j < mems.length; j++) {
                        ERT_C_RT_M get_mem = avg_rt_reduction_of_each_mem_config.get(mems[j]);
                        if (get_mem.getRt() == max_rt_reduction && get_mem.getCost() == min_increased_cost_under_MAX_rt_reduction) {
                            rt_m_ert_c_avg_rt_reduction_of_each_node.put(node, new Mem_ERT_C(mems[j], max_rt_reduction, min_increased_cost_under_MAX_rt_reduction));
                        }
                    }
                }
            }

            if (rt_m_ert_c_avg_rt_reduction_of_each_node.size() == 0)
                break;

            WVertex target_node = null;
            int target_mem = 0;

            Vector<Double> v = new Vector<Double>();
            Mem_ERT_C[] values = rt_m_ert_c_avg_rt_reduction_of_each_node.values().toArray(new Mem_ERT_C[0]);
            for (int j = 0; j < values.length; j++)
                v.add(values[j].getRt());
            double max_rt_reduction = Collections.max(v);  //获取最大ΔERT
            v.clear();
            for (int j = 0; j < values.length; j++)
                if (values[j].getRt() == max_rt_reduction)
                    v.add(values[j].getCost());
            double min_increased_cost_under_MAX_rt_reduction = Collections.min(v);  //获取最小ΔC
            Vector<Integer> vm = new Vector<Integer>();
            int min_mem = 3009; // 大于最大可用内存3008MB
            WVertex[] vertices = rt_m_ert_c_avg_rt_reduction_of_each_node.keySet().toArray(new WVertex[0]);
            for (int j = 0; j < vertices.length; j++) {
                Mem_ERT_C value = rt_m_ert_c_avg_rt_reduction_of_each_node.get(vertices[j]);
                if (value.getRt() == max_rt_reduction && value.getCost() == min_increased_cost_under_MAX_rt_reduction)
                    if (value.getMem() < min_mem) {
                        min_mem = value.getMem();
                        target_node = vertices[j];
                        target_mem = value.getMem();
                    }
            }

            //每次只更新一个结点
            TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();
            mem_dict.put(target_node, target_mem);
            this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);
            max_rt_reduction = rt_m_ert_c_avg_rt_reduction_of_each_node.get(target_node).getRt();
            min_increased_cost_under_MAX_rt_reduction = rt_m_ert_c_avg_rt_reduction_of_each_node.get(target_node).getCost();
            current_avg_rt = current_avg_rt - max_rt_reduction;
            surplus = surplus - min_increased_cost_under_MAX_rt_reduction;
            current_cost = current_cost + min_increased_cost_under_MAX_rt_reduction;
        }
        long endTime = System.currentTimeMillis();

        TreeMap<WVertex, Integer> mem_backup = new TreeMap<WVertex, Integer>();  //每个结点的当前内存配置信息
        WVertex[] vertexArr = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for (int i = 0; i < vertexArr.length; i++)
            mem_backup.put(vertexArr[i], vertexArr[i].getMem());
        StringBuffer sb = new StringBuffer("");
        WVertex[] vertices = mem_backup.keySet().toArray(new WVertex[0]);
        for (int j = 0; j < vertices.length; j++)
            sb.append(vertices[j].toString() + " : " + mem_backup.get(vertices[j]).toString() + " ");
        System.out.println("Optimized Memory Configuration:" + sb);
        System.out.println("Budget Constraint: " + budget);
        System.out.println("Average end-to-end response time: " + current_avg_rt);
        System.out.println("Average Cost: " + current_cost);
        System.out.println("Iteration Count: " + iterations_count);
        System.out.println("Time: " + (double)(endTime-startTime)/1000 + " s");
        System.out.println("UWC_BCPO Optimization Completed.");
        return new ERT_C_MEM_Config_iter(budget, current_avg_rt, current_cost, mem_backup, iterations_count, 0);
    }

    public ERT_C_MEM_Config_iter UWC_PCCO(double performanceConstraint) {
        long startTime = System.currentTimeMillis();
        update_available_mem_list();
        this.perfOpt.update_App_workflow_mem_rt_cost(this.perfOpt.getMaximal_mem_configuration());
        double current_avg_rt = this.perfOpt.getMinimal_avg_rt();
        double performance_surplus = performanceConstraint - current_avg_rt;
        double current_cost = this.perfOpt.getMaximal_cost();
        updateUrgency("PCCO");
        SortBasedOnUrgency();
        WVertex[] verticesInGraph = new WVertex[this.vertices.size()];
        for(int i=0;i<this.vertices.size();i++){
            WVertex[] temp= this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
            for(int j=0;j< temp.length;j++)
                if(temp[j].getVertexInfo().equals(this.vertices.get(i).getVertexInfo())){
                    verticesInGraph[i] = temp[j];
                    break;
                }
        }
        int iterations_count = 0; //迭代次数
        while (new BigDecimal(performance_surplus).setScale(4, RoundingMode.HALF_UP).doubleValue() >= 0 && iterations_count<=15) {
            iterations_count += 1;
            // key：node  value:(使得BCR,ΔERT,ΔC都最优的mem，最大ΔERT，最小ΔC)
            TreeMap<WVertex, Mem_ERT_C> rt_m_ert_c_cost_reduction_of_each_node = new TreeMap<WVertex, Mem_ERT_C>();

            for (int i = 0; i < verticesInGraph.length; i++) {
                WVertex node = verticesInGraph[i];
                //key：mem   value：元组(rt_reduction, increased_cost)
                TreeMap<Integer, ERT_C_RT_M> cost_reduction_of_each_mem_config = new TreeMap<Integer, ERT_C_RT_M>();   //保存每种mem下的 ΔERT 和 ΔC  此变量用于ERT/C和RT/M策略
                int primary_mem = node.getMem();  //更换结点mem配置前的mem大小
                for (int j = 0; j < node.getAvailable_mem_list().length; j++) {
                    if (node.getAvailable_mem_list()[j] >= primary_mem)
                        break;
                    TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();  //保存每个结点和对应的mem大小
                    mem_dict.put(node, node.getAvailable_mem_list()[j]);
                    this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);
                    double increased_rt = this.perfOpt.getApp().GetAverageRT() - current_avg_rt;  //计算更换了mem后的cost增量
                    double cost_reduction = current_cost - this.perfOpt.getApp().GetAverageCost();
                    if (increased_rt < performance_surplus && cost_reduction > 0)
                        cost_reduction_of_each_mem_config.put(node.getAvailable_mem_list()[j], new ERT_C_RT_M(increased_rt, cost_reduction));
                }
                TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();
                mem_dict.put(node, primary_mem);
                this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);  //还原内存配置

                if (cost_reduction_of_each_mem_config.size() != 0) {
                    Vector<Double> v = new Vector<Double>();
                    ERT_C_RT_M[] values = cost_reduction_of_each_mem_config.values().toArray(new ERT_C_RT_M[0]);
                    for (int j = 0; j < values.length; j++)
                        v.add(values[j].getCost());
                    double max_cost_reduction = Collections.max(v);  //获取最大ΔC
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getCost() == max_cost_reduction)
                            v.add(values[j].getRt());
                    double min_increased_rt_under_MAX_cost_reduction = Collections.min(v);
                    Integer[] mems = cost_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);
                    for (int j = 0; j < mems.length; j++) {
                        ERT_C_RT_M get_mem = cost_reduction_of_each_mem_config.get(mems[j]);
                        if (get_mem.getCost() == max_cost_reduction && get_mem.getRt() == min_increased_rt_under_MAX_cost_reduction) {
                            rt_m_ert_c_cost_reduction_of_each_node.put(node, new Mem_ERT_C(mems[j], min_increased_rt_under_MAX_cost_reduction, max_cost_reduction));
                        }
                    }
                }
            }

            if (rt_m_ert_c_cost_reduction_of_each_node.size() == 0)
                break;

            WVertex target_node = null;
            int target_mem = 0;
            Vector<Double> v = new Vector<Double>();
            Mem_ERT_C[] values = rt_m_ert_c_cost_reduction_of_each_node.values().toArray(new Mem_ERT_C[0]);
            for (int j = 0; j < values.length; j++)
                v.add(values[j].getCost());
            double max_cost_reduction = Collections.max(v);  //获取最大ΔC
            v.clear();
            for (int j = 0; j < values.length; j++)
                if (values[j].getCost() == max_cost_reduction)
                    v.add(values[j].getRt());
            double min_increased_rt_under_MAX_cost_reduction = Collections.min(v);  //获取最小ΔERT
            Vector<Integer> vm = new Vector<Integer>();
            int min_mem = 3009;  // 大于最大可用内存
            WVertex[] vertices = rt_m_ert_c_cost_reduction_of_each_node.keySet().toArray(new WVertex[0]);
            for (int j = 0; j < vertices.length; j++) {
                Mem_ERT_C value = rt_m_ert_c_cost_reduction_of_each_node.get(vertices[j]);
                if (value.getCost() == max_cost_reduction && value.getRt() == min_increased_rt_under_MAX_cost_reduction)
                    if (value.getMem() < min_mem) {
                        min_mem = value.getMem();
                        target_node = vertices[j];
                        target_mem = value.getMem();
                    }
            }

            TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();
            mem_dict.put(target_node, target_mem);
            this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);
            max_cost_reduction = rt_m_ert_c_cost_reduction_of_each_node.get(target_node).getCost();
            min_increased_rt_under_MAX_cost_reduction = rt_m_ert_c_cost_reduction_of_each_node.get(target_node).getRt();
            current_cost = current_cost - max_cost_reduction;
            current_avg_rt = current_avg_rt + min_increased_rt_under_MAX_cost_reduction;
            performance_surplus = performance_surplus - min_increased_rt_under_MAX_cost_reduction;
        }
        long endTime = System.currentTimeMillis();

        TreeMap<WVertex, Integer> mem_backup = new TreeMap<WVertex, Integer>();  //每个结点的当前内存配置信息
        WVertex[] vertexArr = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for (int i = 0; i < vertexArr.length; i++)
            mem_backup.put(vertexArr[i], vertexArr[i].getMem());
        StringBuffer sb = new StringBuffer("");
        WVertex[] vertices = mem_backup.keySet().toArray(new WVertex[0]);
        for (int j = 0; j < vertices.length; j++)
            sb.append(vertices[j].toString() + ":" + mem_backup.get(vertices[j]).toString() + "  ");
        System.out.println("Optimized Memory Configuration: " + sb);
        System.out.println("Performance constraint: " + performanceConstraint);
        System.out.println("Average end-to-end response time:" + current_avg_rt);
        System.out.println("Average Cost:" + current_cost);
        System.out.println("Time: " + (double)(endTime-startTime)/1000 + " s");
        System.out.println("Iteration Count: " + iterations_count);
        System.out.println("UWC_PCCO Optimization Completed.");
        return new ERT_C_MEM_Config_iter(performanceConstraint, current_avg_rt, current_cost, mem_backup, iterations_count, 0);
    }

    public long UWC_OPT(double[] constraints, String OPTType, int repeatedTimes) {
        ArrayList<ERT_C_MEM_Config_iter> results = new ArrayList<ERT_C_MEM_Config_iter>();
        long UWCStartTime = System.currentTimeMillis();
        for (double constraint : constraints) {   //constraint为budgetConstraint或performanceConstraint
            ERT_C_MEM_Config_iter tempResults = null;
            if (OPTType.equals("BCPO")) {
                tempResults = this.UWC_BCPO(constraint);
            } else if (OPTType.equals("PCCO")) {
                tempResults = this.UWC_PCCO(constraint);
            }
            results.add(tempResults);
        }
        long UWCEndTime = System.currentTimeMillis();

        DataStoreTools.UWCDataStore(results, this.perfOpt.getApp().getGraph().getNode_num(), OPTType, repeatedTimes);
        return (UWCEndTime-UWCStartTime) / constraints.length;
    }
}
