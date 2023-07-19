package Main;

import DFBA.DFBA;
import PRCP.PRCP;
import UWC.UWC;
import EASW.EASW_SAL_OPT;
import serverlessWorkflow.graph.APPGraph;
import serverlessWorkflow.PerformanceAndCostModel.PerfOpt;
import serverlessWorkflow.PerformanceAndCostModel.ServerlessAppWorkflow;
import util.DataStoreTools;
import util.Parameters;
import util.ProgramExecutionTimeComparison;
import util.ResultsVisualization;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Optimizer {
    private static String serverlessWorkflowDelayType;
    private static String serverlessWorkflowPlatform;
    private static int numOfGenesOfEASW;
    private static double crossRateOfEASW;
    private static double mutationRateOfEASW;
    private static int numOfGenerationsOfEASW;
    private static double ETA_M_OfEASW;
    private static int budgetNumberOfEASW;
    private static int performanceNumberOfEASW;
    private static double BCRthresholdOfPRCP;

    public static void setServerlessWorkflowDelayType(String serverlessWorkflowDelayType) {
        Optimizer.serverlessWorkflowDelayType = serverlessWorkflowDelayType;
    }

    public static void setServerlessWorkflowPlatform(String serverlessWorkflowPlatform) {
        Optimizer.serverlessWorkflowPlatform = serverlessWorkflowPlatform;
    }

    public static void setNumOfGenesOfEASW(int numOfGenesOfEASW) {
        Optimizer.numOfGenesOfEASW = numOfGenesOfEASW;
    }

    public static void setCrossRateOfEASW(double crossRateOfEASW) {
        Optimizer.crossRateOfEASW = crossRateOfEASW;
    }

    public static void setMutationRateOfEASW(double mutationRateOfEASW) {
        Optimizer.mutationRateOfEASW = mutationRateOfEASW;
    }

    public static void setNumOfGenerationsOfEASW(int numOfGenerationsOfEASW) {
        Optimizer.numOfGenerationsOfEASW = numOfGenerationsOfEASW;
    }

    public static void setETA_M_OfEASW(double ETA_M_OfEASW) {
        Optimizer.ETA_M_OfEASW = ETA_M_OfEASW;
    }

    public static void setBudgetNumberOfEASW(int budgetNumberOfEASW) {
        Optimizer.budgetNumberOfEASW = budgetNumberOfEASW;
    }

    public static void setPerformanceNumberOfEASW(int performanceNumberOfEASW) {
        Optimizer.performanceNumberOfEASW = performanceNumberOfEASW;
    }

    public static void setBCRthresholdOfPRCP(double BCRthresholdOfPRCP) {
        Optimizer.BCRthresholdOfPRCP = BCRthresholdOfPRCP;
    }

    public static double[] OptimizationUnderConstraint(String jsonPath, double[] executionTime, int iter) {
        Parameters.GetParameters();

        APPGraph graph = new APPGraph(jsonPath);
        ServerlessAppWorkflow App = new ServerlessAppWorkflow(graph, Optimizer.serverlessWorkflowDelayType, Optimizer.serverlessWorkflowPlatform, -1, -1);
        PerfOpt perfOpt = new PerfOpt(App, true, null);

        double maxRTUnderMinimalMem = perfOpt.getRT_under_minimal_mem_configuration(),
                minRtUnderMaximalMem = perfOpt.getRT_under_maximal_mem_configuration(),
                maxCostUnderMaximalMem = perfOpt.getCost_under_maximal_mem_configuration(),
                minCostUnderMinimalMem = perfOpt.getCost_under_minimal_mem_configuration();
        System.out.println("maxRT = " + new BigDecimal(maxRTUnderMinimalMem).setScale(2, RoundingMode.HALF_UP) + ",  maxCost = " +
                new BigDecimal(maxCostUnderMaximalMem).setScale(2, RoundingMode.HALF_UP) + ", minRT = " +
                new BigDecimal(minRtUnderMaximalMem).setScale(2, RoundingMode.HALF_UP) + ", minCost = " +
                new BigDecimal(minCostUnderMinimalMem).setScale(2, RoundingMode.HALF_UP));
        double[] budgetConstraint = new double[Optimizer.budgetNumberOfEASW];
        double[] perfConstraint = new double[Optimizer.performanceNumberOfEASW];

        double step = 250 / Optimizer.budgetNumberOfEASW;
        for (int i = 0; i < Optimizer.budgetNumberOfEASW; i++) {
            budgetConstraint[i] = minCostUnderMinimalMem + step * (i + 1);
        }
        step = 300 / (Optimizer.performanceNumberOfEASW);
        for (int i = 0; i < Optimizer.performanceNumberOfEASW; i++) {
            perfConstraint[i] = minRtUnderMaximalMem + step * (i + 1);
        }

        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(new Thread(new Runnable() {
            @Override
            public void run() {
                APPGraph graph = new APPGraph(jsonPath);
                ServerlessAppWorkflow App = new ServerlessAppWorkflow(graph, Optimizer.serverlessWorkflowDelayType, Optimizer.serverlessWorkflowPlatform, -1, -1);
                PerfOpt perfOptOfEASW = new PerfOpt(App, true, null);

                EASW_SAL_OPT EASW = new EASW_SAL_OPT(Optimizer.numOfGenesOfEASW, Optimizer.crossRateOfEASW, Optimizer.mutationRateOfEASW,
                        Optimizer.numOfGenerationsOfEASW, Optimizer.ETA_M_OfEASW, perfOptOfEASW);
                perfOptOfEASW.update_App_workflow_mem_rt_cost(perfOptOfEASW.getMinimal_mem_configuration());
                long EASWExecutionTimeOfBCPO = EASW.EASWSearch(budgetConstraint, "BCPO", iter);
                perfOptOfEASW.update_App_workflow_mem_rt_cost(perfOptOfEASW.getMaximal_mem_configuration());
                long EASWExecutionTimeOfPCCO = EASW.EASWSearch(perfConstraint, "PCCO", iter);
                executionTime[0] = EASWExecutionTimeOfBCPO;
                executionTime[1] = EASWExecutionTimeOfPCCO;
            }
        }));

        executorService.submit(new Thread(new Runnable() {
            @Override
            public void run() {
                APPGraph graph = new APPGraph(jsonPath);
                ServerlessAppWorkflow App = new ServerlessAppWorkflow(graph, Optimizer.serverlessWorkflowDelayType, Optimizer.serverlessWorkflowPlatform, -1, -1);
                PerfOpt perfOptOfPRCP = new PerfOpt(App, true, null);

                PRCP prcp = new PRCP(perfOptOfPRCP);
                perfOptOfPRCP.update_App_workflow_mem_rt_cost(perfOptOfPRCP.getMinimal_mem_configuration());
                long PRCPExecutionTimeOfBCPO = prcp.PRCP_OPT(budgetConstraint, "BCPO", Optimizer.BCRthresholdOfPRCP, iter);
                perfOptOfPRCP.update_App_workflow_mem_rt_cost(perfOptOfPRCP.getMaximal_mem_configuration());
                long PRCPExecutionTimeOfPCCO = prcp.PRCP_OPT(perfConstraint, "PCCO", Optimizer.BCRthresholdOfPRCP, iter);
                executionTime[2] = PRCPExecutionTimeOfBCPO;
                executionTime[3] = PRCPExecutionTimeOfPCCO;
            }
        }));

        executorService.submit(new Thread(new Runnable() {
            @Override
            public void run() {
                APPGraph graph = new APPGraph(jsonPath);
                ServerlessAppWorkflow App = new ServerlessAppWorkflow(graph, Optimizer.serverlessWorkflowDelayType, Optimizer.serverlessWorkflowPlatform, -1, -1);
                PerfOpt perfOptOfDFBA = new PerfOpt(App, true, null);

                DFBA dfba = new DFBA(perfOptOfDFBA);
                perfOptOfDFBA.update_App_workflow_mem_rt_cost(perfOptOfDFBA.getMinimal_mem_configuration());
                long DFBAExecutionTimeOfBCPO = dfba.DFBA_OPT(budgetConstraint, "BCPO", iter);
                perfOptOfDFBA.update_App_workflow_mem_rt_cost(perfOptOfDFBA.getMaximal_mem_configuration());
                long DFBAExecutionTimeOfPCCO = dfba.DFBA_OPT(perfConstraint, "PCCO", iter);
                executionTime[6] = DFBAExecutionTimeOfBCPO;
                executionTime[7] = DFBAExecutionTimeOfPCCO;
            }
        }));

        executorService.submit(new Thread(new Runnable() {
            @Override
            public void run() {
                APPGraph graph = new APPGraph(jsonPath);
                ServerlessAppWorkflow App = new ServerlessAppWorkflow(graph, Optimizer.serverlessWorkflowDelayType, Optimizer.serverlessWorkflowPlatform, -1, -1);
                PerfOpt perfOptOfUWC = new PerfOpt(App, true, null);

                UWC uwc = new UWC(perfOptOfUWC);
                perfOptOfUWC.update_App_workflow_mem_rt_cost(perfOptOfUWC.getMinimal_mem_configuration());
                long UWCExecutionTimeOfBCPO = uwc.UWC_OPT(budgetConstraint, "BCPO", iter);
                perfOptOfUWC.update_App_workflow_mem_rt_cost(perfOptOfUWC.getMaximal_mem_configuration());
                long UWCExecutionTimeOfPCCO = uwc.UWC_OPT(perfConstraint, "PCCO", iter);
                executionTime[4] = UWCExecutionTimeOfBCPO;
                executionTime[5] = UWCExecutionTimeOfPCCO;
            }
        }));

        executorService.shutdown();

        while (true) {
            if (executorService.isTerminated()) {
                ResultsVisualization.DrawPictures(perfOpt, iter);
                double[] BCPOPCCOSuccessRate = ResultsVisualization.AlgorithmComparisonDigitization();
                return BCPOPCCOSuccessRate;
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException interruptedException) {
                System.out.println(interruptedException.getMessage());
                System.exit(1);
            }
        }
    }

    public static void OptimizationOfAllServerlessWorkflow() {
        long startTime = System.currentTimeMillis();
        String projectPath = null, jsonDirectoryPath = null, jsonFilePath = null;
        File jsonDirectory = null, jsonFile = null;
        File[] files = null;
        double[] BCPOSuccessRate = null, PCCOSuccessRate = null;
        int repeatedTimes = 10;
        try {
            for (int iter = 1; iter <= repeatedTimes; iter++) {
                projectPath = new File("").getCanonicalPath();
                jsonDirectoryPath = projectPath + "/src/main/resources/serverless_workflow_json_files/";
                jsonDirectory = new File(jsonDirectoryPath);
                if (!jsonDirectory.isDirectory())
                    System.exit(1);
                files = jsonDirectory.listFiles();
                BCPOSuccessRate = new double[files.length];
                PCCOSuccessRate = new double[files.length];
                double[][] executionTime = new double[files.length][8];
                for (int i = 0; i < files.length; i++) {
                    jsonFile = files[i];
                    jsonFilePath = jsonFile.getAbsolutePath();
                    double[] results = Optimizer.OptimizationUnderConstraint(jsonFilePath, executionTime[i], iter);
                    BCPOSuccessRate[i] = results[0];
                    PCCOSuccessRate[i] = results[1];
                    ProgramExecutionTimeComparison.WriteExecutionTimeToFile(jsonFile.getName().replace(".json", ""), executionTime[i], iter);
                }
                DataStoreTools.WriteSuccessRateToFile(files, BCPOSuccessRate, PCCOSuccessRate, iter);
            }
            DataStoreTools.getFinalOptimizationResult();
            DataStoreTools.getFinalExecutionTime();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("The execution time of program：" + new DecimalFormat("0.00").format((double) (endTime - startTime) / 1000 / 3600) + "h");
    }

}
