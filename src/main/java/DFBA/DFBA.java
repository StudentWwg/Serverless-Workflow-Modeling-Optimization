package DFBA;

import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;
import serverlessWorkflow.graph.WEdge;
import serverlessWorkflow.graph.WVertex;
import util.DataStoreTools;

import java.util.*;

public class DFBA {
    private PerfOpt perfOpt;
    private HashMap<WVertex, HashMap<Integer, Double>> avgCostMap = new HashMap<>();
    private HashMap<WVertex, HashMap<Integer, Double>> avgRtCostRatio = new HashMap<>();
    private HashMap<WVertex, Integer> minimumCostConfiguration = new HashMap<>();
    private HashMap<WVertex, Integer> bestPerformanceConfiguration = new HashMap<>();
    private HashMap<WVertex, Integer> maximumCostConfiguration = new HashMap<>();
    private HashMap<WVertex, Integer> worstPerformanceConfiguration = new HashMap<>();
    private static double earlyRejectThreshold = 1.1;
    private static String OPTType;

    public DFBA(PerfOpt perfOpt) {
        this.perfOpt = perfOpt;
        Set<WVertex> vertices = perfOpt.getApp().getGraph().getDirectedGraph().vertexSet();
        for (WVertex aVertex : vertices)
            aVertex.setAvailable_mem_list(perfOpt.getAvalable_mem_list());

        this.initializePerformanceProfile();
    }

