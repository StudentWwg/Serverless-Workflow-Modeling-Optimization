package AWSLambda.LambdaFunctions.f5;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, String>, Map<String, Map<String, String>>> {
    @Override
    public Map<String, Map<String, String>> handleRequest(Map<String, String> event, Context context) {
        byte[] finalResult = null;
        for(int i=0;i<35000;i++){
            try {
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA512");
                SecretKey secretKey = generator.generateKey();
                Mac mac = Mac.getInstance("HmacMD5");
                mac.init(secretKey);
                String data = "Lambda function f5";
                finalResult = mac.doFinal(data.getBytes());  //generate summary
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                e.printStackTrace();
            }
        }

        Map<String, String> head = new HashMap<>();
        head.put("StatusCode", "200");
        head.put("FunctionName", "f5");
        head.put("Task type", "CPU intensive task");
        head.put("Digest", finalResult.toString());
        Map<String, String> body = new HashMap<>();
        for(String key : event.keySet())
            body.put(key,event.get(key));
        Map<String, Map<String, String>> result = new HashMap<>();
        result.put("head", head);
        result.put("body", body);
        return result;
    }
}
