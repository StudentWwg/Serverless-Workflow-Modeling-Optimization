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
    private double[] node_delay;  //用户自定义的node_delay
    private double[] edge_delay;
    private WVertex start;
    private WVertex end;

    public String getAPPName() {
        return APPName;
    }

    public void setAPPName(String APPName) {
        this.APPName = APPName;
    }

    public int getNode_num() {
        return node_num;
    }

    public void setNode_num(int node_num) {
        this.node_num = node_num;
    }

    public DefaultDirectedWeightedGraph<WVertex, WEdge> getDirectedGraph() {
        return directedGraph;
    }

    public void setDirectedGraph(DefaultDirectedWeightedGraph<WVertex, WEdge> directedGraph) {
        this.directedGraph = directedGraph;
    }

    public double[] getNode_delay() {
        return node_delay;
    }

    public void setNode_delay(double[] node_delay) {
        this.node_delay = node_delay;
    }

    public double[] getEdge_delay() {
        return edge_delay;
    }

    public void setEdge_delay(double[] edge_delay) {
        this.edge_delay = edge_delay;
    }

    public WVertex getStart() {
        return start;
    }

    public void setStart(WVertex start) {
        this.start = start;
    }

    public WVertex getEnd() {
        return end;
    }

    public void setEnd(WVertex end) {
        this.end = end;
    }

    public APPGraph(String jsonPath) {
        this.APPName = new String("");
        // key是结点类型，value是边类型
        this.directedGraph = new DefaultDirectedWeightedGraph<WVertex, WEdge>(WEdge.class);
        this.graphGenerate(jsonPath);
        this.node_delay = new double[this.node_num];
        for (int i = 0; i < this.node_num; i++)
            this.node_delay[i] = 3.2;    //每个结点的nodeDelay和edgeDelay
        int edge_num = this.directedGraph.edgeSet().size();
        this.edge_delay = new double[edge_num];
        for (int i = 0; i < edge_num; i++)
            this.edge_delay[i] = 3.2;
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
        sb.append("}");  //dot文件的格式字符串
        //将dot字符串写入文件中
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
            //生成dot文件后可通过cmd生成dot图
            /* windows系统通过cmd执行dot命令
            Runtime rt = Runtime.getRuntime();
            //执行多条cmd命令可通过&&连接
            String cdBinDirectory = "cmd /c  D: && cd ";   //先进入dot.exe所在的bin文件夹再执行dot命令
            String dotExePath = " D:\\software\\Graphviz\\bin ";
            cdBinDirectory = cdBinDirectory + dotExePath + " && ";  //执行多条命令要使用&&隔开
            String exeCommand = cdBinDirectory + "dot -Tpng " + dotFilePath + " -o " + dotPicturePath;
            Process process = rt.exec(exeCommand);
            int status = process.waitFor();
            if (status == 0) System.out.println(this.APPName + "  DOT图绘制成功！");
            else System.out.println(this.APPName + "  DOT图绘制失败！");
            */

            /* mac通过terminal执行dot命令*/
            String exeCommand = "dot " + dotFilePath + " -T png " + " -o " + dotPicturePath;
            Process process = Runtime.getRuntime().exec(exeCommand);
            int status = process.waitFor();
            if (status == 0) System.out.println(this.APPName + "  DOT图绘制成功！");
            else System.out.println(this.APPName + "  DOT图绘制失败！");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(System.out);
        }
    }

    public void graphGenerate(String jsonPath) {
        try {
            String jsonContent = FileUtils.readFileToString(new File(jsonPath), "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonContent);
            this.APPName = jsonObject.getString("name");
            JSONArray vertices = jsonObject.getJSONArray("vertices");
            this.node_num = vertices.length();
            HashMap<String, WVertex> map = new HashMap<String, WVertex>();
            for (int i = 0; i < this.node_num; i++) {
                WVertex v = new WVertex(vertices.getString(i));  //顶点和边都由自己定义的类实现，不使用DefaultDirectedWeightedGraph中自带的边的权重
                if (i == 0) this.start = v;
                if (i == 7) this.end = v;
                this.directedGraph.addVertex(v);
                map.put(vertices.getString(i), v);  //HashMap用于下面添加边时查找结点
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
