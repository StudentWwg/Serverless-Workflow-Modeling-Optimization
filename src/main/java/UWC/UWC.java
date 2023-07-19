package UWC;

import PRCP.*;
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
                vertices.get(i).setUrgency(vertices.get(i).getVertex().getRt() * vertices.get(i).getProbabilityOfAllFunInEdges() /
                        vertices.get(i).getNumOfAllFunInEdges());
        } else if (type.equals("PCCO")) {
            for (int i = 0; i < this.vertices.size(); i++)
                vertices.get(i).setUrgency(vertices.get(i).getVertex().getCost() * vertices.get(i).getProbabilityOfAllFunInEdges() /
                        vertices.get(i).getNumOfAllFunInEdges());
        }
    }

    public void GetTotalProbabilityAndNumOfFunInEdgesOfAllVerticesByTopology() {
        DefaultDirectedWeightedGraph graph = this.perfOpt.getApp().getGraph().getDirectedGraph();
        WVertex[] wVertices = (WVertex[]) graph.vertexSet().toArray(new WVertex[0]);

        for (int i = 0; i < wVertices.length; i++) {
            UWCVertex aVertex = new UWCVertex(wVertices[i]);
            if (wVertices[i].equals(this.perfOpt.getApp().getGraph().getStart()) || wVertices[i].equals(this.perfOpt.getApp().getGraph().getEnd())) {
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
            Integer[] mem_list = node_list[i].getPerf_profile().keySet().toArray(new Integer[0]);
            Arrays.sort(mem_list);
            node_i.setAvailable_mem_list(Arrays.stream(mem_list).mapToInt(Integer::valueOf).toArray());
        }
    }

    public ERT_C_MEM_Config_iter UWC_BCPO(double budget, int iter) {
        long startTime = System.currentTimeMillis();
        update_available_mem_list();
        this.perfOpt.update_App_workflow_mem_rt_cost(this.perfOpt.getMinimal_mem_configuration());
        double cost = this.perfOpt.getCost_under_minimal_mem_configuration();
        double surplus = budget - cost;
        double current_avg_rt = this.perfOpt.getRT_under_minimal_mem_configuration();
        double current_cost = this.perfOpt.getCost_under_minimal_mem_configuration();
        updateUrgency("BCPO");
        SortBasedOnUrgency();
        WVertex[] verticesInGraph = new WVertex[this.vertices.size()];
        for (int i = 0; i < this.vertices.size(); i++) {
            verticesInGraph[i] = this.vertices.get(i).getVertex();
        }
        int iterations_count = 0;
        while ((new BigDecimal(surplus).setScale(4, RoundingMode.HALF_UP).doubleValue() >= 0)) {
            iterations_count += 1;
            TreeMap<WVertex, Mem_ERT_C> rt_m_ert_c_avg_rt_reduction_of_each_node = new TreeMap<WVertex, Mem_ERT_C>();

            for (int i = 0; i < verticesInGraph.length -  (verticesInGraph.length/3); i++) {
                WVertex node = verticesInGraph[i];
                TreeMap<Integer, ERT_C_RT_M> avg_rt_reduction_of_each_mem_config = new TreeMap<Integer, ERT_C_RT_M>();
                int[] mem_list_reversed = new int[node.getAvailable_mem_list().length];
                for (int j = 0; j < mem_list_reversed.length; j++)
                    mem_list_reversed[j] = node.getAvailable_mem_list()[mem_list_reversed.length - 1 - j];
                int primary_mem = node.getMem();
                for (int j = 0; j < mem_list_reversed.length; j++) {
                    if (mem_list_reversed[j] <= primary_mem) break;
                    TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();
                    mem_dict.put(node, mem_list_reversed[j]);
                    this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);
                    double increased_cost = this.perfOpt.getApp().GetAverageCost() - current_cost;
                    if (increased_cost < surplus) {
                        double rt_reduction = current_avg_rt - this.perfOpt.getApp().GetAverageRT();
                        if (rt_reduction > 0)
                            avg_rt_reduction_of_each_mem_config.put(mem_list_reversed[j], new ERT_C_RT_M(rt_reduction, increased_cost));
                    }
                }
                TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();
                mem_dict.put(node, Integer.valueOf(primary_mem));
                this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);

                if (avg_rt_reduction_of_each_mem_config.size() != 0) {
                    ArrayList<Double> v = new ArrayList<Double>();
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

            ArrayList<Double> v = new ArrayList<Double>();
            Mem_ERT_C[] values = rt_m_ert_c_avg_rt_reduction_of_each_node.values().toArray(new Mem_ERT_C[0]);
            for (int j = 0; j < values.length; j++)
                v.add(values[j].getRt());
            double max_rt_reduction = Collections.max(v);
            v.clear();
            for (int j = 0; j < values.length; j++)
                if (values[j].getRt() == max_rt_reduction)
                    v.add(values[j].getCost());
            double min_increased_cost_under_MAX_rt_reduction = Collections.min(v);
            int min_mem = 10241;
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

        TreeMap<WVertex, Integer> mem_backup = new TreeMap<WVertex, Integer>();
        WVertex[] vertexArr = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for (int i = 0; i < vertexArr.length; i++)
            mem_backup.put(vertexArr[i], vertexArr[i].getMem());
        StringBuffer sb = new StringBuffer("");
        WVertex[] vertices = mem_backup.keySet().toArray(new WVertex[0]);
        for (int j = 0; j < vertices.length; j++)
            sb.append(vertices[j].toString() + " : " + mem_backup.get(vertices[j]).toString() + " ");
        System.out.println("Repeated times:" + iter);
        System.out.println("Optimized Memory Configuration:" + sb);
        System.out.println("Budget Constraint: " + budget +" USD.");
        System.out.println("Average end-to-end response time: " + current_avg_rt + " ms.");
        System.out.println("Average Cost: " + current_cost +" USD.");
        System.out.println("Iteration Count: " + iterations_count);
        System.out.println("Time: " + (double) (endTime - startTime) / 1000 + " s");
        System.out.println("UWC_BCPO Optimization Completed.");
        return new ERT_C_MEM_Config_iter(budget, current_avg_rt, current_cost, mem_backup, iterations_count, 0);
    }

    public ERT_C_MEM_Config_iter UWC_PCCO(double performanceConstraint, int iter) {
        long startTime = System.currentTimeMillis();
        update_available_mem_list();
        this.perfOpt.update_App_workflow_mem_rt_cost(this.perfOpt.getMaximal_mem_configuration());
        double current_avg_rt = this.perfOpt.getRT_under_maximal_mem_configuration();
        double performance_surplus = performanceConstraint - current_avg_rt;
        double current_cost = this.perfOpt.getCost_under_maximal_mem_configuration();
        updateUrgency("PCCO");
        SortBasedOnUrgency();
        WVertex[] verticesInGraph = new WVertex[this.vertices.size()];
        for (int i = 0; i < this.vertices.size(); i++) {
            verticesInGraph[i] = this.vertices.get(i).getVertex();
        }
        int iterations_count = 0;
        while ((new BigDecimal(performance_surplus).setScale(4, RoundingMode.HALF_UP).doubleValue() >= 0) ) {
            iterations_count += 1;
            TreeMap<WVertex, Mem_ERT_C> rt_m_ert_c_cost_reduction_of_each_node = new TreeMap<WVertex, Mem_ERT_C>();

            for (int i = 0; i < verticesInGraph.length -  (verticesInGraph.length/3); i++) {
                WVertex node = verticesInGraph[i];
                TreeMap<Integer, ERT_C_RT_M> cost_reduction_of_each_mem_config = new TreeMap<Integer, ERT_C_RT_M>();
                int primary_mem = node.getMem();
                for (int j = 0; j < node.getAvailable_mem_list().length; j++) {
                    if (node.getAvailable_mem_list()[j] >= primary_mem)
                        break;
                    TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();
                    mem_dict.put(node, node.getAvailable_mem_list()[j]);
                    this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);
                    double increased_rt = this.perfOpt.getApp().GetAverageRT() - current_avg_rt;
                    double cost_reduction = current_cost - this.perfOpt.getApp().GetAverageCost();
                    if (increased_rt < performance_surplus && cost_reduction > 0)
                        cost_reduction_of_each_mem_config.put(node.getAvailable_mem_list()[j], new ERT_C_RT_M(increased_rt, cost_reduction));
                }
                TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();
                mem_dict.put(node, primary_mem);
                this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);

                if (cost_reduction_of_each_mem_config.size() != 0) {
                    ArrayList<Double> v = new ArrayList<>();
                    ERT_C_RT_M[] values = cost_reduction_of_each_mem_config.values().toArray(new ERT_C_RT_M[0]);
                    for (int j = 0; j < values.length; j++)
                        v.add(values[j].getCost());
                    double max_cost_reduction = Collections.max(v);
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
            ArrayList<Double> v = new ArrayList<>();
            Mem_ERT_C[] values = rt_m_ert_c_cost_reduction_of_each_node.values().toArray(new Mem_ERT_C[0]);
            for (int j = 0; j < values.length; j++)
                v.add(values[j].getCost());
            double max_cost_reduction = Collections.max(v);
            v.clear();
            for (int j = 0; j < values.length; j++)
                if (values[j].getCost() == max_cost_reduction)
                    v.add(values[j].getRt());
            double min_increased_rt_under_MAX_cost_reduction = Collections.min(v);
            int min_mem = 10241;
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

        TreeMap<WVertex, Integer> mem_backup = new TreeMap<WVertex, Integer>();
        WVertex[] vertexArr = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for (int i = 0; i < vertexArr.length; i++)
            mem_backup.put(vertexArr[i], vertexArr[i].getMem());
        StringBuffer sb = new StringBuffer("");
        WVertex[] vertices = mem_backup.keySet().toArray(new WVertex[0]);
        for (int j = 0; j < vertices.length; j++)
            sb.append(vertices[j].toString() + ":" + mem_backup.get(vertices[j]).toString() + "  ");
        System.out.println("Repeated times:" + iter);
        System.out.println("Optimized Memory Configuration: " + sb);
        System.out.println("Performance constraint: " + performanceConstraint +" ms.");
        System.out.println("Average end-to-end response time:" + current_avg_rt + " ms.");
        System.out.println("Average Cost:" + current_cost + " USD.");
        System.out.println("Time: " + (double) (endTime - startTime) / 1000 + " s");
        System.out.println("Iteration Count: " + iterations_count);
        System.out.println("UWC_PCCO Optimization Completed.");
        return new ERT_C_MEM_Config_iter(performanceConstraint, current_avg_rt, current_cost, mem_backup, iterations_count, 0);
    }

    public long UWC_OPT(double[] constraints, String OPTType, int iter) {
        ArrayList<ERT_C_MEM_Config_iter> results = new ArrayList<ERT_C_MEM_Config_iter>();
        long UWCStartTime = System.currentTimeMillis();
        for (double constraint : constraints) {
            ERT_C_MEM_Config_iter tempResult = null;
            if (OPTType.equals("BCPO")) {
                tempResult = this.UWC_BCPO(constraint, iter);
            } else if (OPTType.equals("PCCO")) {
                tempResult = this.UWC_PCCO(constraint, iter);
            }
            results.add(tempResult);
        }
        long UWCEndTime = System.currentTimeMillis();

        DataStoreTools.UWCDataStore(results, this.perfOpt.getApp().getGraph().getNode_num(), OPTType, iter);
        return (UWCEndTime - UWCStartTime) / constraints.length;
    }
}
