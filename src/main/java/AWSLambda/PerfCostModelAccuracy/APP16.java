package AWSLambda.PerfCostModelAccuracy;

import AWSLambda.FunctionsCreator.FunctionUpdator;
import AWSLambda.StateMachineInvoker.Invoker;
import AWSLambda.StateMachineMonitor.Monitor;
import AWSLambda.Util.Tools;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class APP16 {
    private static TreeMap<String,Integer> memConfig = new TreeMap<>();
    static {
        memConfig.put("f1",1088);
        memConfig.put("f2",448);
        memConfig.put("f3",2112);
        memConfig.put("f4",2880);
        memConfig.put("f5",960);
        memConfig.put("f6",1408);
        memConfig.put("f7",768);
        memConfig.put("f8",2816);
        memConfig.put("f9",1280);
        memConfig.put("f10",768);
        memConfig.put("f11",576);
        memConfig.put("f12",1984);
        memConfig.put("f13",1536);
        memConfig.put("f14",960);
        memConfig.put("f15",1152);
        memConfig.put("f16",2944);
    }

    public static void main(String[] args) {
        try {
            FunctionUpdator.updateFunctions(APP16.memConfig);
            System.out.println("The memory sizes of AWS Lambda have been updated. Time : " + new Date());

            Date startDate = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.MINUTE,-2);
            Date startDateInUTC = calendar.getTime();  //start time
            TimeUnit.SECONDS.sleep(5);

            System.out.println("StateMachines start execution! Time : " + new Date());
            String APPName = "APP16";
            int repeatedTimes = 300;
            for(int i=0;i< repeatedTimes + 20;i++)
                Invoker.invokeStateMachine(APPName, i+1);

            calendar.add(Calendar.MINUTE,30);
            Date endDateInUTC = calendar.getTime();  //end time
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println("StateMachines execution is over! Time : " + new Date());

//             wait for statemachine execution and execution logs are stored into CloudWatch
//             the delay of wait can decrease the number of query of CloudWatch
            TimeUnit.MINUTES.sleep(5);

            System.out.println("The query of StateMachines execution log start ! Time : " + new Date());
            String logGroupName = "/aws/vendedlogs/states/SLA-opt-" + APPName + "-StateMachine-Logs";
            int numOfLogs = Monitor.getAmazonCloudWatchLogs(logGroupName, dateFormat.format(startDateInUTC), dateFormat.format(endDateInUTC), repeatedTimes);
//            Monitor.getAmazonCloudWatchLogs(logGroupName, "2022-12-03 21:05:00", "2022-12-03 21:20:00", repeatedTimes);
            System.out.println("The query of StateMachines execution log end ! Time : " + new Date());

            double avgStateMachineDuration = Accuracy.getAvgDurationOfStateMachine(APPName);
            double avgStateMachineCost = Accuracy.getAvgCostOfStateMachine(APPName, dateFormat.format(startDateInUTC), dateFormat.format(endDateInUTC), repeatedTimes);
//            double avgStateMachineCost = Accuracy.getAvgCostOfStateMachine(APPName, "2022-12-03 21:00:00", "2022-12-03 21:30:00", repeatedTimes);
            System.out.println("Average Duration obtained from StateMachine Execution = "+ avgStateMachineDuration +"ms");
            System.out.println("Average Cost obtained from StateMachine Execution = " + avgStateMachineCost + "USD");
            System.out.println("Average duration and cost of StateMachine have been got ! Time : " + new Date());

            double avgPerfCostModelDuration = Accuracy.getAvgDurationOfPerfCostModel(APP16.memConfig, APPName);
            double avgPerfCostModelCost = Accuracy.getAvgCostOfPerfCostModel(APP16.memConfig, APPName);
            System.out.println("Average Duration obtained from PerfCost Model = "+ avgPerfCostModelDuration +"ms");
            System.out.println("Average Cost obtained from PerfCost Model = " + avgPerfCostModelCost + "USD");
            System.out.println("Average duration and cost of PerfCostModel have been got ! Time : " + new Date());

            //计算accuracy放入excel文件
            Tools.generateAccuracy(APPName,avgStateMachineDuration,avgStateMachineCost,avgPerfCostModelDuration,avgPerfCostModelCost);
        }catch (InterruptedException e){
            e.printStackTrace();
            System.exit(1);
        }
    }
}
