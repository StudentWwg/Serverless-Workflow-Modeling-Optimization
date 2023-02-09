package Main;

import PRCPG.PRCP;
import UWCG.UWC;
import CPOGA.CPOGA_SAL_OPT;
import serverlessWorkflow.graph.APPGraph;
import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;
import serverlessWorkflow.PerformanceAndCostModel.ServerlessAppWorkflow;
import util.DataStoreTools;
import util.Parameters;
import util.ProgramExecutionTimeComparison;
import util.ResultsVisualization;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;

public class Optimizer {
    private static String serverlessWorkflowDelayType;
    private static String serverlessWorkflowPlatform;
    private static int numOfGenesOfCPOGA;
    private static double crossRateOfCPOGA;
    private static double mutateRateOfCPOGA;
    private static int numOfGenerationsOfCPOGA;
    private static double ETA_M_OfCPOGA;
    private static int budgetNumberOfCPOGA;
    private static int performanceNumberOfCPOGA;
    private static double BCRthresholdOfPRCP;

    public static String getServerlessWorkflowDelayType() {
        return serverlessWorkflowDelayType;
    }

    public static void setServerlessWorkflowDelayType(String serverlessWorkflowDelayType) {
        Optimizer.serverlessWorkflowDelayType = serverlessWorkflowDelayType;
    }

    public static String getServerlessWorkflowPlatform() {
        return serverlessWorkflowPlatform;
    }

    public static void setServerlessWorkflowPlatform(String serverlessWorkflowPlatform) {
        Optimizer.serverlessWorkflowPlatform = serverlessWorkflowPlatform;
    }

    public static int getNumOfGenesOfCPOGA() {
        return numOfGenesOfCPOGA;
    }

    public static void setNumOfGenesOfCPOGA(int numOfGenesOfCPOGA) {
        Optimizer.numOfGenesOfCPOGA = numOfGenesOfCPOGA;
    }

    public static double getCrossRateOfCPOGA() {
        return crossRateOfCPOGA;
    }

    public static void setCrossRateOfCPOGA(double crossRateOfCPOGA) {
        Optimizer.crossRateOfCPOGA = crossRateOfCPOGA;
    }

    public static double getMutateRateOfCPOGA() {
        return mutateRateOfCPOGA;
    }

    public static void setMutateRateOfCPOGA(double mutateRateOfCPOGA) {
        Optimizer.mutateRateOfCPOGA = mutateRateOfCPOGA;
    }

    public static int getNumOfGenerationsOfCPOGA() {
        return numOfGenerationsOfCPOGA;
    }

    public static void setNumOfGenerationsOfCPOGA(int numOfGenerationsOfCPOGA) {
        Optimizer.numOfGenerationsOfCPOGA = numOfGenerationsOfCPOGA;
    }

    public static double getETA_M_OfCPOGA() {
        return ETA_M_OfCPOGA;
    }

    public static void setETA_M_OfCPOGA(double ETA_M_OfCPOGA) {
        Optimizer.ETA_M_OfCPOGA = ETA_M_OfCPOGA;
    }

    public static int getBudgetNumberOfCPOGA() {
        return budgetNumberOfCPOGA;
    }

    public static void setBudgetNumberOfCPOGA(int budgetNumberOfCPOGA) {
        Optimizer.budgetNumberOfCPOGA = budgetNumberOfCPOGA;
    }

    public static int getPerformanceNumberOfCPOGA() {
        return performanceNumberOfCPOGA;
    }

    public static void setPerformanceNumberOfCPOGA(int performanceNumberOfCPOGA) {
        Optimizer.performanceNumberOfCPOGA = performanceNumberOfCPOGA;
    }

    public static double getBCRthresholdOfPRCP() {
        return BCRthresholdOfPRCP;
    }

    public static void setBCRthresholdOfPRCP(double BCRthresholdOfPRCP) {
        Optimizer.BCRthresholdOfPRCP = BCRthresholdOfPRCP;
    }

