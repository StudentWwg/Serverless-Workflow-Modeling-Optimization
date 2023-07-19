package PRCP;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import serverlessWorkflow.graph.WEdge;
import serverlessWorkflow.graph.WVertex;
import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;
import util.LeastSquareMethod;
import util.DataStoreTools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class PRCP {
    public PerfOpt perfOpt;

    class Tuple implements Comparable {
        double prrt;
        int index;

        public Tuple(double prrt, int index) {
            this.prrt = prrt;
            this.index = index;
        }

        @Override
        public int compareTo(Object o) {
            Tuple t = (Tuple) o;
            if (this.prrt > t.prrt) return -1;
            else if (this.prrt == t.prrt) return 0;
            else return 1;
        }
    }

    public Tuple[] arr;

    public PRCP(PerfOpt perfOpt) {
        this.perfOpt = perfOpt;
        this.UpdateBCR();
    }

    public void UpdateBCR() {
        WVertex[] vertices = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for (int i = 0; i < vertices.length; i++) {
            Integer[] available_mem_list = vertices[i].getPerf_profile().keySet().toArray(new Integer[0]);
            Double[] available_rt_list = vertices[i].getPerf_profile().values().toArray(new Double[0]);
            double[] x = new double[available_mem_list.length];
            double[] y = new double[available_rt_list.length];
            for (int j = 0; j < available_mem_list.length; j++) {
                x[j] = available_mem_list[j];
                y[j] = available_rt_list[j];
            }
            LeastSquareMethod eastSquareMethod = new LeastSquareMethod(x, y, 2);
            double[] coefficients = eastSquareMethod.getCoefficient();
            vertices[i].setBCR(Math.abs(coefficients[1]));
        }
    }

    public void update_available_mem_list(boolean BCR, double BCRthreshold, boolean BCRinverse) {
        WVertex[] node_list = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for (int i = 0; i < node_list.length; i++) {
            WVertex node_i = node_list[i];
            ArrayList<Integer> available_mem_list = new ArrayList<>();
            if (BCR) {
                Integer[] mem_list = node_list[i].getPerf_profile().keySet().toArray(new Integer[0]);
                Arrays.sort(mem_list);
                if (BCRinverse) {
                    for (int j = 0; j < mem_list.length - 1; j++) {
                        if (Math.abs((mem_list[j + 1] - mem_list[j]) / (node_i.getPerf_profile().get(mem_list[j + 1]) - node_i.getPerf_profile().get(mem_list[j]))) >
                                1.0 / node_i.getBCR() * BCRthreshold) {
                            available_mem_list.add(mem_list[j]);
                        }
                    }
                    available_mem_list.add(mem_list[mem_list.length - 1]);
                } else {
                    for (int j = 0; j < mem_list.length - 1; j++) {
                        if (Math.abs((node_i.getPerf_profile().get(mem_list[j + 1]) - node_i.getPerf_profile().get(mem_list[j])) / (mem_list[j + 1] - mem_list[j])) >
                                node_i.getBCR() * BCRthreshold) {
                            available_mem_list.add(mem_list[j]);
                        }
                    }
                    available_mem_list.add(mem_list[mem_list.length - 1]);
                }

                node_i.setAvailable_mem_list(Arrays.stream(available_mem_list.toArray(new Integer[0])).mapToInt(Integer::valueOf).toArray());
            } else {
                Integer[] mem_list = node_list[i].getPerf_profile().keySet().toArray(new Integer[0]);
                Arrays.sort(mem_list);
                node_i.setAvailable_mem_list(Arrays.stream(mem_list).mapToInt(Integer::valueOf).toArray());
            }
        }
    }

    public GraphPath<WVertex, WEdge> find_PRCP(int order, boolean leastCritical) {
        AllDirectedPaths allDirectedPaths = new AllDirectedPaths(this.perfOpt.getApp().getGraph().getDirectedGraph());
        List<GraphPath<WVertex, WEdge>> graphPathList = allDirectedPaths.getAllPaths(this.perfOpt.getApp().getGraph().getStart(),
                this.perfOpt.getApp().getGraph().getEnd(), true, 10000);
        if (this.arr == null) {
            double[] tp_list = new double[graphPathList.size()];
            double[] rt_list = new double[graphPathList.size()];
            double[] prrt_list = new double[graphPathList.size()];
            for (int i = 0; i < graphPathList.size(); i++) {
                tp_list[i] = this.perfOpt.getApp().GetTPOfAPath(graphPathList.get(i));
                rt_list[i] = this.perfOpt.getApp().GetRTOfAPath(graphPathList.get(i));
                prrt_list[i] = tp_list[i] * rt_list[i];
            }
            this.arr = new Tuple[prrt_list.length];
            for (int i = 0; i < prrt_list.length; i++)
                arr[i] = new Tuple(prrt_list[i], i);
            Arrays.sort(arr);
        }
        int indexOfPRPC = arr[order].index;
        return graphPathList.get(indexOfPRPC);
    }

    public ERT_C_MEM_Config_iter PRCPG_BCPO(double budget, boolean BCR, String BCRtype, double BCRthreshold, int iter) {
        long startTime = System.currentTimeMillis();
        if (BCR && BCRtype.equals("rt-mem"))
            BCRtype = "RT/M";
        else if (BCR && BCRtype.equals("e2ert-cost"))
            BCRtype = "ERT/C";
        else if (BCR && BCRtype.equals("max"))
            BCRtype = "MAX";

        if (BCR && BCRtype.equals("RT/M"))
            this.update_available_mem_list(true, BCRthreshold, false);
        else
            this.update_available_mem_list(false, BCRthreshold, false);

        this.perfOpt.update_App_workflow_mem_rt_cost(this.perfOpt.getMinimal_mem_configuration());
        double cost = this.perfOpt.getCost_under_minimal_mem_configuration();
        double surplus = budget - cost;
        double current_avg_rt = this.perfOpt.getRT_under_minimal_mem_configuration();
        double current_cost = this.perfOpt.getCost_under_minimal_mem_configuration();
        double last_e2ert_cost_BCR = 0;
        int order = 0;
        int iterations_count = 0;

        long loopTime = System.currentTimeMillis();
        while ((new BigDecimal(surplus).setScale(4, RoundingMode.HALF_UP).doubleValue() >= 0) && (loopTime - startTime <= 100000)) {
            iterations_count += 1;
            GraphPath<WVertex, WEdge> cp = this.find_PRCP(order, false);
            HashMap<WVertex, Mem_ERT_C_BCR_MAX> max_avg_rt_reduction_of_each_node = new HashMap<>();
            HashMap<WVertex, Mem_ERT_C> rt_m_ert_c_avg_rt_reduction_of_each_node = new HashMap<WVertex, Mem_ERT_C>();
            List<WVertex> nodes = cp.getVertexList();
            for (int i = 0; i < nodes.size(); i++) {
                WVertex node = nodes.get(i);
                HashMap<Integer, ERT_C_RT_M> avg_rt_reduction_of_each_mem_config = new HashMap<Integer, ERT_C_RT_M>();
                HashMap<Integer, ERT_C_BCR_MAX> avg_rt_reduction_of_each_mem_config_max = new HashMap<Integer, ERT_C_BCR_MAX>();
                int[] mem_list_reversed = new int[node.getAvailable_mem_list().length];
                for (int j = 0; j < mem_list_reversed.length; j++)
                    mem_list_reversed[j] = node.getAvailable_mem_list()[mem_list_reversed.length - 1 - j];
                int primary_mem = node.getMem();
                for (int j = 0; j < mem_list_reversed.length; j++) {
                    if (mem_list_reversed[j] <= primary_mem) break;
                    TreeMap<WVertex, Integer> mem_dict = new TreeMap<>();
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

                if (BCR && BCRtype.equals("ERT/C")) {
                    Integer[] mems = avg_rt_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);
                    HashMap<Integer, ERT_C_RT_M> temp = new HashMap<>();
                    for (int j = 0; j < mems.length; j++)
                        if (avg_rt_reduction_of_each_mem_config.get(mems[j]).getRt() / avg_rt_reduction_of_each_mem_config.get(mems[j]).getCost() >
                                last_e2ert_cost_BCR * BCRthreshold)
                            temp.put(mems[j], avg_rt_reduction_of_each_mem_config.get(mems[j]));
                    avg_rt_reduction_of_each_mem_config = temp;
                } else if (BCR && BCRtype.equals("MAX")) {
                    Integer[] mems = avg_rt_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);
                    for (int j = 0; j < mems.length; j++) {
                        double ERT = avg_rt_reduction_of_each_mem_config.get(mems[j]).getRt();
                        double C = avg_rt_reduction_of_each_mem_config.get(mems[j]).getCost();
                        avg_rt_reduction_of_each_mem_config_max.put(mems[j], new ERT_C_BCR_MAX(ERT, C, ERT / C));
                    }
                }

                if (BCR && BCRtype.equals("MAX") && avg_rt_reduction_of_each_mem_config_max.size() != 0) {
                    ArrayList<Double> v = new ArrayList<>();
                    ERT_C_BCR_MAX[] values = avg_rt_reduction_of_each_mem_config_max.values().toArray(new ERT_C_BCR_MAX[0]);
                    for (int j = 0; j < values.length; j++)
                        v.add(values[j].getBCR());
                    double max_BCR = Collections.max(v);
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getBCR() == max_BCR)
                            v.add(values[j].getRt());
                    double max_rt_reduction_under_MAX_BCR = Collections.max(v);
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getBCR() == max_BCR && values[j].getRt() == max_rt_reduction_under_MAX_BCR)
                            v.add(values[j].getCost());
                    double min_increased_cost_under_MAX_rt_reduction_MAX_BCR = Collections.min(v);
                    Integer[] mems = avg_rt_reduction_of_each_mem_config_max.keySet().toArray(new Integer[0]);
                    for (int j = 0; j < mems.length; j++) {
                        ERT_C_BCR_MAX get_mem = avg_rt_reduction_of_each_mem_config_max.get(mems[j]);
                        if (get_mem.getBCR() == max_BCR && get_mem.getRt() == max_rt_reduction_under_MAX_BCR &&
                                get_mem.getCost() == min_increased_cost_under_MAX_rt_reduction_MAX_BCR) {
                            max_avg_rt_reduction_of_each_node.put(node, new Mem_ERT_C_BCR_MAX(mems[j], max_rt_reduction_under_MAX_BCR,
                                    min_increased_cost_under_MAX_rt_reduction_MAX_BCR, max_BCR));
                        }
                    }
                } else if (avg_rt_reduction_of_each_mem_config.size() != 0) {
                    ArrayList<Double> v = new ArrayList<>();
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

            if ((BCR && BCRtype.equals("MAX") && max_avg_rt_reduction_of_each_node.size() == 0) ||
                    ((BCR == false || (BCR && (BCRtype.equals("ERT/C") || BCRtype.equals("RT/M")))) && rt_m_ert_c_avg_rt_reduction_of_each_node.size() == 0)) {
                if (order >= new AllDirectedPaths<WVertex, WEdge>(this.perfOpt.getApp().getGraph().
                        getDirectedGraph()).getAllPaths(this.perfOpt.getApp().getGraph().getStart(), this.perfOpt.getApp().getGraph().getEnd(), true, 10000).size() - 1)
                    break;
                else {
                    order += 1;
                    continue;
                }
            }

            WVertex target_node = null;
            int target_mem = 0;
            if (BCR && BCRtype.equals("MAX")) {
                ArrayList<Double> v = new ArrayList<>();
                Mem_ERT_C_BCR_MAX[] values = max_avg_rt_reduction_of_each_node.values().toArray(new Mem_ERT_C_BCR_MAX[0]);
                for (int j = 0; j < values.length; j++)
                    v.add(values[j].getBCR());
                double max_BCR = Collections.max(v);
                v.clear();
                for (int j = 0; j < values.length; j++)
                    if (values[j].getBCR() == max_BCR)
                        v.add(values[j].getRt());
                double max_rt_reduction_under_MAX_BCR = Collections.max(v);
                WVertex[] vertices = max_avg_rt_reduction_of_each_node.keySet().toArray(new WVertex[0]);
                for (int j = 0; j < vertices.length; j++) {
                    Mem_ERT_C_BCR_MAX value = max_avg_rt_reduction_of_each_node.get(vertices[j]);
                    if (value.getBCR() == max_BCR && value.getRt() == max_rt_reduction_under_MAX_BCR) {
                        target_node = vertices[j];
                        target_mem = value.getMem();
                        break;
                    }
                }
            } else {
                ArrayList<Double> v = new ArrayList<>();
                Mem_ERT_C[] values = rt_m_ert_c_avg_rt_reduction_of_each_node.values().toArray(new Mem_ERT_C[0]);
                for (int j = 0; j < values.length; j++)
                    v.add(values[j].getRt());
                double max_rt_reduction = Collections.max(v);
                v.clear();
                for (int j = 0; j < values.length; j++)
                    if (values[j].getRt() == max_rt_reduction)
                        v.add(values[j].getCost());
                double min_increased_cost_under_MAX_rt_reduction = Collections.min(v);
                ArrayList<Integer> vm = new ArrayList<>();
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
            }

            TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();
            mem_dict.put(target_node, target_mem);
            this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);
            double max_rt_reduction = 0, min_increased_cost_under_MAX_rt_reduction = 0;
            if (BCR && BCRtype.equals("MAX")) {
                max_rt_reduction = max_avg_rt_reduction_of_each_node.get(target_node).getRt();
                min_increased_cost_under_MAX_rt_reduction = max_avg_rt_reduction_of_each_node.get(target_node).getCost();
            } else {
                max_rt_reduction = rt_m_ert_c_avg_rt_reduction_of_each_node.get(target_node).getRt();
                min_increased_cost_under_MAX_rt_reduction = rt_m_ert_c_avg_rt_reduction_of_each_node.get(target_node).getCost();
            }
            current_avg_rt = current_avg_rt - max_rt_reduction;
            surplus = surplus - min_increased_cost_under_MAX_rt_reduction;
            current_cost = current_cost + min_increased_cost_under_MAX_rt_reduction;
            double current_e2ert_cost_BCR = Math.abs(max_rt_reduction / min_increased_cost_under_MAX_rt_reduction);
            if (current_e2ert_cost_BCR == Double.POSITIVE_INFINITY)
                last_e2ert_cost_BCR = 0;
            else
                last_e2ert_cost_BCR = current_e2ert_cost_BCR;

            loopTime = System.currentTimeMillis();
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
        System.out.println("Repeated times: " + iter);
        System.out.println("Optimized Memory Configuration: " + sb);
        System.out.println("Budget Constraint: " + budget + " USD.");
        System.out.println("Average end-to-end response time: " + current_avg_rt + " ms.");
        System.out.println("Average Cost: " + current_cost + " USD.");
        System.out.println("Iteration Count: " + iterations_count);
        System.out.println("Time: " + (double) (endTime - startTime) / 1000 + " s");
        System.out.println("PRCP_BCPO Optimization Completed.");
        return new ERT_C_MEM_Config_iter(budget, current_avg_rt, current_cost, mem_backup, iterations_count, BCRthreshold);
    }

    public ERT_C_MEM_Config_iter PRCPG_PCCO(double performanceConstraint, boolean BCR, String BCRtype, double BCRthreshold, int iter) {
        long startTime = System.currentTimeMillis();
        if (BCR && BCRtype.equals("rt-mem"))
            BCRtype = "M/RT";
        else if (BCR && BCRtype.equals("e2ert-cost"))
            BCRtype = "C/ERT";
        else if (BCR && BCRtype.equals("max"))
            BCRtype = "MAX";

        if (BCR && BCRtype.equals("M/RT"))
            this.update_available_mem_list(true, BCRthreshold, true);
        else
            this.update_available_mem_list(false, BCRthreshold, true);

        this.perfOpt.update_App_workflow_mem_rt_cost(this.perfOpt.getMaximal_mem_configuration());
        double current_avg_rt = this.perfOpt.getRT_under_maximal_mem_configuration();
        double performance_surplus = performanceConstraint - current_avg_rt;
        double current_cost = this.perfOpt.getCost_under_maximal_mem_configuration();
        double last_e2ert_cost_BCR = 0;
        int order = 0;
        int iterations_count = 0;

        long loopTime = System.currentTimeMillis();
        while ((new BigDecimal(performance_surplus).setScale(4, RoundingMode.HALF_UP).doubleValue() >= 0) && loopTime - startTime <= 100000) {
            iterations_count += 1;
            GraphPath<WVertex, WEdge> cp = this.find_PRCP(order, true);
            HashMap<WVertex, Mem_ERT_C_BCR_MAX> max_cost_reduction_of_each_node = new HashMap<WVertex, Mem_ERT_C_BCR_MAX>();
            HashMap<WVertex, Mem_ERT_C> rt_m_ert_c_cost_reduction_of_each_node = new HashMap<WVertex, Mem_ERT_C>();
            List<WVertex> nodes = cp.getVertexList();
            for (int i = 0; i < nodes.size(); i++) {
                WVertex node = nodes.get(i);
                HashMap<Integer, ERT_C_RT_M> cost_reduction_of_each_mem_config = new HashMap<Integer, ERT_C_RT_M>();
                HashMap<Integer, ERT_C_BCR_MAX> cost_reduction_of_each_mem_config_max = new HashMap<Integer, ERT_C_BCR_MAX>();
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

                if (BCR && BCRtype.equals("C/ERT")) {
                    Integer[] mems = cost_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);
                    HashMap<Integer, ERT_C_RT_M> temp = new HashMap<>();
                    for (int j = 0; j < mems.length; j++)
                        if (cost_reduction_of_each_mem_config.get(mems[j]).getCost() / cost_reduction_of_each_mem_config.get(mems[j]).getRt() >
                                last_e2ert_cost_BCR * BCRthreshold)
                            temp.put(mems[j], cost_reduction_of_each_mem_config.get(mems[j]));
                    cost_reduction_of_each_mem_config = temp;
                } else if (BCR && BCRtype.equals("MAX")) {
                    Integer[] mems = cost_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);
                    for (int j = 0; j < mems.length; j++) {
                        double ERT = cost_reduction_of_each_mem_config.get(mems[j]).getRt();
                        double C = cost_reduction_of_each_mem_config.get(mems[j]).getCost();
                        cost_reduction_of_each_mem_config_max.put(mems[j], new ERT_C_BCR_MAX(ERT, C, C / ERT));
                    }
                }

                if (BCR && BCRtype.equals("MAX") && cost_reduction_of_each_mem_config_max.size() != 0) {
                    ArrayList<Double> v = new ArrayList<Double>();
                    ERT_C_BCR_MAX[] values = cost_reduction_of_each_mem_config_max.values().toArray(new ERT_C_BCR_MAX[0]);
                    for (int j = 0; j < values.length; j++)
                        v.add(values[j].getBCR());
                    double max_BCR = Collections.max(v);
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getBCR() == max_BCR)
                            v.add(values[j].getCost());
                    double max_cost_reduction_under_MAX_BCR = Collections.max(v);  //
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getBCR() == max_BCR && values[j].getCost() == max_cost_reduction_under_MAX_BCR)
                            v.add(values[j].getRt());
                    double min_increased_rt_under_MAX_rt_reduction_MAX_BCR = Collections.min(v);  //
                    Integer[] mems = cost_reduction_of_each_mem_config_max.keySet().toArray(new Integer[0]);
                    for (int j = 0; j < mems.length; j++) {
                        ERT_C_BCR_MAX get_mem = cost_reduction_of_each_mem_config_max.get(mems[j]);
                        if (get_mem.getBCR() == max_BCR && get_mem.getCost() == max_cost_reduction_under_MAX_BCR &&
                                get_mem.getRt() == min_increased_rt_under_MAX_rt_reduction_MAX_BCR) {
                            max_cost_reduction_of_each_node.put(node, new Mem_ERT_C_BCR_MAX(mems[j], min_increased_rt_under_MAX_rt_reduction_MAX_BCR,
                                    max_cost_reduction_under_MAX_BCR, max_BCR));
                        }
                    }
                } else if (cost_reduction_of_each_mem_config.size() != 0) {
                    ArrayList<Double> v = new ArrayList<Double>();
                    ERT_C_RT_M[] values = cost_reduction_of_each_mem_config.values().toArray(new ERT_C_RT_M[0]);
                    for (int j = 0; j < values.length; j++)
                        v.add(values[j].getCost());
                    double max_cost_reduction = Collections.max(v);  //
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getCost() == max_cost_reduction)
                            v.add(values[j].getRt());
                    double min_increased_rt_under_MAX_cost_reduction = Collections.min(v); //
                    Integer[] mems = cost_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);
                    for (int j = 0; j < mems.length; j++) {
                        ERT_C_RT_M get_mem = cost_reduction_of_each_mem_config.get(mems[j]);
                        if (get_mem.getCost() == max_cost_reduction && get_mem.getRt() == min_increased_rt_under_MAX_cost_reduction) {
                            rt_m_ert_c_cost_reduction_of_each_node.put(node, new Mem_ERT_C(mems[j], min_increased_rt_under_MAX_cost_reduction, max_cost_reduction));
                        }
                    }
                }
            }

            if ((BCR && BCRtype.equals("MAX") && max_cost_reduction_of_each_node.size() == 0) ||
                    ((BCR == false || (BCR && (BCRtype.equals("M/RT") || BCRtype.equals("C/ERT")))) && rt_m_ert_c_cost_reduction_of_each_node.size() == 0)) {
                if (order >= new AllDirectedPaths<WVertex, WEdge>(this.perfOpt.getApp().getGraph().getDirectedGraph()).
                        getAllPaths(this.perfOpt.getApp().getGraph().getStart(), this.perfOpt.getApp().getGraph().getEnd(), true, 10000).size() - 1)
                    break;
                else {
                    order += 1;
                    continue;
                }
            }

            WVertex target_node = null;
            int target_mem = 0;
            if (BCR && BCRtype.equals("MAX")) {
                ArrayList<Double> v = new ArrayList<Double>();
                Mem_ERT_C_BCR_MAX[] values = max_cost_reduction_of_each_node.values().toArray(new Mem_ERT_C_BCR_MAX[0]);
                for (int j = 0; j < values.length; j++)
                    v.add(values[j].getBCR());
                double max_BCR = Collections.max(v);
                v.clear();
                for (int j = 0; j < values.length; j++)
                    if (values[j].getBCR() == max_BCR)
                        v.add(values[j].getCost());
                double max_cost_reduction_under_MAX_BCR = Collections.max(v);
                WVertex[] vertices = max_cost_reduction_of_each_node.keySet().toArray(new WVertex[0]);
                for (int j = 0; j < vertices.length; j++) {
                    Mem_ERT_C_BCR_MAX value = max_cost_reduction_of_each_node.get(vertices[j]);
                    if (value.getBCR() == max_BCR && value.getCost() == max_cost_reduction_under_MAX_BCR) {
                        target_node = vertices[j];
                        target_mem = value.getMem();
                        break;
                    }
                }
            } else {
                ArrayList<Double> v = new ArrayList<Double>();
                Mem_ERT_C[] values = rt_m_ert_c_cost_reduction_of_each_node.values().toArray(new Mem_ERT_C[0]);
                for (int j = 0; j < values.length; j++)
                    v.add(values[j].getCost());
                double max_cost_reduction = Collections.max(v);
                v.clear();
                for (int j = 0; j < values.length; j++)
                    if (values[j].getCost() == max_cost_reduction)
                        v.add(values[j].getRt());
                double min_increased_rt_under_MAX_cost_reduction = Collections.min(v);
                ArrayList<Integer> vm = new ArrayList<Integer>();
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
            }

            TreeMap<WVertex, Integer> mem_dict = new TreeMap<WVertex, Integer>();
            mem_dict.put(target_node, target_mem);
            this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);

            double max_cost_reduction = 0, min_increased_rt_under_MAX_cost_reduction = 0;
            if (BCR && BCRtype.equals("MAX")) {
                max_cost_reduction = max_cost_reduction_of_each_node.get(target_node).getCost();
                min_increased_rt_under_MAX_cost_reduction = max_cost_reduction_of_each_node.get(target_node).getRt();
            } else {
                max_cost_reduction = rt_m_ert_c_cost_reduction_of_each_node.get(target_node).getCost();
                min_increased_rt_under_MAX_cost_reduction = rt_m_ert_c_cost_reduction_of_each_node.get(target_node).getRt();
            }
            current_cost = current_cost - max_cost_reduction;
            current_avg_rt = current_avg_rt + min_increased_rt_under_MAX_cost_reduction;
            performance_surplus = performance_surplus - min_increased_rt_under_MAX_cost_reduction;
            double current_e2ert_cost_BCR = Math.abs(max_cost_reduction / min_increased_rt_under_MAX_cost_reduction);
            if (current_e2ert_cost_BCR == Double.POSITIVE_INFINITY)
                last_e2ert_cost_BCR = 0;
            else
                last_e2ert_cost_BCR = current_e2ert_cost_BCR;

            loopTime = System.currentTimeMillis();
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
        System.out.println("Repeated times: " + iter);
        System.out.println("Optimized Memory Configuration: " + sb);
        System.out.println("Performance Constraint: " + performanceConstraint + " ms.");
        System.out.println("Average end-to-end response time:" + current_avg_rt + " ms.");
        System.out.println("Average Cost:" + current_cost + " USD.");
        System.out.println("Iteration Count: " + iterations_count);
        System.out.println("Time: " + (double) (endTime - startTime) / 1000 + " s");
        System.out.println("PRCP_PCCO Optimization Completed.");
        return new ERT_C_MEM_Config_iter(performanceConstraint, current_avg_rt, current_cost, mem_backup, iterations_count, BCRthreshold);
    }

    public long PRCP_OPT(double[] constraints, String OPTType, double BCRthreshold, int iter) {
        ArrayList<ERT_C_MEM_Config_iter[]> results = new ArrayList<ERT_C_MEM_Config_iter[]>();
        long PRCPStartTime = System.currentTimeMillis();
        for (double constraint : constraints) {
            ERT_C_MEM_Config_iter[] tempResults = new ERT_C_MEM_Config_iter[4];
            if (OPTType.equals("BCPO")) {
                tempResults[0] = this.PRCPG_BCPO(constraint, false, null, BCRthreshold, iter);
                tempResults[1] = this.PRCPG_BCPO(constraint, true, "RT/M", BCRthreshold, iter);
                tempResults[2] = this.PRCPG_BCPO(constraint, true, "ERT/C", BCRthreshold, iter);
                tempResults[3] = this.PRCPG_BCPO(constraint, true, "MAX", BCRthreshold, iter);
            } else if (OPTType.equals("PCCO")) {
                tempResults[0] = this.PRCPG_PCCO(constraint, false, null, BCRthreshold, iter);
                tempResults[1] = this.PRCPG_PCCO(constraint, true, "M/RT", BCRthreshold, iter);
                tempResults[2] = this.PRCPG_PCCO(constraint, true, "C/ERT", BCRthreshold, iter);
                tempResults[3] = this.PRCPG_PCCO(constraint, true, "MAX", BCRthreshold, iter);
            }
            results.add(tempResults);
        }
        long PRCPEndTime = System.currentTimeMillis();

        DataStoreTools.PRCPDataStore(results, this.perfOpt.getApp().getGraph().getNode_num(), OPTType, iter);
        return (PRCPEndTime - PRCPStartTime) / constraints.length;
    }
}
