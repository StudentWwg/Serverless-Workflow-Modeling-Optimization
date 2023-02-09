package AWSLambda.FunctionsMonitor;

import AWSLambda.Util.Tools;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.*;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Monitor {
    private static AWSLogs logsClient;

    public static DataTypeOfLog logResultParsing(InvokeResult response, String functionName) {
        Map<String, String> m = new HashMap<>();
        String RequestId = response.getSdkResponseMetadata().getRequestId();
        String logString = null;
        try {
            logString = new String(Base64.getDecoder().decode(response.getLogResult()), "UTF-8");  //获得的Logresult需要解码
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String[] tempResult = logString.split("\n");
        String logMetrics = null;
        for (int i = 0; i < tempResult.length; i++) {
            if (tempResult[i].startsWith("REPORT RequestId:")) {
                logMetrics = tempResult[i];
                break;
            }
        }

        String[] allMetrics = logMetrics.split("\t");
        double duration = Double.valueOf(allMetrics[1].split(":")[1].trim().replace(" ms", ""));
        int billedDuration = Integer.valueOf(allMetrics[2].split(":")[1].trim().replace(" ms", ""));
        int memorySize = Integer.valueOf(allMetrics[3].split(":")[1].trim().replace(" MB", ""));
        int maxMemoryUsed = Integer.valueOf(allMetrics[4].split(":")[1].trim().replace(" MB", ""));
        String functionError = response.getFunctionError();
        String functionState = null;
        if (functionError == null)
            functionState = "Success";
        else
            functionState = "Error";
        DataTypeOfLog dataTypeOfLog = new DataTypeOfLog(RequestId, duration, billedDuration, memorySize, maxMemoryUsed, functionState, functionName);
        return dataTypeOfLog;
    }

    public static void getAmazonCloudWatchLogs(String logGroupName, String startTimeInString, String endTimeInString, String APPName){
        Monitor.logsClient = Tools.getAWSLogsClient();
        Long logStartTime= 0L;
        Long logEndTime = 0L;
        try{
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date startDate = dateFormat.parse(startTimeInString), endDate = dateFormat.parse(endTimeInString);
            logStartTime = startDate.getTime();
            logEndTime = endDate.getTime();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

        String queryString = "fields @timestamp, @message | filter @message like 'REPORT' | sort id asc";
        StartQueryRequest startQueryRequest = new StartQueryRequest().withLogGroupName(logGroupName).
                withStartTime(logStartTime).withEndTime(logEndTime).withQueryString(queryString).withLimit(10000);
        StartQueryResult startQueryResult = logsClient.startQuery(startQueryRequest);

        GetQueryResultsRequest getQueryResultsRequest = null;
        GetQueryResultsResult getQueryResultsResult = null;
        do{
            getQueryResultsRequest = new GetQueryResultsRequest().withQueryId(startQueryResult.getQueryId());
            getQueryResultsResult = logsClient.getQueryResults(getQueryResultsRequest);
            try{
                TimeUnit.SECONDS.sleep(5);
            }catch (InterruptedException e){
                e.printStackTrace();
                System.exit(1);
            }
        }while ("Running".equals(getQueryResultsResult.getStatus().toString()));

        List<List<ResultField>> results = getQueryResultsResult.getResults();
        try{
            TimeUnit.SECONDS.sleep(10);   //add a time delay to get results from AWS CloudWatch
        }catch (InterruptedException e){
            e.printStackTrace();
            System.exit(1);
        }
        ArrayList<DataTypeOfLog> invokeResults = new ArrayList<>();

        for(List<ResultField> resultFields : results){
                String message = resultFields.get(1).getValue();
                String[] infoOfLogs = message.split("\t");
                DataTypeOfLog dataOfLog = new DataTypeOfLog();
                for(int i=0;i< infoOfLogs.length;i++){
                    String[] keyValuePair = infoOfLogs[i].split(": ");
                    if(i==0) dataOfLog.setRequestedId(keyValuePair[1]);
                    else if(i==1) dataOfLog.setDuration(Double.valueOf(keyValuePair[1].split(" ms")[0]));
                    else if(i==2) dataOfLog.setBilledDuration(Integer.valueOf(keyValuePair[1].split(" ms")[0]));
                    else if(i==3) dataOfLog.setMemorySize(Integer.valueOf(keyValuePair[1].split(" MB")[0]));
                    else if(i==4) dataOfLog.setMaxMemoryUsed(Integer.valueOf(keyValuePair[1].split(" MB")[0]));
                }
                dataOfLog.setFunctionName(logGroupName.replace("/aws/lambda/", ""));
                dataOfLog.setFunctionState("Success");
                invokeResults.add(dataOfLog);
        }
        Tools.generateFunctionInvokeResult(invokeResults, "CloudWatchLog", APPName);
    }
}
