package AWSLambda.PerfCostModelAccuracy;

import AWSLambda.FunctionsCreator.FunctionUpdator;
import AWSLambda.StateMachineInvoker.Invoker;
import AWSLambda.StateMachineMonitor.Monitor;
import AWSLambda.Util.Tools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class APP16 {
    private static TreeMap<String,Integer> memConfig = new TreeMap<>();
    private static int[][] memSize = new int[3][16];
    private static String[] taskTypes = {"Disk I/O","CPU","CPU","Network I/O","CPU","CPU","CPU","CPU","CPU","CPU","Disk I/O",
            "Network I/O","CPU","CPU","Disk I/O","Network I/O"};

    static {
        memSize[0] = new int[]{192,192,192,192,192,192,192,192,192,192,192,192,192,192,192,192};
        memSize[1] = new int[]{5120,5120,5120,5120,5120,5120,5120,5120,5120,5120,5120,5120,5120,5120,5120,5120};
        memSize[2] = new int[]{10240,10240,10240,10240,10240,10240,10240,10240,10240,10240,10240,10240,10240,10240,10240,10240};
    }

    public static void getAccuracyOfAPP16() {
        try {
            double[] perfAccuracy = new double[3];
            double[] costAccuracy = new double[3];
            String APPName = "APP16";
            for(int numOfMemSize = 0;numOfMemSize<memSize.length;numOfMemSize++){
                memConfig.clear();
                for(int i=0;i< memSize[numOfMemSize].length;i++)
                    memConfig.put("f"+(i+1),memSize[numOfMemSize][i]);
                System.out.println("The memory configuration of all functions: ");
                for (int i = 1; i <= 16; i++) {
                    System.out.print("f"+i +" : "+memConfig.get("f"+i)+"MB");
                    if(i!=16) System.out.print(",");
                }
                System.out.println();

                FunctionUpdator.updateFunctions(APP16.memConfig);
                System.out.println("The memory sizes of AWS Lambda have been updated. Time : " + new Date());

                Date startDate = null;
                Calendar calendar = Calendar.getInstance();
                Date startDateInUTC = null;  //start time
                TimeUnit.SECONDS.sleep(5);

                System.out.println("StateMachines start execution! Time : " + new Date());
                int repeatedTimes = 300;
                for(int i=1;i<= repeatedTimes + 10;i++){
                    Invoker.invokeStateMachine(APPName, i);
                    if(i%100==0) TimeUnit.MINUTES.sleep(1);
                    if(i==10){
                        TimeUnit.MINUTES.sleep(3);
                        startDate = new Date();
                        calendar.setTime(startDate);
                        calendar.add(Calendar.SECOND, -20);
                        startDateInUTC = calendar.getTime();  //start time
                    }
                }

                calendar.add(Calendar.MINUTE,600);
                Date endDateInUTC = calendar.getTime();  //end time
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                System.out.println("StateMachines execution is over! Time : " + new Date());

                System.out.println("Start time of logs:" + dateFormat.format(startDateInUTC));
                System.out.println("End time of logs:" + dateFormat.format(endDateInUTC));
                String logGroupName = "/aws/vendedlogs/states/SLA-opt-" + APPName + "-StateMachine-Logs";
                TimeUnit.MINUTES.sleep(10);
                System.out.println("The query of StateMachines execution log start ! Time : " + new Date());
                int numOfLogs = Monitor.getAmazonCloudWatchLogs(logGroupName, dateFormat.format(startDateInUTC), dateFormat.format(endDateInUTC), repeatedTimes, numOfMemSize+1);
                System.out.println("There are " + numOfLogs + " StateMachine execution logs queried from Cloud Watch.");
                System.out.println("The query of StateMachines execution log end ! Time : " + new Date());

                double avgStateMachineDuration = Accuracy.getAvgDurationOfStateMachine(APPName,numOfMemSize+1);
                double avgStateMachineCost = Accuracy.getAvgCostOfStateMachine(APPName, dateFormat.format(startDateInUTC), dateFormat.format(endDateInUTC),
                        repeatedTimes,taskTypes, numOfMemSize+1);
                System.out.println("Average Duration obtained from StateMachine Execution = "+
                        new BigDecimal(avgStateMachineDuration).setScale(2, RoundingMode.HALF_UP) +"ms.");
                System.out.println("Average Cost obtained from StateMachine Execution = " +
                        new BigDecimal(avgStateMachineCost).setScale(2, RoundingMode.HALF_UP) + "USD.");
                System.out.println("Average duration and cost of StateMachine have been got ! Time : " + new Date());

                double avgPerfCostModelDuration = Accuracy.getAvgDurationOfPerfCostModel(APP16.memConfig, APPName);
                double avgPerfCostModelCost = Accuracy.getAvgCostOfPerfCostModel(APP16.memConfig, APPName);
                System.out.println("Average Duration obtained from PerfCost Model = "+
                        new BigDecimal(avgPerfCostModelDuration).setScale(2, RoundingMode.HALF_UP) +"ms.");
                System.out.println("Average Cost obtained from PerfCost Model = " +
                        new BigDecimal(avgPerfCostModelCost).setScale(2, RoundingMode.HALF_UP) + "USD.");
                System.out.println("Average duration and cost of PerfCostModel have been got ! Time : " + new Date());

                double[] accuracyResult = Tools.generateAccuracy(APPName,avgStateMachineDuration,avgStateMachineCost,avgPerfCostModelDuration,
                        avgPerfCostModelCost,numOfMemSize+1);
                perfAccuracy[numOfMemSize] = accuracyResult[0];
                costAccuracy[numOfMemSize] = accuracyResult[1];
            }
            double avgPerfAccuracy = Arrays.stream(perfAccuracy).average().getAsDouble();
            double avgCostAccuracy = Arrays.stream(costAccuracy).average().getAsDouble();
            Tools.storeAvgAccuracyOfApp(APPName,avgPerfAccuracy,avgCostAccuracy);
        }catch (InterruptedException e){
            e.printStackTrace();
            System.exit(1);
        }
    }
}
