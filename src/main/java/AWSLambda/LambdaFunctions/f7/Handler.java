package AWSLambda.LambdaFunctions.f7;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, String>, Map<String, Map<String, String>>> {

    @Override
    public Map<String, Map<String, String>> handleRequest(Map<String, String> event, Context context) {
        int i=0;
        long finalResult = 0;
        while(i<120000){
            finalResult = Handler.factorial(2500);  // perhaps the result is 0 because of overflow
            i++;
        }

        Map<String, String> head = new HashMap<>();
        head.put("StatusCode", "200");
        head.put("FunctionName", "f7");
        head.put("Task type", "CPU intensive task");
        head.put("FactorialResult", String.valueOf(finalResult));
        Map<String, String> body = new HashMap<>();
        for(String key : event.keySet())
            body.put(key,event.get(key));
        Map<String, Map<String, String>> result = new HashMap<>();
        result.put("head", head);
        result.put("body", body);
        return result;
    }

    private static long factorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++)
            result = result * i;
        return result;
    }
}