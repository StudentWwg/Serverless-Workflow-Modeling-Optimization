package AWSLambda.FunctionsInvoker;

import AWSLambda.FunctionsMonitor.DataTypeOfLog;
import AWSLambda.FunctionsMonitor.Monitor;
import AWSLambda.Util.Tools;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.*;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Invoker {
    private static AWSLambda lambdaClient;

    public static void invokeFunctions(String[] functionName) {
        lambdaClient = Tools.getAWSLambdaClient();
        String filePath = null;
        try {
            filePath = new File("").getCanonicalFile().getPath() + "/src/main/resources/AWSLambda_request_events/testEvent.txt";
        } catch (IOException e) {
            e.printStackTrace();
        }
        String requestEvent = Tools.getFileContent(filePath);
        for (int i = 0; i < functionName.length; i++) {
            if(!(functionName[i].equals("f2")))
                continue;
            ArrayList<DataTypeOfLog> logVector = new ArrayList<>();
            Map<Integer, Integer> perfProfile = new TreeMap();  //记录各函数的存储容量和花费时间的映射关系
            for (int memorySize = 192; memorySize <= 3008; memorySize += 64) {
                lambdaClient = Tools.getAWSLambdaClient();   //防止访问用户过期
                int repeatedTimes = 100;  //函数重复执行次数
                int[] billedDurations = new int[repeatedTimes];
                UpdateFunctionConfigurationRequest updateFunctionConfigurationRequest = new UpdateFunctionConfigurationRequest();
                updateFunctionConfigurationRequest.withFunctionName(functionName[i]).withMemorySize(memorySize);  //更新函数内存大小
                UpdateFunctionConfigurationResult updateFunctionConfigurationResult = lambdaClient.updateFunctionConfiguration(updateFunctionConfigurationRequest);
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (int j = 0; j < repeatedTimes + 10; j++) {  //每个函数执行 repeatedTimes + 10 次，前10次不计入统计结果
                    System.out.println("The state of updating memory size of " + functionName[i] + " : " + updateFunctionConfigurationResult.getState());
                    InvokeRequest invokeRequest = new InvokeRequest().withFunctionName(functionName[i]).withLogType(LogType.Tail).withPayload(requestEvent);
                    InvokeResult response = lambdaClient.invoke(invokeRequest);
                    DataTypeOfLog dataTypeOfLog = Monitor.logResultParsing(response, functionName[i]);
                    logVector.add(dataTypeOfLog);
                    if(j>=10)
                        billedDurations[j-10] = dataTypeOfLog.getBilledDuration();
                    System.out.printf("%s worked.  MemorySize: %d, BilledDuration: %d ms, FunctionState: %s, Time: %s\n", dataTypeOfLog.getFunctionName(),
                            dataTypeOfLog.getMemorySize(), dataTypeOfLog.getBilledDuration(), dataTypeOfLog.getFunctionState(), Instant.now().toString());
                    if(j%50==0) lambdaClient = Tools.getAWSLambdaClient();
                }
                double count = 0;
                for (int item : billedDurations)
                    count += item;
                int avgbilledDuration = (int) (count / billedDurations.length + 0.5);  //计算平均值
                perfProfile.put(memorySize, avgbilledDuration);
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Tools.generateFunctionPerfProfile(perfProfile, functionName[i]);
            Tools.generateFunctionInvokeResult(logVector,"FunctionInvoke", null);
        }
        lambdaClient.shutdown();
    }
}
