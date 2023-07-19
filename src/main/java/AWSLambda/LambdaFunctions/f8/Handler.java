package AWSLambda.LambdaFunctions.f8;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, String>, Map<String, Map<String, String>>> {

    @Override
    public Map<String, Map<String, String>> handleRequest(Map<String, String> event, Context context) {
        int i = 0;
        double finalResult = 0;
        while (i < 210000) {
            finalResult = e_x(10500, 50);
            i++;
        }

        Map<String, String> head = new HashMap<>();
        head.put("StatusCode", "200");
        head.put("FunctionName", "f8");
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

    private double e_x(double x, int n) {
        double result = 0;
        for (int i = 0; i <= n; i++) {
            result += Math.pow(x, n) / Factorial(n);
        }
        return result;
    }

    private double Factorial(int n) {
        if(n==0 || n==1) return 1;
        double result = 1;
        for (int i = 2; i <= n; i++)
            result *= n;
        return result;
    }
}