    public static double[] OptimizationUnderConstraintInFiveStrategies(String jsonPath, int repeatedTimes) {
        Parameters.GetParameters();
        APPGraph graph = new APPGraph(jsonPath);
        ServerlessAppWorkflow App = new ServerlessAppWorkflow(graph, Optimizer.serverlessWorkflowDelayType, Optimizer.serverlessWorkflowPlatform, null, -1, -1);
        PerfOpt perfOpt = new PerfOpt(App, true, null);
        CPOGA_SAL_OPT ga = new CPOGA_SAL_OPT(Optimizer.numOfGenesOfCPOGA, Optimizer.crossRateOfCPOGA, Optimizer.mutateRateOfCPOGA,
                Optimizer.numOfGenerationsOfCPOGA, Optimizer.ETA_M_OfCPOGA, Optimizer.budgetNumberOfCPOGA, Optimizer.performanceNumberOfCPOGA, perfOpt);
        PRCP prcp = new PRCP(perfOpt);
        UWC uwc = new UWC(perfOpt);
        double maxRTUnderMinimalMem = perfOpt.getMaximal_avg_rt(), minRtUnderMaximalMem = perfOpt.getMinimal_avg_rt(),
                maxCostUnderMaximalMem = perfOpt.getMaximal_cost(), minCostUnderMinimalMem = perfOpt.getMinimal_cost();
        System.out.println("maxRT = " + maxRTUnderMinimalMem + ",  maxCost = " + maxCostUnderMaximalMem + ", minRT = " + minRtUnderMaximalMem + ", minCost = " + minCostUnderMinimalMem);
        double[] budgetConstraint = new double[ga.getBUDGET_NUMBER()];
        double[] perfConstraint = new double[ga.getPERFORMANCE_NUMBER()];
//        for (int i = 0; i < ga.BUDGET_NUMBER; i++)
//            budgetConstraint[i] = (maxCostUnderMaximalMem - minCostUnderMinimalMem) / 5 / (ga.BUDGET_NUMBER) * i + minCostUnderMinimalMem;
//        for (int i = 0; i < ga.PERFORMANCE_NUMBER; i++)
//            perfConstraint[i] = (maxRTUnderMinimalMem - minRtUnderMaximalMem) / 3 / (ga.PERFORMANCE_NUMBER) * i + minRtUnderMaximalMem;
        for (int i = 1; i <= ga.getBUDGET_NUMBER(); i++)
            budgetConstraint[i-1] = (maxCostUnderMaximalMem - minCostUnderMinimalMem) / ga.getBUDGET_NUMBER() * i + minCostUnderMinimalMem;
        for (int i = 1; i <= ga.getPERFORMANCE_NUMBER(); i++)
            perfConstraint[i-1] = (maxRTUnderMinimalMem - minRtUnderMaximalMem) / ga.getPERFORMANCE_NUMBER() * i + minRtUnderMaximalMem;

        long CPOGAExecutionTimeOfBCPO = ga.CPOGASearch(budgetConstraint, "BCPO",repeatedTimes);
        long CPOGAExecutionTimeOfPCCO = ga.CPOGASearch(perfConstraint, "PCCO",repeatedTimes);
        long PRCPExecutionTimeOfBCPO = prcp.PRCP_OPT(budgetConstraint, "BCPO", Optimizer.BCRthresholdOfPRCP,repeatedTimes);
        long PRCPExecutionTimeOfPCCO = prcp.PRCP_OPT(perfConstraint, "PCCO", Optimizer.BCRthresholdOfPRCP,repeatedTimes);
        long UWCExecutionTimeOfBCPO = uwc.UWC_OPT(budgetConstraint,"BCPO",repeatedTimes);
        long UWCExecutionTimeOfPCCO = uwc.UWC_OPT(perfConstraint,"PCCO",repeatedTimes);
        ProgramExecutionTimeComparison.WriteExecutionTimeToFile(CPOGAExecutionTimeOfBCPO,CPOGAExecutionTimeOfPCCO,
                PRCPExecutionTimeOfBCPO,PRCPExecutionTimeOfPCCO,UWCExecutionTimeOfBCPO, UWCExecutionTimeOfPCCO, repeatedTimes);
        ResultsVisualization.DrawPictures(perfOpt,repeatedTimes);  //算法优化结果可视化
        double[] BCPOPCCOSuccessRate = ResultsVisualization.AlgorithmComparisonDigitization();
        return BCPOPCCOSuccessRate;
    }
    public static void OptimizationOfAllServerlessWorkflow() {
        long startTime = System.currentTimeMillis(); //获取开始时间
        double[][] successRate = new double[10][2];
        for(int repeatedTimes = 0;repeatedTimes< successRate.length;repeatedTimes++){
            String projectPath = null, jsonDirectoryPath = null, jsonFilePath = null;
            File jsonDirectory = null, jsonFile = null;
            try {
                projectPath = new File("").getCanonicalPath();
                jsonDirectoryPath = projectPath + "/src/main/resources/serverless_workflow_json_files/";
                jsonDirectory = new File(jsonDirectoryPath);
                if (!jsonDirectory.isDirectory())  //如果不是文件夹果断退出
                    System.exit(1);
                File[] files = jsonDirectory.listFiles();
                double[] BCPOPercent = new double[files.length];
                double[] PCCOPercent = new double[files.length];
                for (int i = 0; i < files.length; i++) {
                    jsonFile = files[i];
                    jsonFilePath = jsonFile.getAbsolutePath();
                    double[] results = Optimizer.OptimizationUnderConstraintInFiveStrategies(jsonFilePath,repeatedTimes+1);
                    BCPOPercent[i] = results[0];
                    PCCOPercent[i] = results[1];
                }
                successRate[repeatedTimes][0] = Arrays.stream(BCPOPercent).average().getAsDouble();
                successRate[repeatedTimes][1] = Arrays.stream(PCCOPercent).average().getAsDouble();
            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
        }
        DataStoreTools.WriteSuccessRateToFile(successRate);
        long endTime = System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间：" + new DecimalFormat("0.00").format((double)(endTime - startTime) / 1000/3600) + "h"); //输出程序运行时间
    }


}
