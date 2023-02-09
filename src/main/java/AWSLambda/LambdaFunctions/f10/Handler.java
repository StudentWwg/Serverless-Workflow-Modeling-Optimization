package AWSLambda.LambdaFunctions.f10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.Map;
//处理程序: f10.Handler::handleRequest
public class Handler implements RequestHandler<Map<String, String>, Map<String, Map<String, String>>> {

    @Override
    public Map<String, Map<String, String>> handleRequest(Map<String, String> event, Context context) {
        int i=0;
        int finalResult = 0;
        while(i<80){
            finalResult = fibonacci(28);
            i++;
        }

        Map<String, String> head = new HashMap<>();
        head.put("StatusCode", "200");
        head.put("FunctionName", "f10");
        head.put("Task type", "CPU intensive task");
        head.put("FibonacciResult", String.valueOf(finalResult));
        Map<String, String> body = new HashMap<>();
        for(String key : event.keySet())
            body.put(key,event.get(key));
        Map<String, Map<String, String>> result = new HashMap<>();
        result.put("head", head);
        result.put("body", body);
        return result;
    }

    private int fibonacci(int n) {
        if(n<=1) return n;
        else
            return fibonacci(n-1) + fibonacci(n-2);
    }
}