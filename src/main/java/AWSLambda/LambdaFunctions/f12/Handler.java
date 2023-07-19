package AWSLambda.LambdaFunctions.f12;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.json.JSONObject;

import java.io.*;
import java.sql.Time;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Handler implements RequestHandler<Map<String, String>, Map<String, Map<String, String>>> {

    @Override
    public Map<String, Map<String, String>> handleRequest(Map<String, String> event, Context context) {
        String AWS_ACCESS_KEY = "xxxxxxxxxx";
        String AWS_SECRET_KEY = "xxxxxxxxxx";
        String bucketName = "serverless-network-intensive-source-bucket";
        AWSCredentials credentials = new BasicAWSCredentials(AWS_ACCESS_KEY,AWS_SECRET_KEY);
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).
                withRegion(Regions.US_EAST_1).build();
        // download all objects in S3 bucket
        ListObjectsV2Request v2Request = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result v2Result = s3.listObjectsV2(v2Request);
        try {
            for(S3ObjectSummary objectSummary : v2Result.getObjectSummaries()){
                String key = objectSummary.getKey();
                S3Object S3object = s3.getObject(new GetObjectRequest(bucketName,key));

                InputStream objectDataInputStream = S3object.getObjectContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(objectDataInputStream));
                String line=null;
                int numOfLines = 0;
                while ((line=reader.readLine())!=null){
                    numOfLines ++;
                    StringReader stringReader = new StringReader(line);
                    char[] charArr = new char[line.length()];
                    stringReader.read(charArr);
                }
                System.out.println("The number of lines: " + numOfLines);
                String token = v2Result.getNextContinuationToken();
                v2Request.setContinuationToken(token);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, String> head = new HashMap<>();
        head.put("StatusCode", "200");
        head.put("FunctionName", "f12");
        head.put("Task type", "Network I/O intensive task");
        Map<String, String> body = new HashMap<>();
        for(String key : event.keySet())
            body.put(key,event.get(key));
        Map<String, Map<String, String>> result = new HashMap<>();
        result.put("head", head);
        result.put("body", body);
        return result;
    }
}