    public void initializePerformanceProfile() {
        Set<WVertex> verticesSet = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet();
        for (WVertex aVertex : verticesSet) {
            HashMap<Integer, Double> memCostMap = new HashMap<>();
            double maxRT = Double.MIN_VALUE, costOfMaxRT = -1;
            double maxCost = Double.MIN_VALUE, minCost = Double.MAX_VALUE, minRT = Double.MAX_VALUE;
            int memOfMaxRt = 0, memOfMaxCost = 0, memOfMinRT = 0, memOfMinCost = 0;
            for (Map.Entry<Integer, Double> item : aVertex.getPerf_profile().entrySet()) {
                int mem = item.getKey();
                double rt = item.getValue();
                double cost = this.perfOpt.getApp().GetVertexCostInMem(aVertex, mem);
                memCostMap.put(mem, cost);
                if (rt > maxRT) {
                    maxRT = rt;
                    memOfMaxRt = mem;
                    costOfMaxRT = cost;
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
            this.avgCostMap.put(aVertex, memCostMap); //结点和不同内存下的cost映射
            this.minimumCostConfiguration.put(aVertex, memOfMinCost);
            this.bestPerformanceConfiguration.put(aVertex, memOfMinRT);
            this.maximumCostConfiguration.put(aVertex, memOfMaxCost);
            this.worstPerformanceConfiguration.put(aVertex, memOfMaxRt);

            HashMap<Integer, Double> memRatioMap = new HashMap<>();
            for (Map.Entry<Integer, Double> item : aVertex.getPerf_profile().entrySet()) {
                int mem = item.getKey();
                double rt = item.getValue();
                double cost = this.avgCostMap.get(aVertex).get(mem);
                double ratio = 0;
                if (mem != memOfMaxRt) ratio = (maxRT - rt) / (costOfMaxRT - cost);
                memRatioMap.put(mem, ratio);
            }
            this.avgRtCostRatio.put(aVertex, memRatioMap);
        }
    }

    class TwoTuple{
        WVertex msiFunction = null;
        double mostSignificantImpact = -1;
        public TwoTuple(WVertex msif, double impact) {
            msiFunction = msif;
            mostSignificantImpact = impact;
        }
    }

    public TwoTuple DFSGetMostSignificantFunction(WVertex root, HashMap<WVertex, Boolean> isOptimized) {
        WVertex vertex = null;
        if (root == this.perfOpt.getApp().getGraph().getEnd()) {
            vertex = root;
            if (!isOptimized.get(vertex)) {
                if(DFBA.OPTType.equals("PCCO"))
                    return new TwoTuple(vertex,vertex.getCost());
                else if(DFBA.OPTType.equals("BCPO"))
                    return new TwoTuple(vertex, vertex.getRt());
            } else {
                return new TwoTuple(null,-1);
            }
        }

        if (root != null) vertex = root;
        else vertex = perfOpt.getApp().getGraph().getStart();
        Set<WEdge> outGoingEdges = this.perfOpt.getApp().getGraph().getDirectedGraph().outgoingEdgesOf(vertex);
        TwoTuple bottleneckFunctionCandidate = null;
        double maxImpactOfRt = -1, maxImpactOfCost = -1;
        for (WEdge aEdge : outGoingEdges) {
            WVertex endVertex = aEdge.getV2();
            TwoTuple bottleneckFunctionOfSubgraph = DFSGetMostSignificantFunction(endVertex,isOptimized);
            if(DFBA.OPTType.equals("PCCO") && bottleneckFunctionOfSubgraph.mostSignificantImpact > maxImpactOfCost){
                maxImpactOfCost = bottleneckFunctionOfSubgraph.mostSignificantImpact;
                if (bottleneckFunctionOfSubgraph.msiFunction != null){
                    bottleneckFunctionCandidate = bottleneckFunctionOfSubgraph;
                }
            }else if(DFBA.OPTType.equals("BCPO") && bottleneckFunctionOfSubgraph.mostSignificantImpact > maxImpactOfRt){
                maxImpactOfRt = bottleneckFunctionOfSubgraph.mostSignificantImpact;
                if (bottleneckFunctionOfSubgraph.msiFunction != null){
                    bottleneckFunctionCandidate = bottleneckFunctionOfSubgraph;
                }
            }
        }

        if(DFBA.OPTType.equals("PCCO")){
            if(bottleneckFunctionCandidate.msiFunction.getCost() > vertex.getCost())
                return new TwoTuple(bottleneckFunctionCandidate.msiFunction, bottleneckFunctionCandidate.mostSignificantImpact+ vertex.getCost());
            else
                return new TwoTuple(vertex,bottleneckFunctionCandidate.mostSignificantImpact+ vertex.getCost());
        }else if(DFBA.OPTType.equals("BCPO")){
            if(bottleneckFunctionCandidate.msiFunction.getRt() > vertex.getRt())
                return new TwoTuple(bottleneckFunctionCandidate.msiFunction, bottleneckFunctionCandidate.mostSignificantImpact+ vertex.getRt());
            else
                return new TwoTuple(vertex,bottleneckFunctionCandidate.mostSignificantImpact+ vertex.getRt());
        }else return null;
//        if (bottleneckFunctionCandidate.isEmpty())
//            return null;
//
//        if (DFBA.OPTType.equals("PCCO")) {
//            double maxCost = Double.MIN_VALUE;
//            if (isOptimized.get(vertex))
//                vertex = null;
//            else {
//                maxCost = vertex.getCost();
//            }
//            for (WVertex aVertex : bottleneckFunctionCandidate) {
//                if (isOptimized.get(aVertex))
//                    continue;
//                if (aVertex.getCost() > maxCost){
//                    maxCost = aVertex.getCost();
//                    vertex = aVertex;
//                }
//            }
//        } else if (DFBA.OPTType.equals("BCPO")) {
//            double maxRT = Double.MIN_VALUE;
//            if (isOptimized.get(vertex))
//                vertex = null;
//            else {
//                maxRT = vertex.getRt();
//            }
//            for (WVertex aVertex : bottleneckFunctionCandidate) {
//                if (isOptimized.get(aVertex))
//                    continue;
//                if (aVertex.getRt() > maxRT){
//                    maxRT = aVertex.getRt();
//                    vertex = aVertex;
//                }
//            }
//        }
//        return vertex;
    }

    public ArrayList<Integer> getEligibleMemOptions(WVertex vertex, int mem, HashMap<WVertex, ArrayList<Integer>> ineligibleMemOptionsMap) {
        if (DFBA.OPTType.equals("BCPO")) {
            TreeMap<Integer, Double> perfProfile = vertex.getPerf_profile();
            double rtPre = perfProfile.get(mem);

            TreeMap<Integer, Double> memRtMap = new TreeMap<>();
            for (int aMem : vertex.getPerf_profile().keySet()) {
                if (perfProfile.get(aMem) < rtPre && !ineligibleMemOptionsMap.get(vertex).contains(aMem))
                    memRtMap.put(aMem, perfProfile.get(aMem));
            }

            List<Map.Entry<Integer, Double>> memRtMapList = new ArrayList<Map.Entry<Integer, Double>>(memRtMap.entrySet());
            Collections.sort(memRtMapList, new Comparator<Map.Entry<Integer, Double>>() {
                @Override
                public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                    //返回值为1, o1和o2交换位置, 此处是按照rt从小到大排序
                    if (o1.getValue() > o2.getValue()) return 1;
                    else if (o1.getValue() == o2.getValue()) return 0;
                    else return -1;
//                    return Double.compare(o1.getValue(), o2.getValue());
                }
            });

            ArrayList<Integer> options = new ArrayList<>();
            for (Map.Entry<Integer, Double> item : memRtMapList)
                options.add(item.getKey());
            return options;
        } else if (DFBA.OPTType.equals("PCCO")) {
            double costPre = this.avgCostMap.get(vertex).get(mem);
            TreeMap<Integer, Double> memCostMap = new TreeMap<>();

            for (int aMem : vertex.getAvailable_mem_list()) {
                double costUnderaMem = this.avgCostMap.get(vertex).get(aMem);
                if (costUnderaMem < costPre && !ineligibleMemOptionsMap.get(vertex).contains(aMem))
                    memCostMap.put(aMem, costUnderaMem);
            }

            List<Map.Entry<Integer, Double>> memCostMapList = new ArrayList<Map.Entry<Integer, Double>>(memCostMap.entrySet());
            Collections.sort(memCostMapList, new Comparator<Map.Entry<Integer, Double>>() {
                @Override
                public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                    if (o1.getValue() > o2.getValue()) return 1;
                    else if (o1.getValue() == o2.getValue()) return 0;
                    else return -1;
//                    return Double.compare(o1.getValue(), o2.getValue());
                }
            });

            ArrayList<Integer> options = new ArrayList<>();
            for (Map.Entry<Integer, Double> item : memCostMapList)
                options.add(item.getKey());
            return options;
        } else return null;
    }

