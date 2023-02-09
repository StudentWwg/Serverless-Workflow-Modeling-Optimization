package serverlessWorkflow.PerformanceAndCostModel;

import org.jgrapht.GraphPath;
import serverlessWorkflow.graph.*;

import java.util.*;

public class ServerlessAppWorkflow {
    private double pgs;
    private double ppr;
    private String platform;  //platform决定计算结点cost的计算方式
    private APPGraph Graph;

    public double getPgs() {
        return pgs;
    }

    public void setPgs(double pgs) {
        this.pgs = pgs;
    }

    public double getPpr() {
        return ppr;
    }

    public void setPpr(double ppr) {
        this.ppr = ppr;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public APPGraph getGraph() {
        return Graph;
    }

    public void setGraph(APPGraph g) {
        Graph = g;
    }

    public ServerlessAppWorkflow(APPGraph G, String delayType, String platform,
                                 TreeMap<Integer, Double> pricing_model, double PGS, double PPR) {
        this.setGraph(G);
        Set<WVertex> vertexSet = G.getDirectedGraph().vertexSet();
        Set<WEdge> edgeSet = G.getDirectedGraph().edgeSet();
        if (delayType.equals("None")) {
            for (WVertex vertex : vertexSet)
                vertex.setNode_delay(0);
            for (WEdge edge : edgeSet)
                edge.setEdge_delay(0);
        } else if (delayType.equals("SFN")) {
            for (WVertex vertex : vertexSet)
                vertex.setNode_delay(50);
            for (WEdge edge : edgeSet)
                edge.setEdge_delay(1);
        } else if (delayType.equals("Defined")) {
            double[] node_delay = G.getNode_delay();
            WVertex[] vertexs = vertexSet.toArray(new WVertex[0]);
            for (int i = 0; i < this.getGraph().getNode_num(); i++)
                vertexs[i].setNode_delay(node_delay[i]);
            double[] edge_delay = G.getEdge_delay();
            WEdge[] edges = edgeSet.toArray(new WEdge[0]);
            for (int i = 0; i < edge_delay.length; i++)
                edges[i].setEdge_delay(edge_delay[i]);
        }

        if (PGS == -1)  //香港地区计费价格
            this.pgs = 0.00002292;
        else
            this.pgs = PGS;
        if (PPR == -1)
            this.ppr = 0.00000028;
        else
            this.ppr = PPR;

        this.platform = platform;
    }

    public double GetTPOfAPath(GraphPath<WVertex, WEdge> path) {
        double tp = 1.0;
        List<WEdge> edges = path.getEdgeList();
        for (WEdge edge : edges)
            tp = tp * edge.getWeight();
        return tp;
    }

    public double GetRTOfAPath(GraphPath<WVertex, WEdge> path) {
        double rt = 0.0;
        List<WEdge> edges = path.getEdgeList();
        List<WVertex> vertices = path.getVertexList();

        for(WEdge aEdge : edges)
            rt = rt + aEdge.getEdge_delay();
        for(WVertex aVertex : vertices)
            rt = rt + aVertex.getRt() + aVertex.getNode_delay();
        return rt;
    }

    public void UpdateVertexCost(WVertex vertex) {  //更新vertex的cost
        vertex.setCost((vertex.getRt() / 1000 * vertex.getMem() / 1024 * this.pgs + this.ppr) * 1000000);   //每一百万次调用的费用
    }

    public double GetVertexCostInMem(WVertex vertex, int mem) {
        Double rt = vertex.getPerf_profile().get(mem);
        double cost = (rt / 1000 * mem / 1024 * this.pgs + this.ppr) * 1000000;
        return cost;
    }

    public double GetAverageRT() {
        double ERT = 0;
        Set<PathsInGraph> allExecutionInstances = GetExecutionInstanceBasedOnDepthFirstSearch(this.getGraph().getStart());
        for (PathsInGraph executionInstance : allExecutionInstances)
            ERT += executionInstance.rt * executionInstance.tp;
        return ERT;
    }

    public double GetAverageCost() {
        double COST = 0;
        Set<PathsInGraph> allExecutionInstances = GetExecutionInstanceBasedOnDepthFirstSearch(this.getGraph().getStart());
        for (PathsInGraph executionInstance : allExecutionInstances)
            COST += executionInstance.cost * executionInstance.tp;
        return COST;
    }

    class PathsInGraph {
        double tp;
        Set<WVertex> vertices = new HashSet<>();
        Set<WEdge> edges = new HashSet<>();
        double rt;
        double cost;
    }

    //Set存放各条可能路径的集合
    public Set<PathsInGraph> GetExecutionInstanceBasedOnDepthFirstSearch(WVertex vertex) {
        if (vertex.equals(this.getGraph().getEnd())) {  //直到end node创建PathsInGraph，按照逆拓扑序列搜寻所有路径
            PathsInGraph pathsInGraph = new PathsInGraph();
            pathsInGraph.tp = 1.0;
            pathsInGraph.vertices.add(this.getGraph().getEnd());
            pathsInGraph.rt = this.getGraph().getEnd().getRt() + this.getGraph().getEnd().getNode_delay();
            pathsInGraph.cost = this.getGraph().getEnd().getCost();
            Set<PathsInGraph> returnValue = new HashSet<>();
            returnValue.add(pathsInGraph);
            return returnValue;
        } else {
            ArrayList<Set<PathsInGraph>> list = new ArrayList<>();
            WEdge[] outGoingEdges = this.getGraph().getDirectedGraph().outgoingEdgesOf(vertex).toArray(new WEdge[0]);
            for (int i = 0; i < outGoingEdges.length; i++) {
                WVertex targetVertex = this.getGraph().getDirectedGraph().getEdgeTarget(outGoingEdges[i]);
                Set<PathsInGraph> pathsInGraph = GetExecutionInstanceBasedOnDepthFirstSearch(targetVertex);

                for (PathsInGraph path : pathsInGraph) {
                    path.vertices.add(vertex);
                    path.edges.add(outGoingEdges[i]);
                    path.tp *= outGoingEdges[i].getWeight();
                    path.rt = path.rt + vertex.getRt() + vertex.getNode_delay() + outGoingEdges[i].getEdge_delay();
                    path.cost += vertex.getCost();
                }
                list.add(pathsInGraph);
            }

            //对返回的不同路径进行处理
            ArrayList<Set<PathsInGraph>> choice = new ArrayList<>();
            ArrayList<Set<PathsInGraph>> parallel = new ArrayList<>();
            Set<PathsInGraph> executionInstance = new HashSet<>();

            for(int i=0;i< list.size();i++){
                WEdge aFunOutEdge = outGoingEdges[i];
                if(aFunOutEdge.getWeight() ==1.0)
                    parallel.add(list.get(i));
                else
                    choice.add(list.get(i));
            }

            if(parallel.size()!=0){
                for(int i=0;i< parallel.size();i++){
                    if(i==0){
                        Set<PathsInGraph> pathsInGraphs = parallel.get(0);
                        for(PathsInGraph aPathsInGraph : pathsInGraphs)
                            executionInstance.add(aPathsInGraph);
                    }
                    else {
                        Set<PathsInGraph> tempExecutionInstance = new HashSet<>();
                        Set<PathsInGraph> pathsInGraphs = parallel.get(i);
                        for(PathsInGraph pathInExecutionInstance : executionInstance){
                            for(PathsInGraph pathsInParalleli : pathsInGraphs){
                                PathsInGraph aNewPath = new PathsInGraph();
                                aNewPath.tp = pathInExecutionInstance.tp * pathsInParalleli.tp;
                                aNewPath.rt = Math.max(pathInExecutionInstance.rt, pathsInParalleli.rt);
                                aNewPath.cost = pathInExecutionInstance.cost + pathsInParalleli.cost;

                                for(WVertex aVertex : pathInExecutionInstance.vertices)
                                    aNewPath.vertices.add(aVertex);
                                for(WEdge aEdge : pathInExecutionInstance.edges)
                                    aNewPath.edges.add(aEdge);
                                for(WVertex aVertex : pathsInParalleli.vertices){
                                    if(aNewPath.vertices.contains(aVertex))
                                        aNewPath.cost -= aVertex.getCost();
                                    else
                                        aNewPath.vertices.add(aVertex);
                                }
                                for (WEdge aEdge : pathsInParalleli.edges)
                                    aNewPath.edges.add(aEdge);
                                tempExecutionInstance.add(aNewPath);
                            }
                        }
                        executionInstance = tempExecutionInstance;
                    }
                }

                if(choice.size() !=0){
                    for( int i=0;i< choice.size();i++){
                        Set<PathsInGraph> tempExecutionInstance = new HashSet<>();
                        Set<PathsInGraph> pathsInGraphs = choice.get(i);
                        for(PathsInGraph pathInExecutionInstance : executionInstance){
                            for (PathsInGraph pathInChoicei : pathsInGraphs){
                                PathsInGraph aNewPath = new PathsInGraph();
                                aNewPath.tp = pathInExecutionInstance.tp * pathInChoicei.tp;
                                aNewPath.rt = Math.max(pathInExecutionInstance.rt, pathInChoicei.rt);
                                aNewPath.cost = pathInExecutionInstance.cost + pathInChoicei.cost;

                                for(WVertex aVertex : pathInExecutionInstance.vertices)
                                    aNewPath.vertices.add(aVertex);
                                for(WEdge aEdge : pathInExecutionInstance.edges)
                                    aNewPath.edges.add(aEdge);
                                for(WVertex aVertex : pathInChoicei.vertices){
                                    if(aNewPath.vertices.contains(aVertex))
                                        aNewPath.cost -= aVertex.getCost();
                                    else
                                        aNewPath.vertices.add(aVertex);
                                }
                                for (WEdge aEdge : pathInChoicei.edges)
                                    aNewPath.edges.add(aEdge);
                                tempExecutionInstance.add(aNewPath);
                            }
                        }
                        executionInstance = tempExecutionInstance;
                    }
                }
            }else {
                for(int i=0;i< choice.size();i++){
                    Set<PathsInGraph> pathsInGraphs = choice.get(i);
                    for(PathsInGraph aPathsInGraph : pathsInGraphs)
                        executionInstance.add(aPathsInGraph);
                }
            }
            return executionInstance;
        }
    }
}
