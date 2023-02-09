package AWSLambda.PerfCostModelAccuracy;

import AWSLambda.FunctionsMonitor.Monitor;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import serverlessWorkflow.graph.APPGraph;
import serverlessWorkflow.graph.WVertex;
import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;
import serverlessWorkflow.PerformanceAndCostModel.ServerlessAppWorkflow;

import java.io.*;
import java.util.*;

public class Accuracy {
    public static double getAvgDurationOfStateMachine(String APPName) {
//        String durationFilePath  = null;
        double avgDuration = 0;
        try {
            File stateMachineInvokeResult = new File(new File("").getCanonicalPath() +
                    "/src/main/resources/AWSLambda_StateMachine_invoke_results/AWSLambda_StateMachine_" + APPName + "_Logs.xls");
            FileInputStream inputStream = new FileInputStream(stateMachineInvokeResult.getPath());
            HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
            HSSFSheet sheet = workbook.getSheet("StateMachine_" + APPName + "_logs");
            int rowNumbers = sheet.getLastRowNum();
            System.out.println("StateMachine " +APPName+ " executed " + rowNumbers + " times.");
            double[] durationsOfAPP = new double[rowNumbers];
            for (int i = 1; i <= rowNumbers; i++) {
                HSSFRow aRow = sheet.getRow(i);
                double aDuration = aRow.getCell(2).getNumericCellValue();
                durationsOfAPP[i - 1] = aDuration;
            }
            OptionalDouble avgDurationDouble = Arrays.stream(durationsOfAPP).average();
            avgDuration = avgDurationDouble.getAsDouble();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return avgDuration;
    }

    public static double getAvgCostOfStateMachine(String APPName, String startTime, String endTime, int repeatedTimes) {
        double cost = 0;
        int funcNum = Integer.valueOf(APPName.split("APP")[1]);
        try {
            for (int i = 1; i <= funcNum; i++) { //计算所有函数的cost之和
                String logGroupName = "/aws/lambda/f" + i;
                Monitor.getAmazonCloudWatchLogs(logGroupName, startTime, endTime, APPName);  //先获取函数执行日志
                String funcLogPath = new File("").getCanonicalPath() +
                        "/src/main/resources/AWSLambda_functions_invoke_results_got_by_cloudwatchlog/"+APPName+"/AWSLambda_f" + i + "_Logs.xls";
                FileInputStream inputStream = new FileInputStream(funcLogPath);
                HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
                HSSFSheet sheet = workbook.getSheet("f" + i + "_logs");
                int rowNums = sheet.getLastRowNum();
                System.out.println("AWS Lambda function f" +i + " executed " + rowNums +" times.");
                for (int j = 1; j <= rowNums; j++) {
                    HSSFRow aRow = sheet.getRow(j);
                    double rt = aRow.getCell(5).getNumericCellValue();
                    double memSize = aRow.getCell(2).getNumericCellValue();
                    cost += (rt / 1000 * memSize / 1024 * 0.00002292 + 0.00000028) * 1000000;
                }
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        cost = cost/repeatedTimes;
        return cost;
    }

    public static double getAvgDurationOfPerfCostModel(TreeMap<String, Integer> memConfig, String APPName) {
        PerfOpt perfOpt = Accuracy.generatePerfOPTAndUpdateMemConfig(memConfig,APPName);
        double duration = perfOpt.getApp().GetAverageRT();
        return duration;
    }

    public static double getAvgCostOfPerfCostModel(TreeMap<String, Integer> memConfig, String APPName) {
        PerfOpt perfOpt = Accuracy.generatePerfOPTAndUpdateMemConfig(memConfig,APPName);
        double cost = perfOpt.getApp().GetAverageCost();
        return  cost;
    }

    public static  PerfOpt generatePerfOPTAndUpdateMemConfig(TreeMap<String, Integer> memConfig, String APPName){
        String jsonPath = null;
        try{
            jsonPath = new File("").getCanonicalPath() + "/src/main/resources/serverless_workflow_json_files/" + APPName+".json";
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }

        APPGraph appGraph = new APPGraph(jsonPath);
        ServerlessAppWorkflow App = new ServerlessAppWorkflow(appGraph,"SFN","AWS",null,-1,-1);
        PerfOpt perfOpt = new PerfOpt(App, true, null);
        TreeMap<WVertex, Integer> updateMemConfig = new TreeMap<WVertex, Integer>();
        WVertex[] vertices = perfOpt.getApp().getGraph().getDirectedGraph().vertexSet().toArray(new WVertex[0]);
        for(int i=0;i< vertices.length;i++){
            int memSize = 0;
            for(String functionName : memConfig.keySet()){
                if(functionName.replaceFirst("f","v").equals(vertices[i].getVertexInfo())){
                    memSize = memConfig.get(functionName);
                    updateMemConfig.put(vertices[i], memSize);
                    break;
                }
            }
            updateMemConfig.put(vertices[i], memSize);
        }
        perfOpt.update_App_workflow_mem_rt_cost(updateMemConfig);
        return perfOpt;
    }
}