    public DFBA_Result DFBA_PCCO(double performanceConstraint, int iter) {
        long startTime = System.currentTimeMillis();
        this.initializeWithBestPerformanceConfiguration();
        HashMap<WVertex, Boolean> isOptimized = new HashMap<>();
        for (WVertex vertex : perfOpt.getApp().getGraph().getDirectedGraph().vertexSet())
            isOptimized.put(vertex, false);

        HashMap<WVertex, ArrayList<Integer>> ineligibleMemOptionsMap = new HashMap<>();
        for (WVertex vertex : perfOpt.getApp().getGraph().getDirectedGraph().vertexSet())
            ineligibleMemOptionsMap.put(vertex, new ArrayList<>());
        ArrayList<WVertex> functionsToBeRevisited = new ArrayList<>();
        HashMap<WVertex, Double> earlyRejectMap = new HashMap<>();
        for (WVertex aVertex : perfOpt.getApp().getGraph().getDirectedGraph().vertexSet())
            earlyRejectMap.put(aVertex, Double.MAX_VALUE);
        boolean skipDfsFlag = false;
        WVertex mostSignificantFunction = null;
        WVertex nextMostSignificantFunction = null;
        TreeMap<WVertex, Integer> configuration = new TreeMap<>();

        long loopTime = System.currentTimeMillis();
        while (loopTime-startTime <= Double.MAX_VALUE) {
            if (!skipDfsFlag) {
                mostSignificantFunction = DFSGetMostSignificantFunction(perfOpt.getApp().getGraph().getStart(),isOptimized).msiFunction;
                skipDfsFlag = false;
            } else {
                mostSignificantFunction = nextMostSignificantFunction;
            }
            if (mostSignificantFunction == null) {
                if (functionsToBeRevisited.isEmpty()) {
                    long endTime = System.currentTimeMillis();
                    Set<WVertex> vertices = perfOpt.getApp().getGraph().getDirectedGraph().vertexSet();
                    for (WVertex aVertex : vertices)
                        configuration.put(aVertex, aVertex.getMem());
                    StringBuffer sb = new StringBuffer("");
                    for (Map.Entry<WVertex,Integer> entry : configuration.entrySet())
                        sb.append(entry.getKey().toString() + ":" + entry.getValue().toString() + "  ");
                    double rt = perfOpt.getApp().GetAverageRT();
                    double cost = perfOpt.getApp().GetAverageCost();

                    System.out.println("Repeated times: " + iter);
                    System.out.println("Optimized Memory Configuration: " + sb);
                    System.out.println("Performance constraint: " + performanceConstraint +" ms.");
                    System.out.println("Average end-to-end response time:" + rt +" ms.");
                    System.out.println("Average Cost:" + cost +" USD.");
                    System.out.println("Time: " + (double) (endTime - startTime) / 1000 + " s");
                    System.out.println("DFBA_PCCO Optimization Completed.");
                    DFBA_Result dfbaResult = new DFBA_Result(performanceConstraint,rt,cost,configuration);
                    return dfbaResult;
                } else break;
            }

            int previousMem = mostSignificantFunction.getMem();
            ArrayList<Integer> eligibleMemOptions = getEligibleMemOptions(mostSignificantFunction, previousMem, ineligibleMemOptionsMap);
            if (eligibleMemOptions.isEmpty()) {
                isOptimized.put(mostSignificantFunction, true);
                skipDfsFlag = false;
                continue;
            }

            boolean constraintPreviouslySatisfiedFlag = false;
            boolean msFlag = false;
            double minimumNonMsiRT = Double.MAX_VALUE;
            int minimumNonMsiMem = previousMem;

            for (int mem : eligibleMemOptions) {
                if (mostSignificantFunction.getPerf_profile().get(mem) > earlyRejectMap.get(mostSignificantFunction) * earlyRejectThreshold)
                    continue;
                TreeMap memConf = new TreeMap();
                memConf.put(mostSignificantFunction, mem);
                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                double rtOfMem = perfOpt.getApp().GetAverageRT();
                if (rtOfMem > performanceConstraint) {
                    ineligibleMemOptionsMap.get(mostSignificantFunction).add(mem);
                    if (earlyRejectMap.get(mostSignificantFunction) > mostSignificantFunction.getPerf_profile().get(mem))
                        earlyRejectMap.put(mostSignificantFunction, mostSignificantFunction.getPerf_profile().get(mem));
                    continue;
                }

                WVertex newMostSignificantFunction = DFSGetMostSignificantFunction(null,isOptimized).msiFunction;
                if (newMostSignificantFunction == mostSignificantFunction) {
                    if (!constraintPreviouslySatisfiedFlag) {
                        minimumNonMsiMem = mem;
                        isOptimized.put(mostSignificantFunction, true);
                        if (this.bestPerformanceConfiguration.get(mostSignificantFunction) == previousMem) {
                            HashMap<Integer, Double> rtCostRatio = new HashMap<>();
                            for (int memory : eligibleMemOptions) {
                                if (this.avgRtCostRatio.get(mostSignificantFunction).get(memory) > 0 && mostSignificantFunction.getPerf_profile().get(memory) <
                                        mostSignificantFunction.getPerf_profile().get(minimumNonMsiMem) &&
                                        this.avgRtCostRatio.get(mostSignificantFunction).get(memory) >
                                        this.avgRtCostRatio.get(mostSignificantFunction).get(minimumNonMsiMem)) {
                                    rtCostRatio.put(memory, this.avgRtCostRatio.get(mostSignificantFunction).get(memory));
                                }
                            }

                            ArrayList<Integer> sortedOptimalMemOptions = new ArrayList<>();
                            ArrayList<Map.Entry<Integer, Double>> list = new ArrayList(rtCostRatio.entrySet());
                            Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
                                @Override
                                //这里要采用降序排序
                                public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                                    if (o1.getValue() > o2.getValue()) return -1;
                                    else if (o1.getValue() == o2.getValue()) return 0;
                                    else return 1;
                                }
                            });
                            for (Map.Entry<Integer, Double> entry : list)
                                sortedOptimalMemOptions.add(entry.getKey());

                            rtCostRatio.clear();
                            for (int memory : eligibleMemOptions) {
                                if (this.avgRtCostRatio.get(mostSignificantFunction).get(memory) < 0 && mostSignificantFunction.getPerf_profile().get(memory) <
                                        mostSignificantFunction.getPerf_profile().get(minimumNonMsiMem) &&
                                        this.avgRtCostRatio.get(mostSignificantFunction).get(memory) <
                                        this.avgRtCostRatio.get(mostSignificantFunction).get(minimumNonMsiMem)) {
                                    rtCostRatio.put(memory, this.avgRtCostRatio.get(mostSignificantFunction).get(memory));
                                }
                            }
                            list = new ArrayList(rtCostRatio.entrySet());
                            Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
                                @Override
                                public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                                    if (o1.getValue() > o2.getValue()) return 1;
                                    else if (o1.getValue() == o2.getValue()) return 0;
                                    else return -1;
                                }
                            });
                            for (Map.Entry<Integer, Double> entry : list)
                                sortedOptimalMemOptions.add(entry.getKey());

