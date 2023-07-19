package serverlessWorkflow.graph;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.io.*;
import java.util.HashMap;

public class APPGraph {
    private String APPName;
    private int node_num = 0;
    private DefaultDirectedWeightedGraph<WVertex, WEdge> directedGraph;
    private double[] node_delay;
    private double[] edge_delay;
    private WVertex start;
    private WVertex end;

    public String getAPPName() {
        return APPName;
    }

    public int getNode_num() {
        return node_num;
    }

    public DefaultDirectedWeightedGraph<WVertex, WEdge> getDirectedGraph() {
        return directedGraph;
    }

    public double[] getNode_delay() {
        return node_delay;
    }

    public double[] getEdge_delay() {
        return edge_delay;
    }

    public WVertex getStart() {
        return start;
    }

    public WVertex getEnd() {
        return end;
    }

    public void setEnd(WVertex end) {
        this.end = end;
    }

    public APPGraph(String jsonPath) {
        this.APPName = new String("");
        this.directedGraph = new DefaultDirectedWeightedGraph<WVertex, WEdge>(WEdge.class);
        this.graphGenerate(jsonPath);
        this.node_delay = new double[this.node_num];
        int edge_num = this.directedGraph.edgeSet().size();
        this.edge_delay = new double[edge_num];
    }

    public void drawGraph() {
        WVertex[] vertices = this.directedGraph.vertexSet().toArray(new WVertex[0]);
        WEdge[] edges = this.directedGraph.edgeSet().toArray(new WEdge[0]);
        StringBuffer sb = new StringBuffer("digraph ");
        sb.append(this.APPName + " {\n");

        for (int i = 0; i < vertices.length; i++)
            sb.append(vertices[i].toString() + " [label=\"" + vertices[i].toString() + "\"];\n");
        for (int i = 0; i < edges.length; i++)
            sb.append(edges[i].getV1().toString() + " -> " + edges[i].getV2().toString() + " [label=" + edges[i].getWeight() + "];\n");
        sb.append("}");
        String dotFilePath = null;
        String dotPicturePath = null;
        try {
            dotFilePath = new File("").getCanonicalPath() + "/src/main/resources/dot_file/" + this.APPName + ".dot";
            File dotFile = new File(dotFilePath);
            FileWriter writer = new FileWriter(dotFile, false);
            writer.write(sb.toString());
            writer.flush();
            writer.close();
            dotPicturePath = new File("").getCanonicalPath() + "/src/main/resources/app_graph_pictures/" + this.APPName + ".png";
            String exeCommand = "dot " + dotFilePath + " -T png " + " -o " + dotPicturePath;
            Process process = Runtime.getRuntime().exec(exeCommand);
            int status = process.waitFor();
            if (status == 0) System.out.println(this.APPName + "  DOT graph draws successfully!");
            else System.out.println(this.APPName + "  DOT graph drawing failed!");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void graphGenerate(String jsonPath) {
        try {
            String jsonContent = FileUtils.readFileToString(new File(jsonPath), "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonContent);
            this.APPName = jsonObject.getString("name");
            JSONArray vertices = jsonObject.getJSONArray("vertices");
            JSONArray taskTypes = jsonObject.getJSONArray("task types");
            this.node_num = vertices.length();
            HashMap<String, WVertex> map = new HashMap<String, WVertex>();
            for (int i = 0; i < this.node_num; i++) {
                WVertex v = new WVertex(vertices.getString(i), taskTypes.getString(i));
                if (i == 0) this.start = v;
                if (i == 9) this.end = v;
                this.directedGraph.addVertex(v);
                map.put(vertices.getString(i), v);
            }
            JSONArray edges = jsonObject.getJSONArray("edges");
            for (int i = 0; i < edges.length(); i++) {
                WVertex startVertex = map.get(edges.getJSONObject(i).getString("startVertex"));
                WVertex endVertex = map.get(edges.getJSONObject(i).getString("endVertex"));
                this.directedGraph.addEdge(startVertex, endVertex, new WEdge(startVertex, endVertex, edges.getJSONObject(i).getDouble("weight")));
            }
            this.drawGraph();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
}
