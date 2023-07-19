package AWSLambda.LambdaFunctions.f2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, String>, Map<String, Map<String, String>>> {

    @Override
    public Map<String, Map<String, String>> handleRequest(Map<String, String> event, Context context) {
        int i=0;
        double pi_n = 0;
        while (i<999){
            pi_n = pi(9000);
            i++;
        }

        Map<String, String> head = new HashMap<>();
        head.put("StatusCode", "200");
        head.put("FunctionName", "f2");
        head.put("Task type", "CPU intensive task");
        head.put("PI", String.valueOf(pi_n));
        Map<String, String> body = new HashMap<>();
        for(String key : event.keySet())
            body.put(key,event.get(key));
        Map<String, Map<String, String>> result = new HashMap<>();
        result.put("head",head);
        result.put("body", body);
        return result;
    }

    private static double pi(int n){
        int numInCircle = 0;
        double x, y;
        double pi;
        for(int i=0;i < n; i++){
            x = Math.random();
            y = Math.random();
            if(x * x + y * y < 1)
                numInCircle++;
        }
        pi = (4.0 * numInCircle) / n;
        return pi;
    }
}