                            if (sortedOptimalMemOptions.isEmpty())
                                break;
                            functionsToBeRevisited.add(mostSignificantFunction);
                            skipDfsFlag = false;
                            for (int ms : sortedOptimalMemOptions) {
                                if (mostSignificantFunction.getPerf_profile().get(ms) > earlyRejectMap.get(mostSignificantFunction) * earlyRejectThreshold)
                                    continue;
                                memConf = new TreeMap();
                                memConf.put(mostSignificantFunction, ms);
                                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                                double rtOfMs = perfOpt.getApp().GetAverageRT();
                                if (rtOfMs > performanceConstraint) {
                                    ineligibleMemOptionsMap.get(mostSignificantFunction).add(ms);
                                    if (earlyRejectMap.get(mostSignificantFunction) > mostSignificantFunction.getPerf_profile().get(ms))
                                        earlyRejectMap.put(mostSignificantFunction, mostSignificantFunction.getPerf_profile().get(ms));
                                    continue;
                                } else {
                                    msFlag = true;
                                    break;
                                }
                            }
                            if (!msFlag) {
                                memConf = new TreeMap();
                                memConf.put(mostSignificantFunction, mem);
                                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                            }
                        }
                        break;
                    } else break;
                } else {
                    constraintPreviouslySatisfiedFlag = true;
                    if (rtOfMem < minimumNonMsiRT) {
                        minimumNonMsiRT = rtOfMem;
                        minimumNonMsiMem = mem;
                        nextMostSignificantFunction = newMostSignificantFunction;
                    }
                }
            }

            if (msFlag) continue;
            TreeMap memConf = new TreeMap();
            if (minimumNonMsiMem == previousMem) {
                memConf.put(mostSignificantFunction, previousMem);
                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                isOptimized.put(mostSignificantFunction, true);
                skipDfsFlag = false;
            } else {
                memConf.put(mostSignificantFunction, minimumNonMsiMem);
                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                if (constraintPreviouslySatisfiedFlag) skipDfsFlag = true;
                else skipDfsFlag = false;
            }
            loopTime = System.currentTimeMillis();
            break;
        }

        for (WVertex function : functionsToBeRevisited) {
            boolean msFlag = false;
            int previousMem = function.getMem();
            ArrayList<Integer> eligibleMemOptions = getEligibleMemOptions(function, previousMem, ineligibleMemOptionsMap);
            if (eligibleMemOptions.isEmpty())
                continue;

            for (int mem : eligibleMemOptions) {
                TreeMap<WVertex, Integer> memConf = new TreeMap<>();
                memConf.put(function, mem);
                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                double rtOfMem = perfOpt.getApp().GetAverageRT();
                if (rtOfMem > performanceConstraint)
                    ineligibleMemOptionsMap.get(function).add(mem);
                else {
                    msFlag = true;
                    break;
                }
            }
            if (!msFlag) {
                TreeMap<WVertex, Integer> memConf = new TreeMap<>();
                memConf.put(function, previousMem);
                perfOpt.update_App_workflow_mem_rt_cost(memConf);
            }
        }
        long endTime = System.currentTimeMillis();

        for (WVertex function : perfOpt.getApp().getGraph().getDirectedGraph().vertexSet())
            configuration.put(function, function.getMem());
        StringBuffer sb = new StringBuffer("");
        for (Map.Entry<WVertex,Integer> entry : configuration.entrySet())
            sb.append(entry.getKey().toString() + ":" + entry.getValue().toString() + "  ");
        double rt = perfOpt.getApp().GetAverageRT();
        double cost = perfOpt.getApp().GetAverageCost();

        System.out.println("Repeated times: " + iter);
        System.out.println("Optimized Memory Configuration: " + sb);
        System.out.println("Performance constraint: " + performanceConstraint +" ms.");
        System.out.println("Average end-to-end response time:" + rt + " ms.");
        System.out.println("Average Cost:" + cost + " USD.");
        System.out.println("Time: " + (double) (endTime - startTime) / 1000 + " s");
        System.out.println("DFBA_PCCO Optimization Completed.");
        DFBA_Result dfbaResult = new DFBA_Result(performanceConstraint,rt,cost,configuration);
        return dfbaResult;
    }

    public DFBA_Result DFBA_BCPO(double budgetConstraint, int iter) {
        long startTime = System.currentTimeMillis();
        this.initializeWithMinimumCostConfiguration();
        HashMap<WVertex, Boolean> isOptimized = new HashMap<>();
        for (WVertex vertex : perfOpt.getApp().getGraph().getDirectedGraph().vertexSet())
            isOptimized.put(vertex, false);

        HashMap<WVertex, ArrayList<Integer>> ineligibleMemOptionsMap = new HashMap<>();
        for (WVertex vertex : perfOpt.getApp().getGraph().getDirectedGraph().vertexSet())
            ineligibleMemOptionsMap.put(vertex, new ArrayList<>());
        ArrayList<WVertex> functionsToBeRevisited = new ArrayList<>();
        HashMap<WVertex, Double> earlyRejectMap = new HashMap<>();
        for (WVertex aVertex : perfOpt.getApp().getGraph().getDirectedGraph().vertexSet())
            earlyRejectMap.put(aVertex, Double.MAX_VALUE);
        boolean skipDfsFlag = false;
        WVertex mostSignificantFunction = null;
        WVertex nextMostSignificantFunction = null;
        TreeMap<WVertex, Integer> configuration = new TreeMap<>();

        long loopTime = System.currentTimeMillis();
        while (loopTime - startTime <= Double.MAX_VALUE) {
            if (!skipDfsFlag) {
                mostSignificantFunction = DFSGetMostSignificantFunction(perfOpt.getApp().getGraph().getStart(),isOptimized).msiFunction;
                skipDfsFlag = false;
            } else {
                mostSignificantFunction = nextMostSignificantFunction;
            }
            if (mostSignificantFunction == null) {
                if (functionsToBeRevisited.isEmpty()) {
                    long endTime = System.currentTimeMillis();
                    Set<WVertex> vertices = perfOpt.getApp().getGraph().getDirectedGraph().vertexSet();
                    for (WVertex aVertex : vertices)
                        configuration.put(aVertex, aVertex.getMem());
                    StringBuffer sb = new StringBuffer("");
                    for (Map.Entry<WVertex,Integer> entry : configuration.entrySet())
                        sb.append(entry.getKey().toString() + ":" + entry.getValue().toString() + "  ");
                    double rt = perfOpt.getApp().GetAverageRT();
                    double cost = perfOpt.getApp().GetAverageCost();

                    System.out.println("Repeated times: " + iter);
                    System.out.println("Optimized Memory Configuration: " + sb);
                    System.out.println("Budget constraint: " + budgetConstraint + " USD.");
                    System.out.println("Average end-to-end response time:" + rt +" ms.");
                    System.out.println("Average Cost:" + cost + " USD.");
                    System.out.println("Time: " + (double) (endTime - startTime) / 1000 + " s");
                    System.out.println("DFBA_BCPO Optimization Completed.");
                    DFBA_Result dfbaResult = new DFBA_Result(budgetConstraint,rt,cost,configuration);
                    return dfbaResult;
                } else break;
            }

            int previousMem = mostSignificantFunction.getMem();
            ArrayList<Integer> eligibleMemOptions = getEligibleMemOptions(mostSignificantFunction, previousMem, ineligibleMemOptionsMap);
            if (eligibleMemOptions.isEmpty()) {
                isOptimized.put(mostSignificantFunction, true);
                skipDfsFlag = false;
                continue;
            }

            boolean constraintPreviouslySatisfiedFlag = false;
            boolean msFlag = false;
            double minimumNonMsiCost = Double.MAX_VALUE;
            int minimumNonMsiMem = previousMem;

            for (int mem : eligibleMemOptions) {
                if (this.avgCostMap.get(mostSignificantFunction).get(mem) > earlyRejectMap.get(mostSignificantFunction) * earlyRejectThreshold)
                    continue;
                TreeMap memConf = new TreeMap();
                memConf.put(mostSignificantFunction, mem);
                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                double costOfMem = perfOpt.getApp().GetAverageCost();
                if (costOfMem > budgetConstraint) {
                    ineligibleMemOptionsMap.get(mostSignificantFunction).add(mem);
                    if (earlyRejectMap.get(mostSignificantFunction) > this.avgCostMap.get(mostSignificantFunction).get(mem))
                        earlyRejectMap.put(mostSignificantFunction, this.avgCostMap.get(mostSignificantFunction).get(mem));
                    continue;
                }

                WVertex newMostSignificantFunction = DFSGetMostSignificantFunction(null,isOptimized).msiFunction;
                if (newMostSignificantFunction == mostSignificantFunction) {
                    if (!constraintPreviouslySatisfiedFlag) {
                        minimumNonMsiMem = mem;
                        isOptimized.put(mostSignificantFunction, true);
                        if (this.minimumCostConfiguration.get(mostSignificantFunction) == previousMem) {
                            HashMap<Integer, Double> rtCostRatio = new HashMap<>();
                            for (int memory : eligibleMemOptions) {
                                if (this.avgRtCostRatio.get(mostSignificantFunction).get(memory) > 0 && this.avgCostMap.get(mostSignificantFunction).get(memory) <
                                        this.avgCostMap.get(mostSignificantFunction).get(minimumNonMsiMem) &&
                                        this.avgRtCostRatio.get(mostSignificantFunction).get(memory) >
                                        this.avgRtCostRatio.get(mostSignificantFunction).get(minimumNonMsiMem)) {
                                    rtCostRatio.put(memory, this.avgRtCostRatio.get(mostSignificantFunction).get(memory));
                                }
                            }

                            ArrayList<Integer> sortedOptimalMemOptions = new ArrayList<>();
                            ArrayList<Map.Entry<Integer, Double>> list = new ArrayList(rtCostRatio.entrySet());
                            Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
                                @Override
                                public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                                    if (o1.getValue() > o2.getValue()) return -1;
                                    else if (o1.getValue() == o2.getValue()) return 0;
                                    else return 1;
                                }
                            });
                            for (Map.Entry<Integer, Double> entry : list)
                                sortedOptimalMemOptions.add(entry.getKey());

                            rtCostRatio.clear();
                            for (int memory : eligibleMemOptions) {
                                if (this.avgRtCostRatio.get(mostSignificantFunction).get(memory) < 0 && this.avgCostMap.get(mostSignificantFunction).get(memory) <
                                        this.avgCostMap.get(mostSignificantFunction).get(minimumNonMsiMem) &&
                                        this.avgRtCostRatio.get(mostSignificantFunction).get(memory) <
                                                this.avgRtCostRatio.get(mostSignificantFunction).get(minimumNonMsiMem)) {
                                    rtCostRatio.put(memory, this.avgRtCostRatio.get(mostSignificantFunction).get(memory));
                                }
                            }
                            list = new ArrayList(rtCostRatio.entrySet());
                            Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
                                @Override
                                public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                                    if (o1.getValue() > o2.getValue()) return 1;
                                    else if (o1.getValue() == o2.getValue()) return 0;
                                    else return -1;
                                }
                            });
                            for (Map.Entry<Integer, Double> entry : list)
                                sortedOptimalMemOptions.add(entry.getKey());

                            if (sortedOptimalMemOptions.isEmpty())
                                break;
                            functionsToBeRevisited.add(mostSignificantFunction);
                            skipDfsFlag = false;
                            for (int ms : sortedOptimalMemOptions) {
                                if (this.avgCostMap.get(mostSignificantFunction).get(ms) > earlyRejectMap.get(mostSignificantFunction) * earlyRejectThreshold)
                                    continue;
                                memConf = new TreeMap();
                                memConf.put(mostSignificantFunction, ms);
                                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                                double costOfMs = perfOpt.getApp().GetAverageCost();
                                if (costOfMs > budgetConstraint) {
                                    ineligibleMemOptionsMap.get(mostSignificantFunction).add(ms);
                                    if (earlyRejectMap.get(mostSignificantFunction) > this.avgCostMap.get(mostSignificantFunction).get(ms))
                                        earlyRejectMap.put(mostSignificantFunction, this.avgCostMap.get(mostSignificantFunction).get(ms));
                                    continue;
                                } else {
                                    msFlag = true;
                                    break;
                                }
                            }
                            if (!msFlag) {
                                memConf = new TreeMap();
                                memConf.put(mostSignificantFunction, mem);
                                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                            }
                        }
                        break;
                    } else break;
                } else {
                    constraintPreviouslySatisfiedFlag = true;
                    if (costOfMem < minimumNonMsiCost) {
                        minimumNonMsiCost = costOfMem;
                        minimumNonMsiMem = mem;
                        nextMostSignificantFunction = newMostSignificantFunction;
                    }
                }
            }

            if (msFlag) continue;
            TreeMap memConf = new TreeMap();
            if (minimumNonMsiMem == previousMem) {
                memConf.put(mostSignificantFunction, previousMem);
                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                isOptimized.put(mostSignificantFunction, true);
                skipDfsFlag = false;
            } else {
                memConf.put(mostSignificantFunction, minimumNonMsiMem);
                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                if (constraintPreviouslySatisfiedFlag) skipDfsFlag = true;
                else skipDfsFlag = false;
            }
            loopTime = System.currentTimeMillis();
            break;
        }

        for (WVertex function : functionsToBeRevisited) {
            boolean msFlag = false;
            int previousMem = function.getMem();
            ArrayList<Integer> eligibleMemOptions = getEligibleMemOptions(function, previousMem, ineligibleMemOptionsMap);
            if (eligibleMemOptions.isEmpty()) continue;

            for (int mem : eligibleMemOptions) {
                TreeMap<WVertex, Integer> memConf = new TreeMap<>();
                memConf.put(function, mem);
                perfOpt.update_App_workflow_mem_rt_cost(memConf);
                double costOfMem = perfOpt.getApp().GetAverageCost();
                if (costOfMem > budgetConstraint)
                    ineligibleMemOptionsMap.get(function).add(mem);
                else {
                    msFlag = true;
                    break;
                }
            }
            if (!msFlag) {
                TreeMap<WVertex, Integer> memConf = new TreeMap<>();
                memConf.put(function, previousMem);
                perfOpt.update_App_workflow_mem_rt_cost(memConf);
            }
        }
        long endTime = System.currentTimeMillis();

        for (WVertex function : perfOpt.getApp().getGraph().getDirectedGraph().vertexSet())
            configuration.put(function, function.getMem());
        StringBuffer sb = new StringBuffer("");
        for (Map.Entry<WVertex,Integer> entry : configuration.entrySet())
            sb.append(entry.getKey().toString() + ":" + entry.getValue().toString() + "  ");
        double rt = perfOpt.getApp().GetAverageRT();
        double cost = perfOpt.getApp().GetAverageCost();

        System.out.println("Repeated times: " + iter);
        System.out.println("Optimized Memory Configuration: " + sb);
        System.out.println("Budget constraint: " + budgetConstraint +" USD.");
        System.out.println("Average end-to-end response time:" + rt +" ms.");
        System.out.println("Average Cost:" + cost + " USD.");
        System.out.println("Time: " + (double) (endTime - startTime) / 1000 + " s");
        System.out.println("DFBA_BCPO Optimization Completed.");
        DFBA_Result dfbaResult = new DFBA_Result(budgetConstraint,rt,cost,configuration);
        return dfbaResult;
    }

    public void initializeWithMinimumCostConfiguration() {
        Set<WVertex> vertices = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet();
        TreeMap<WVertex, Integer> memConfiguration = new TreeMap<>();
        for (WVertex aVertex : vertices) {
            int[] available_mem_list = aVertex.getAvailable_mem_list();
            double minCost = Double.MAX_VALUE;
            int memUnderMinCost = 0;
            for (int mem : available_mem_list) {
                double costUnderMem = this.avgCostMap.get(aVertex).get(mem);
                if (costUnderMem < minCost) {
                    minCost = costUnderMem;
                    memUnderMinCost = mem;
                }
            }
            memConfiguration.put(aVertex, memUnderMinCost);
        }

        this.perfOpt.update_App_workflow_mem_rt_cost(memConfiguration);
    }

    public void initializeWithBestPerformanceConfiguration() {
        Set<WVertex> vertices = this.perfOpt.getApp().getGraph().getDirectedGraph().vertexSet();
        TreeMap<WVertex, Integer> memConfiguration = new TreeMap<>();
        for (WVertex aVertex : vertices) {
            int[] available_mem_list = aVertex.getAvailable_mem_list();
            double minRT = Double.MAX_VALUE;
            int memUnderMinRT = 0;
            for (int mem : available_mem_list) {
                double rtUnderMem = aVertex.getPerf_profile().get(mem);
                if (rtUnderMem < minRT) {
                    minRT = rtUnderMem;
                    memUnderMinRT = mem;
                }
            }
            memConfiguration.put(aVertex, memUnderMinRT);
        }

        this.perfOpt.update_App_workflow_mem_rt_cost(memConfiguration);
    }

    public long DFBA_OPT(double[] constraints, String OPTType,int iter) {
        ArrayList<DFBA_Result> results = new ArrayList<>();
        DFBA.OPTType = OPTType;
        long DFBAStartTime = System.currentTimeMillis();
        for (double constraint : constraints) {
            DFBA_Result tempResult = null;
            if (OPTType.equals("BCPO")) {
                tempResult = this.DFBA_BCPO(constraint, iter);
            } else if (OPTType.equals("PCCO")) {
                tempResult = this.DFBA_PCCO(constraint, iter);
            }
            results.add(tempResult);
        }
        long DFBAEndTime = System.currentTimeMillis();

        DataStoreTools.DFBADataStore(results, this.perfOpt.getApp().getGraph().getNode_num(), OPTType, iter);
        return (DFBAEndTime - DFBAStartTime) / constraints.length;
    }
}
