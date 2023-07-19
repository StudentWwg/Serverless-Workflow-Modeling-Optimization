package AWSLambda.LambdaFunctions.f17;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, String>, Map<String, Map<String, String>>> {
    @Override
    public Map<String, Map<String, String>> handleRequest(Map<String, String> event, Context context) {
        int i=0;
        double x = 987654321;
        double sqrtOfX = 0;
        while (i<70000){
            sqrtOfX = sqrtX(x);
            i++;
        }

        Map<String, String> head = new HashMap<>();
        head.put("StatusCode", "200");
        head.put("FunctionName", "f17");
        head.put("Task type", "CPU intensive task");
        head.put("sqrtOfX", String.valueOf(sqrtOfX));
        Map<String, String> body = new HashMap<>();
        for(String key : event.keySet())
            body.put(key,event.get(key));
        Map<String, Map<String, String>> result = new HashMap<>();
        result.put("head", head);
        result.put("body", body);
        return result;
    }

    private static double sqrtX(double x) {
        double x0 = x / 2;
        for (int i = 0; i < 500; i++) {
            double x1 = (x0 + x / x0) / 2;
            x0 = x1;
        }
        return x0;
    }
}
