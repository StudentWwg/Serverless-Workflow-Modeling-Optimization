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
    public static ArrayList<Integer> availableMemory = new ArrayList<>();
    public static ArrayList<Thread> threads = new ArrayList<>();

    static {
        for (int size = 192; size < 1024; size += 64)
            Invoker.availableMemory.add(size);
        for (int size = 1024; size < 2048; size += 128)
            Invoker.availableMemory.add(size);
        for (int size = 2048; size < 4096; size += 256)
            Invoker.availableMemory.add(size);
        for (int size = 4096; size <= 10240; size += 512)
            Invoker.availableMemory.add(size);
//         Invoker.availableMemory.add(192);
    }

    public void invokeFunctions(String[] functionNames) {
        for (int i = 0; i < functionNames.length; i++) {
            String functionName = functionNames[i];
            if(functionName.equals("f1"))
                continue;
            Thread aThread = new Thread(new MyThread(functionName));
            threads.add(aThread);
        }
        for(Thread aThread : threads){
            aThread.start();
        }
    }

    class MyThread implements Runnable {
        String functionName;

        MyThread(String functionName) {
            this.functionName = functionName;
        }

        @Override
        public void run() {
            AWSLambda lambdaClient;
            String filePath = null;
            try {
                filePath = new File("").getCanonicalFile().getPath() + "/src/main/resources/AWSLambda_request_events/testEvent.txt";
            } catch (IOException e) {
                e.printStackTrace();
            }
            String requestEvent = Tools.getFileContent(filePath);

            ArrayList<DataTypeOfLog> logVector = new ArrayList<>();
            Map<Integer, Integer> perfProfile = new TreeMap();

            for (int memorySize : Invoker.availableMemory) {
                lambdaClient = Tools.getAWSLambdaClient();
                int repeatedTimes = 20;  //函数重复执行次数
                int[] billedDurations = new int[repeatedTimes];
                UpdateFunctionConfigurationRequest updateFunctionConfigurationRequest = new UpdateFunctionConfigurationRequest();
                updateFunctionConfigurationRequest.withFunctionName(functionName).withMemorySize(memorySize);
                UpdateFunctionConfigurationResult updateFunctionConfigurationResult = lambdaClient.updateFunctionConfiguration(updateFunctionConfigurationRequest);
                System.out.println("The state of updating memory size of " + functionName + " : " + updateFunctionConfigurationResult.getState());
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (int j = 0; j < repeatedTimes + 10; j++) {
                    InvokeRequest invokeRequest = new InvokeRequest().withFunctionName(functionName).withLogType(LogType.Tail).withPayload(requestEvent);
                    InvokeResult response = lambdaClient.invoke(invokeRequest);
                    DataTypeOfLog dataTypeOfLog = Monitor.logResultParsing(response, functionName);
                    if (j >= 10) {
                        logVector.add(dataTypeOfLog);
                        billedDurations[j - 10] = dataTypeOfLog.getBilledDuration();
                    }
                    System.out.printf("Iteration: %d, %s worked, MemorySize: %d, BilledDuration: %d ms, FunctionState: %s, Time: %s\n", (j + 1), dataTypeOfLog.getFunctionName(),
                            dataTypeOfLog.getMemorySize(), dataTypeOfLog.getBilledDuration(), dataTypeOfLog.getFunctionState(), Instant.now().toString());
//                    if (j % 20 == 0) lambdaClient = Tools.getAWSLambdaClient();
                }
                double count = 0;
                for (int item : billedDurations)
                    count += item;
                int avgbilledDuration = (int) (count / billedDurations.length + 0.5);
                perfProfile.put(memorySize, avgbilledDuration);
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Tools.generateFunctionPerfProfile(perfProfile, functionName);
            Tools.generateFunctionInvokeResult(logVector, "FunctionInvoke", null, 0);
            // lambdaClient.shutdown();
        }
    }
}
