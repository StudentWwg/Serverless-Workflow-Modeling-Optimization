package PRCPG;

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

    class Tuple implements Comparable {  //内部类
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
            //使用最小二乘法计算BCR
            LeastSquareMethod eastSquareMethod = new LeastSquareMethod(x, y, 2); //只需要两阶，其中a1是我们需要的BCR
            double[] coefficients = eastSquareMethod.getCoefficient();
            vertices[i].setBCR(Math.abs(coefficients[1]));
        }
    }

    public void update_available_mem_list(boolean BCR, double BCRthreshold, boolean BCRinverse) {
        //更新各结点可用的mem
        WVertex[] node_list = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for (int i = 0; i < node_list.length; i++) {
            WVertex node_i = node_list[i];
            ArrayList<Integer> available_mem_list = new ArrayList<>();
            if (BCR) {
                Integer[] mem_list = node_list[i].getPerf_profile().keySet().toArray(new Integer[0]);
                Arrays.sort(mem_list);  //将可用内存升序排序
                if (BCRinverse) {  //M/RT
                    for (int j = 0; j < mem_list.length - 1; j++) {
                        if (Math.abs((mem_list[j + 1] - mem_list[j]) / (node_i.getPerf_profile().get(mem_list[j + 1]) - node_i.getPerf_profile().get(mem_list[j]))) >
                                1.0 / node_i.getBCR() * BCRthreshold) {
                            available_mem_list.add(mem_list[j]);
                        }
                    }
                    available_mem_list.add(mem_list[mem_list.length - 1]);
                } else {  //RT/M
                    for (int j = 0; j < mem_list.length - 1; j++) {
                        if (Math.abs((node_i.getPerf_profile().get(mem_list[j + 1]) - node_i.getPerf_profile().get(mem_list[j])) / (mem_list[j + 1] - mem_list[j])) >
                                node_i.getBCR() * BCRthreshold) {
                            available_mem_list.add(mem_list[j]);
                        }
                    }
                    available_mem_list.add(mem_list[mem_list.length - 1]);
                }

                node_i.setAvailable_mem_list(Arrays.stream(available_mem_list.toArray(new Integer[0])).mapToInt(Integer::valueOf).toArray());  //经过RT/M 或M/RT 策略筛选的mem_list
            } else {
                Integer[] mem_list = node_list[i].getPerf_profile().keySet().toArray(new Integer[0]);
                Arrays.sort(mem_list);  //将可用内存升序排序
                node_i.setAvailable_mem_list(Arrays.stream(mem_list).mapToInt(Integer::valueOf).toArray());
            }
        }//for
    }

    public GraphPath<WVertex, WEdge> find_PRCP(int order, boolean leastCritical) {
        AllDirectedPaths allDirectedPaths = new AllDirectedPaths(this.perfOpt.getApp().getGraph().getDirectedGraph());  //所有简单路径
        List<GraphPath<WVertex, WEdge>> graphPathList = allDirectedPaths.getAllPaths(this.perfOpt.getApp().getGraph().getStart(), this.perfOpt.getApp().getGraph().getEnd(), true, 10000);
        if (this.arr == null) {  //只有为空的时候才需要计算各条路径的期望长度
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
                arr[i] = new Tuple(prrt_list[i], i);  //将值和索引都封装在内
            Arrays.sort(arr);  //根据路径期望长度排序(从大到小)
        }
        int indexOfPRPC = arr[order].index; //路径期望长度排序第order的索引
        return graphPathList.get(indexOfPRPC);
    }

    //四种策略  without BCR、MAX、ERT/C、RT/M
    public ERT_C_MEM_Config_iter PRCPG_BCPO(double budget, boolean BCR, String BCRtype, double BCRthreshold) {
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
            this.update_available_mem_list(false, BCRthreshold, false); // 这里只需要第一个参数为false即可

        this.perfOpt.update_App_workflow_mem_rt_cost(this.perfOpt.getMinimal_mem_configuration()); //函数都更新为最小内存配置
        double cost = this.perfOpt.getMinimal_cost();
        double surplus = budget - cost; //剩余cost可增长空间
        double current_avg_rt = this.perfOpt.getMaximal_avg_rt();
        double current_cost = this.perfOpt.getMinimal_cost();
        double last_e2ert_cost_BCR = 0; // 上一次迭代过程中产生的最优 ΔERT/ΔC  由每一轮迭代后的BCR进行更新，用于下一次迭代的比较
        int order = 0;  //用于寻找简单路径，order在函数中表现为简单路径在paths中的索引
        int iterations_count = 0; //迭代次数
        while (new BigDecimal(surplus).setScale(4, RoundingMode.HALF_UP).doubleValue() >= 0) {
            iterations_count += 1;
            GraphPath<WVertex, WEdge> cp = this.find_PRCP(order, false);
            //max_avg_rt_reduction_of_each_node 保存每个结点最大的BCR, ΔERT，最小ΔC和对应的mem配置
            // key：node  value:(使得BCR,ΔERT,ΔC都最优的mem，最大ΔERT，最小ΔC，最大BCR)
            HashMap<WVertex, Mem_ERT_C_BCR_MAX> max_avg_rt_reduction_of_each_node = new HashMap<>();  //针对max策略
            HashMap<WVertex, Mem_ERT_C> rt_m_ert_c_avg_rt_reduction_of_each_node = new HashMap<WVertex, Mem_ERT_C>();  //针对ERT/C和RT/M策略
            HashMap<WVertex, Integer> mem_backup = new HashMap<WVertex, Integer>();  //每个结点的当前内存配置信息
            WVertex[] vertexArr = cp.getVertexList().toArray(new WVertex[0]);
            List<WVertex> nodes = cp.getVertexList();
            for (int i = 0; i < nodes.size(); i++) {  // 计算每条critical path 中的每个结点在某种mem配置下具有的最优 ΔERT, ΔC ,BCR
                WVertex node = nodes.get(i);
                //avg_rt_reduction_of_each_mem_config是字典    key：mem   value：元组(rt_reduction, increased_cost)
                HashMap<Integer, ERT_C_RT_M> avg_rt_reduction_of_each_mem_config = new HashMap<Integer, ERT_C_RT_M>();   //保存每种mem下的 ΔERT 和 ΔC  此变量用于ERT/C和RT/M策略
                //avg_rt_reduction_of_each_mem_config_max是字典    key：mem   value：元组(rt_reduction, increased_cost, BCR)
                HashMap<Integer, ERT_C_BCR_MAX> avg_rt_reduction_of_each_mem_config_max = new HashMap<Integer, ERT_C_BCR_MAX>();   //保存每种mem下的 ΔERT、ΔC和BCR  此变量用于MAX策略
                int[] mem_list_reversed = new int[node.getAvailable_mem_list().length];
                for (int j = 0; j < mem_list_reversed.length; j++)
                    mem_list_reversed[j] = node.getAvailable_mem_list()[mem_list_reversed.length - 1 - j];  //将可用mem逆转
                int primary_mem = node.getMem();  //更换结点mem配置前的mem
                for (int j = 0; j < mem_list_reversed.length; j++) {
                    if (mem_list_reversed[j] <= primary_mem) break;
                    TreeMap<WVertex, Integer> mem_dict = new TreeMap<>();  //保存每个结点和对应的mem大小
                    mem_dict.put(node, mem_list_reversed[j]);
                    this.perfOpt.update_App_workflow_mem_rt_cost(mem_dict);
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

                if (BCR && BCRtype.equals("ERT/C")) {
                    Integer[] mems = avg_rt_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);
                    HashMap<Integer, ERT_C_RT_M> temp = new HashMap<>();
                    for (int j = 0; j < mems.length; j++)  //删除不符合ERT/C的mem
                        if (avg_rt_reduction_of_each_mem_config.get(mems[j]).getRt() / avg_rt_reduction_of_each_mem_config.get(mems[j]).getCost() > last_e2ert_cost_BCR * BCRthreshold)
                            temp.put(mems[j], avg_rt_reduction_of_each_mem_config.get(mems[j]));
                    avg_rt_reduction_of_each_mem_config = temp;
                } else if (BCR && BCRtype.equals("MAX")) {
                    Integer[] mems = avg_rt_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);  //key是mem
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
                    double max_BCR = Collections.max(v);  //先获取最大BCR
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getBCR() == max_BCR)
                            v.add(values[j].getRt());
                    double max_rt_reduction_under_MAX_BCR = Collections.max(v);  //获取最大BCR下的最大ERT
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getBCR() == max_BCR && values[j].getRt() == max_rt_reduction_under_MAX_BCR)
                            v.add(values[j].getCost());
                    double min_increased_cost_under_MAX_rt_reduction_MAX_BCR = Collections.max(v);
                    Integer[] mems = avg_rt_reduction_of_each_mem_config_max.keySet().toArray(new Integer[0]);
                    for (int j = 0; j < mems.length; j++) {
                        ERT_C_BCR_MAX get_mem = avg_rt_reduction_of_each_mem_config_max.get(mems[j]);
                        if (get_mem.getBCR() == max_BCR && get_mem.getRt() == max_rt_reduction_under_MAX_BCR &&
                                get_mem.getCost() == min_increased_cost_under_MAX_rt_reduction_MAX_BCR) {
                            max_avg_rt_reduction_of_each_node.put(node, new Mem_ERT_C_BCR_MAX(mems[j], max_rt_reduction_under_MAX_BCR, min_increased_cost_under_MAX_rt_reduction_MAX_BCR, max_BCR));
                            //这里需不需要break再考虑
                        }
                    }
                } else if (avg_rt_reduction_of_each_mem_config.size() != 0) {  //适用于 without BCR、RT/M和ERT/C策略
                    ArrayList<Double> v = new ArrayList<>();
                    ERT_C_RT_M[] values = avg_rt_reduction_of_each_mem_config.values().toArray(new ERT_C_RT_M[0]);
                    for (int j = 0; j < values.length; j++)
                        v.add(values[j].getRt());
                    double max_rt_reduction = Collections.max(v);  //获取最大ERT
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
                if (order >= new AllDirectedPaths<WVertex, WEdge>(this.perfOpt.getApp().getGraph().getDirectedGraph()).getAllPaths(this.perfOpt.getApp().getGraph().getStart(), this.perfOpt.getApp().getGraph().getEnd(), true, 10000).size() - 1)
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
                double max_BCR = Collections.max(v);  //获取最大的BCR
                v.clear();
                for (int j = 0; j < values.length; j++)
                    if (values[j].getBCR() == max_BCR)
                        v.add(values[j].getRt());
                double max_rt_reduction_under_MAX_BCR = Collections.max(v);  //获取最大BCR下的最大ΔERT
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
                double max_rt_reduction = Collections.max(v);  //获取最大ΔERT
                v.clear();
                for (int j = 0; j < values.length; j++)
                    if (values[j].getRt() == max_rt_reduction)
                        v.add(values[j].getCost());
                double min_increased_cost_under_MAX_rt_reduction = Collections.min(v);  //获取最小ΔC
                ArrayList<Integer> vm = new ArrayList<>();
                int min_mem = 3009; // 大于最大可用内存
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

            //每次只更新一个结点
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
        System.out.println("Optimized Memory Configuration: " + sb);
        System.out.println("Budget Constraint: " + budget);
        System.out.println("Average end-to-end response time: " + current_avg_rt);
        System.out.println("Average Cost: " + current_cost);
        System.out.println("Iteration Count: " + iterations_count);
        System.out.println("Time: " + (double)(endTime-startTime)/1000 + " s");
        System.out.println("PRCP_BCPO Optimization Completed.");
        return new ERT_C_MEM_Config_iter(budget, current_avg_rt, current_cost, mem_backup, iterations_count, BCRthreshold);
    }

    //四种策略  without BCR、MAX、C/ERT、M/RT
    public ERT_C_MEM_Config_iter PRCPG_PCCO(double performanceConstraint, boolean BCR, String BCRtype, double BCRthreshold) {
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
            this.update_available_mem_list(false, BCRthreshold, true); // 这里只需要第一个参数为false即可

        this.perfOpt.update_App_workflow_mem_rt_cost(this.perfOpt.getMaximal_mem_configuration());
        double current_avg_rt = this.perfOpt.getMinimal_avg_rt();
        double performance_surplus = performanceConstraint - current_avg_rt;
        double current_cost = this.perfOpt.getMaximal_cost();
        double last_e2ert_cost_BCR = 0; // 上一次迭代过程中产生的最优 ΔC/ΔERT  由每一轮迭代后的BCR进行更新，用于下一次迭代的比较
        int order = 0;  //用于寻找简单路径，order在函数中表现为简单路径在paths中的索引
        int iterations_count = 0; //迭代次数

        while (new BigDecimal(performance_surplus).setScale(4, RoundingMode.HALF_UP).doubleValue() >= 0) {
            iterations_count += 1;
            GraphPath<WVertex, WEdge> cp = this.find_PRCP(order, true);
            //max_cost_reduction_of_each_node  保存每个结点最大的BCR, ΔC，最小ΔERT 和 对应的mem配置
            // key：node  value:(使得BCR,ΔERT,ΔC都最优的mem，最大ΔERT，最小ΔC，最大BCR)
            HashMap<WVertex, Mem_ERT_C_BCR_MAX> max_cost_reduction_of_each_node = new HashMap<WVertex, Mem_ERT_C_BCR_MAX>();  //针对max策略
            HashMap<WVertex, Mem_ERT_C> rt_m_ert_c_cost_reduction_of_each_node = new HashMap<WVertex, Mem_ERT_C>();  //针对ERT/C和RT/M策略
            WVertex[] vertexArr = cp.getVertexList().toArray(new WVertex[0]);
            List<WVertex> nodes = cp.getVertexList();
            for (int i = 0; i < nodes.size(); i++) {  // 计算每条critical path 中的每个结点在某种mem配置下具有的最优 ΔERT, ΔC ,BCR
                WVertex node = nodes.get(i);
                //avg_rt_reduction_of_each_mem_config是字典    key：mem   value：元组(rt_reduction, increased_cost)
                HashMap<Integer, ERT_C_RT_M> cost_reduction_of_each_mem_config = new HashMap<Integer, ERT_C_RT_M>();   //保存每种mem下的 ΔERT 和 ΔC  此变量用于ERT/C和RT/M策略
                //avg_rt_reduction_of_each_mem_config_max是字典    key：mem   value：元组(rt_reduction, increased_cost, BCR)
                HashMap<Integer, ERT_C_BCR_MAX> cost_reduction_of_each_mem_config_max = new HashMap<Integer, ERT_C_BCR_MAX>();   //保存每种mem下的 ΔERT、ΔC和BCR  此变量用于MAX策略
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

                if (BCR && BCRtype.equals("C/ERT")) {
                    Integer[] mems = cost_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);
                    //这里使用temp暂时保存满足要求的结果，主要是怕cost_reduction_of_each_mem_config删除元素时出现迭代器失效的情况
                    HashMap<Integer, ERT_C_RT_M> temp = new HashMap<>();
                    for (int j = 0; j < mems.length; j++)  //删除不符合ERT/C的mem
                        if (cost_reduction_of_each_mem_config.get(mems[j]).getCost() / cost_reduction_of_each_mem_config.get(mems[j]).getRt() > last_e2ert_cost_BCR * BCRthreshold)
                            temp.put(mems[j], cost_reduction_of_each_mem_config.get(mems[j]));
                    cost_reduction_of_each_mem_config = temp;
                } else if (BCR && BCRtype.equals("MAX")) {
                    Integer[] mems = cost_reduction_of_each_mem_config.keySet().toArray(new Integer[0]);  //key是mem
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
                    double max_BCR = Collections.max(v);  //先获取最大BCR
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getBCR() == max_BCR)
                            v.add(values[j].getCost());
                    double max_cost_reduction_under_MAX_BCR = Collections.max(v);  //获取最大BCR下的最大ΔC
                    v.clear();
                    for (int j = 0; j < values.length; j++)
                        if (values[j].getBCR() == max_BCR && values[j].getCost() == max_cost_reduction_under_MAX_BCR)
                            v.add(values[j].getRt());
                    double min_increased_rt_under_MAX_rt_reduction_MAX_BCR = Collections.min(v);
                    Integer[] mems = cost_reduction_of_each_mem_config_max.keySet().toArray(new Integer[0]);
                    for (int j = 0; j < mems.length; j++) {
                        ERT_C_BCR_MAX get_mem = cost_reduction_of_each_mem_config_max.get(mems[j]);
                        if (get_mem.getBCR() == max_BCR && get_mem.getCost() == max_cost_reduction_under_MAX_BCR &&
                                get_mem.getRt() == min_increased_rt_under_MAX_rt_reduction_MAX_BCR) {
                            max_cost_reduction_of_each_node.put(node, new Mem_ERT_C_BCR_MAX(mems[j], min_increased_rt_under_MAX_rt_reduction_MAX_BCR, max_cost_reduction_under_MAX_BCR, max_BCR));
                        }
                    }
                } else if (cost_reduction_of_each_mem_config.size() != 0) {
                    ArrayList<Double> v = new ArrayList<Double>();
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

            if ((BCR && BCRtype.equals("MAX") && max_cost_reduction_of_each_node.size() == 0) ||
                    ((BCR == false || (BCR && (BCRtype.equals("M/RT") || BCRtype.equals("C/ERT")))) && rt_m_ert_c_cost_reduction_of_each_node.size() == 0)) {
                if (order >= new AllDirectedPaths<WVertex, WEdge>(this.perfOpt.getApp().getGraph().getDirectedGraph()).getAllPaths(this.perfOpt.getApp().getGraph().getStart(), this.perfOpt.getApp().getGraph().getEnd(), true, 10000).size() - 1)
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
                double max_BCR = Collections.max(v);  //获取最大的BCR
                v.clear();
                for (int j = 0; j < values.length; j++)
                    if (values[j].getBCR() == max_BCR)
                        v.add(values[j].getCost());
                double max_cost_reduction_under_MAX_BCR = Collections.max(v);  //获取最大BCR下的最大ΔC
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
                double max_cost_reduction = Collections.max(v);  //获取最大ΔC
                v.clear();
                for (int j = 0; j < values.length; j++)
                    if (values[j].getCost() == max_cost_reduction)
                        v.add(values[j].getRt());
                double min_increased_rt_under_MAX_cost_reduction = Collections.min(v);  //获取最小ΔERT
                ArrayList<Integer> vm = new ArrayList<Integer>();
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
        System.out.println("Performance Constraint: " + performanceConstraint);
        System.out.println("Average end-to-end response time:" + current_avg_rt);
        System.out.println("Average Cost:" + current_cost);
        System.out.println("Iteration Count: " + iterations_count);
        System.out.println("Time: " + (double)(endTime-startTime)/1000 + " s");
        System.out.println("PRCP_PCCO Optimization Completed.");
        return new ERT_C_MEM_Config_iter(performanceConstraint, current_avg_rt, current_cost, mem_backup, iterations_count, BCRthreshold);
    }

    public long PRCP_OPT(double[] constraints, String OPTType, double BCRthreshold,int repeatedTimes) {
        ArrayList<ERT_C_MEM_Config_iter[]> results = new ArrayList<ERT_C_MEM_Config_iter[]>();
        long PRCPStartTime = System.currentTimeMillis();
        for (double constraint : constraints) {   //constraint为budgetConstraint或performanceConstraint
            ERT_C_MEM_Config_iter[] tempResults = new ERT_C_MEM_Config_iter[4];
            if (OPTType.equals("BCPO")) {
                tempResults[0] = this.PRCPG_BCPO(constraint, false, null, BCRthreshold);
                tempResults[1] = this.PRCPG_BCPO(constraint, true, "RT/M", BCRthreshold);
                tempResults[2] = this.PRCPG_BCPO(constraint, true, "ERT/C", BCRthreshold);
                tempResults[3] = this.PRCPG_BCPO(constraint, true, "MAX", BCRthreshold);
            } else if (OPTType.equals("PCCO")) {
                tempResults[0] = this.PRCPG_PCCO(constraint, false, null, BCRthreshold);
                tempResults[1] = this.PRCPG_PCCO(constraint, true, "M/RT", BCRthreshold);
                tempResults[2] = this.PRCPG_PCCO(constraint, true, "C/ERT", BCRthreshold);
                tempResults[3] = this.PRCPG_PCCO(constraint, true, "MAX", BCRthreshold);
            }
            results.add(tempResults);
        }
        long PRCPEndTime = System.currentTimeMillis();

        DataStoreTools.PRCPDataStore(results, this.perfOpt.getApp().getGraph().getNode_num(), OPTType,repeatedTimes);
        return (PRCPEndTime-PRCPStartTime) / constraints.length;
    }
}
