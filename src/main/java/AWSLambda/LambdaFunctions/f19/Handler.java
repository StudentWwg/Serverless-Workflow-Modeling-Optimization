package AWSLambda.LambdaFunctions.f19;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, String>, Map<String, Map<String, String>>> {

    @Override
    public Map<String, Map<String, String>> handleRequest(Map<String, String> event, Context context) {
        int i = 0;
        double finalResult = 0;
        while (i < 3000) {
            finalResult = sum(200000);
            i++;
        }

        Map<String, String> head = new HashMap<>();
        head.put("StatusCode", "200");
        head.put("FunctionName", "f19");
        head.put("Task type", "CPU intensive task");
        head.put("e^x", String.valueOf(finalResult));
        Map<String, String> body = new HashMap<>();
        for (String key : event.keySet())
            body.put(key, event.get(key));
        Map<String, Map<String, String>> result = new HashMap<>();
        result.put("head", head);
        result.put("body", body);
        return result;
    }

    private double sum(int n) {
        double result = 0;
        for (int i = 0; i <= n; i++) {
            result += i;
        }
        return result;
    }

}
