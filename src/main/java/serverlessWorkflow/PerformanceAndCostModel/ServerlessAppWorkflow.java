package serverlessWorkflow.PerformanceAndCostModel;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.jgrapht.GraphPath;
import serverlessWorkflow.graph.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ServerlessAppWorkflow {
    private double pgs;
    private double ppr;
    private String platform;
    private APPGraph Graph;
    private long sizeOfS3Object;

    public APPGraph getGraph() {
        return Graph;
    }

    public void setGraph(APPGraph g) {
        Graph = g;
    }

    public ServerlessAppWorkflow(APPGraph G, String delayType, String platform, double PGS, double PPR) {
        this.Graph = G;
        Set<WVertex> vertexSet = G.getDirectedGraph().vertexSet();
        Set<WEdge> edgeSet = G.getDirectedGraph().edgeSet();
        if (delayType.equals("None")) {
            for (WVertex vertex : vertexSet)
                vertex.setNode_delay(0);
            for (WEdge edge : edgeSet)
                edge.setEdge_delay(0);
        } else if (delayType.equals("SFN")) {
            for (WVertex vertex : vertexSet)
                vertex.setNode_delay(100);
            for (WEdge edge : edgeSet)
                edge.setEdge_delay(70);
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

        if (PGS == -1)
            this.pgs = 0.0000166667;
        else
            this.pgs = PGS;
        if (PPR == -1)
            this.ppr = 0.0000002;
        else
            this.ppr = PPR;

        this.platform = platform;
        getSizeOfS3Object();
    }

    public ServerlessAppWorkflow(ServerlessAppWorkflow serverlessAppWorkflow){
        this.pgs = serverlessAppWorkflow.pgs;
        this.ppr = serverlessAppWorkflow.ppr;
        this.platform = serverlessAppWorkflow.platform;
        this.sizeOfS3Object = serverlessAppWorkflow.sizeOfS3Object;
        try{
            this.Graph = new APPGraph(new File("").getCanonicalPath() + "/src/main/resources/serverless_workflow_json_files/" +
                    serverlessAppWorkflow.Graph.getAPPName() + ".json");
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void getSizeOfS3Object(){
        String AWS_ACCESS_KEY = "AKIARQP66F75QZZF4AGW";
        String AWS_SECRET_KEY = "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";
        String bucketName = "serverless-network-intensive-source-bucket";
        AWSCredentials credentials = new BasicAWSCredentials(AWS_ACCESS_KEY,AWS_SECRET_KEY);
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).
                withRegion(Regions.US_EAST_1).build();
        // download all objects in S3 bucket
        ListObjectsV2Request v2Request = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result v2Result = s3.listObjectsV2(v2Request);
        long bytes = 0;
        for(S3ObjectSummary objectSummary : v2Result.getObjectSummaries()){
            String key = objectSummary.getKey();
            S3Object S3object = s3.getObject(new GetObjectRequest(bucketName,key));
            ObjectMetadata objectMetadata = S3object.getObjectMetadata();
            bytes += objectMetadata.getContentLength();
        }
        this.sizeOfS3Object = bytes;
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

        for (WEdge aEdge : edges)
            rt = rt + aEdge.getEdge_delay();
        for (WVertex aVertex : vertices)
            rt = rt + aVertex.getRt() + aVertex.getNode_delay();
        return rt;
    }

    public void UpdateVertexCost(WVertex vertex) {
        if (vertex.getTaskType().equals("Network I/O")) {
            double costOfNetworkTask = (vertex.getRt() / 1000 * vertex.getMem() / 1024 * this.pgs + this.ppr) * 10000000;
            costOfNetworkTask += (double) sizeOfS3Object * 10000000 / 1024 / 1024 * 0.09;
            vertex.setCost(costOfNetworkTask);
        } else
            vertex.setCost((vertex.getRt() / 1000 * vertex.getMem() / 1024 * this.pgs + this.ppr) * 10000000);
    }

    public double GetVertexCostInMem(WVertex vertex, int mem) {
        double rt = vertex.getPerf_profile().get(mem);
        double cost = (rt / 1000 * mem / 1024 * this.pgs + this.ppr) * 10000000;
        if (vertex.getTaskType().equals("Network I/O"))
            cost += (double) sizeOfS3Object * 10000000 / 1024 / 1024 * 0.09;
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

    public Set<PathsInGraph> GetExecutionInstanceBasedOnDepthFirstSearch(WVertex vertex) {
        if (vertex.equals(this.getGraph().getEnd())) {
            PathsInGraph pathsInGraph = new PathsInGraph();
            Set<PathsInGraph> returnValue = new HashSet<>();
            pathsInGraph.tp = 1.0;
            pathsInGraph.vertices.add(this.getGraph().getEnd());
            pathsInGraph.rt = this.getGraph().getEnd().getRt() + this.getGraph().getEnd().getNode_delay();
            pathsInGraph.cost = this.getGraph().getEnd().getCost();
            returnValue.add(pathsInGraph);
            return returnValue;
        } else {
            ArrayList<Set<PathsInGraph>> choice = new ArrayList<>();
            ArrayList<Set<PathsInGraph>> parallel = new ArrayList<>();
            Set<WEdge> outGoingEdges = this.getGraph().getDirectedGraph().outgoingEdgesOf(vertex);
            for (WEdge aOutGoingEdge : outGoingEdges) {
                WVertex targetVertex = this.getGraph().getDirectedGraph().getEdgeTarget(aOutGoingEdge);
                Set<PathsInGraph> pathsInGraph = GetExecutionInstanceBasedOnDepthFirstSearch(targetVertex);
                for (PathsInGraph path : pathsInGraph) {
                    path.vertices.add(vertex);
                    path.edges.add(aOutGoingEdge);
                    path.tp *= aOutGoingEdge.getWeight();
                    path.rt = path.rt + vertex.getRt() + vertex.getNode_delay() + aOutGoingEdge.getEdge_delay();
                    path.cost += vertex.getCost();
                }
                if ((1.0 - aOutGoingEdge.getWeight()) < Math.pow(10, -6))
                    parallel.add(pathsInGraph);
                else
                    choice.add(pathsInGraph);
            }


            Set<PathsInGraph> executionInstance = new HashSet<>();
            if (parallel.size() != 0) {
                for (int i = 0; i < parallel.size(); i++) {
                    if (i == 0) {
                        Set<PathsInGraph> pathsInGraphs = parallel.get(0);
                        for (PathsInGraph aPathsInGraph : pathsInGraphs)
                            executionInstance.add(aPathsInGraph);
                    } else {
                        Set<PathsInGraph> tempExecutionInstance = new HashSet<>();
                        Set<PathsInGraph> pathsInGraphs = parallel.get(i);
                        for (PathsInGraph pathInExecutionInstance : executionInstance) {
                            for (PathsInGraph pathsInParalleli : pathsInGraphs) {
                                PathsInGraph aNewPath = new PathsInGraph();
                                aNewPath.tp = pathInExecutionInstance.tp * pathsInParalleli.tp;
                                aNewPath.rt = Math.max(pathInExecutionInstance.rt, pathsInParalleli.rt);
                                aNewPath.cost = pathInExecutionInstance.cost + pathsInParalleli.cost;

                                for (WVertex aVertex : pathInExecutionInstance.vertices)
                                    aNewPath.vertices.add(aVertex);
                                for (WEdge aEdge : pathInExecutionInstance.edges)
                                    aNewPath.edges.add(aEdge);
                                for (WVertex aVertex : pathsInParalleli.vertices) {
                                    if (aNewPath.vertices.contains(aVertex))
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

                if (choice.size() != 0) {
                    for (int i = 0; i < choice.size(); i++) {
                        Set<PathsInGraph> tempExecutionInstance = new HashSet<>();
                        Set<PathsInGraph> pathsInGraphs = choice.get(i);
                        for (PathsInGraph pathInExecutionInstance : executionInstance) {
                            for (PathsInGraph pathInChoicei : pathsInGraphs) {
                                PathsInGraph aNewPath = new PathsInGraph();
                                aNewPath.tp = pathInExecutionInstance.tp * pathInChoicei.tp;
                                aNewPath.rt = Math.max(pathInExecutionInstance.rt, pathInChoicei.rt);
                                aNewPath.cost = pathInExecutionInstance.cost + pathInChoicei.cost;

                                for (WVertex aVertex : pathInExecutionInstance.vertices)
                                    aNewPath.vertices.add(aVertex);
                                for (WEdge aEdge : pathInExecutionInstance.edges)
                                    aNewPath.edges.add(aEdge);
                                for (WVertex aVertex : pathInChoicei.vertices) {
                                    if (aNewPath.vertices.contains(aVertex))
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
            } else {
                for (int i = 0; i < choice.size(); i++) {
                    Set<PathsInGraph> pathsInGraphs = choice.get(i);
                    for (PathsInGraph aPathsInGraph : pathsInGraphs)
                        executionInstance.add(aPathsInGraph);
                }
            }
            return executionInstance;
        }
    }
}
