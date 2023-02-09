package AWSLambda.FunctionsCreator;

import AWSLambda.Util.Tools;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class FunctionUpdator {
    private static AWSLambda lambdaClient;
    public static void updateFunctions(TreeMap<String,Integer> memConfig){
        lambdaClient = Tools.getAWSLambdaClient();
        UpdateFunctionConfigurationResult[] results = new UpdateFunctionConfigurationResult[memConfig.size()];
        int i=0;
        for(String functionName : memConfig.keySet()){
            int memSize = memConfig.get(functionName);
            UpdateFunctionConfigurationRequest request = new UpdateFunctionConfigurationRequest().withFunctionName(functionName);
            request.setMemorySize(memSize);
            results[i++] = lambdaClient.updateFunctionConfiguration(request);
        }
        try{
            TimeUnit.SECONDS.sleep(10);
        }catch (InterruptedException e){
            e.printStackTrace();
            System.exit(1);
        }
    }
}
