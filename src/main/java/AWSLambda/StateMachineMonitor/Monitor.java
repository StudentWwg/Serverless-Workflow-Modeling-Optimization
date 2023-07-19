package AWSLambda.StateMachineMonitor;


import AWSLambda.Util.Tools;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Monitor {
    private static AWSLogs logsClient;

    public static int getAmazonCloudWatchLogs(String logGroupName, String startTimeInString, String endTimeInString, int repeatedTimes, int iter) {
        Monitor.logsClient = Tools.getAWSLogsClient();
        Long logStartTime = 0L;
        Long logEndTime = 0L;
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date startDate = dateFormat.parse(startTimeInString), endDate = dateFormat.parse(endTimeInString);
            logStartTime = startDate.getTime();
            logEndTime = endDate.getTime();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        List<List<ResultField>> results = null;
        do {
            String queryString = "fields type, event_timestamp, @timestamp, execution_arn | filter type = \"ExecutionStarted\" or type = \"ExecutionSucceeded\" or type = \"ExecutionFailed\" | sort id desc";
            StartQueryRequest startQueryRequest = new StartQueryRequest().withLogGroupName(logGroupName).
                    withStartTime(logStartTime).withEndTime(logEndTime).withQueryString(queryString).withLimit(10000);
            StartQueryResult startQueryResult = logsClient.startQuery(startQueryRequest);

            GetQueryResultsRequest getQueryResultsRequest = null;
            GetQueryResultsResult getQueryResultsResult = null;
            do {
                getQueryResultsRequest = new GetQueryResultsRequest().withQueryId(startQueryResult.getQueryId());
                getQueryResultsResult = logsClient.getQueryResults(getQueryResultsRequest);
                try {
                    TimeUnit.MINUTES.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                System.out.println("The state of query : " + getQueryResultsResult.getStatus().toString());
            } while ("Running".equals(getQueryResultsResult.getStatus().toString()));

            results = getQueryResultsResult.getResults();
            System.out.println("The size of results is " + results.size());
        } while (results.size() / 2 / repeatedTimes  != 1);

        List<List<ResultField>> listOfExecutionStarted = new ArrayList<>();
        List<List<ResultField>> listOfExecutionSucceeded = new ArrayList<>();
        for (List<ResultField> resultFields : results) {
            if (resultFields.get(0).getValue().equals("ExecutionSucceeded") || resultFields.get(0).getValue().equals("ExecutionFailed")) {
                listOfExecutionSucceeded.add(resultFields);
            } else if (resultFields.get(0).getValue().equals("ExecutionStarted")) {
                listOfExecutionStarted.add(resultFields);
            }
        }

        ArrayList<Long> timeStampOfExecutionSucceeded = new ArrayList<Long>();
        ArrayList<Long> timeStampOfExecutionStarted = new ArrayList<Long>();
        for (List<ResultField> resultFieldsOfExecutionStarted : listOfExecutionStarted) {
            String arn = resultFieldsOfExecutionStarted.get(3).getValue();
            for(List<ResultField> resultFieldsOfExecutionSucceeded : listOfExecutionSucceeded){
                if(resultFieldsOfExecutionSucceeded.get(3).getValue().equals(arn)){
                    timeStampOfExecutionStarted.add(Long.valueOf(resultFieldsOfExecutionStarted.get(1).getValue()));
                    timeStampOfExecutionSucceeded.add(Long.valueOf(resultFieldsOfExecutionSucceeded.get(1).getValue()));
                    break;
                }
            }
        }

        Tools.generateTimeStampOfStateMachineInvokation(timeStampOfExecutionStarted, timeStampOfExecutionSucceeded, logGroupName, iter);
        return results.size() / 2;
    }
}
