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

    public static int getAmazonCloudWatchLogs(String logGroupName, String startTimeInString, String endTimeInString, int repeatedTimes) {
        Monitor.logsClient = Tools.getAWSLogsClient();
        Long logStartTime = 0L;
        Long logEndTime = 0L;
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date startDate = dateFormat.parse(startTimeInString), endDate = dateFormat.parse(endTimeInString);
            logStartTime = startDate.getTime();
            logEndTime = endDate.getTime();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        //查询结果可能与执行次数不一致，重新查询
        //执行日志的写入需要一定时延
        List<List<ResultField>> results = null;
        do {
            String queryString = "fields type, event_timestamp, @timestamp | filter type = \"ExecutionStarted\" or type = \"ExecutionSucceeded\" | sort id desc";
            StartQueryRequest startQueryRequest = new StartQueryRequest().withLogGroupName(logGroupName).
                    withStartTime(logStartTime).withEndTime(logEndTime).withQueryString(queryString).withLimit(10000);
            StartQueryResult startQueryResult = logsClient.startQuery(startQueryRequest);

            GetQueryResultsRequest getQueryResultsRequest = null;
            GetQueryResultsResult getQueryResultsResult = null;
            do {
                getQueryResultsRequest = new GetQueryResultsRequest().withQueryId(startQueryResult.getQueryId());
                getQueryResultsResult = logsClient.getQueryResults(getQueryResultsRequest);
                try {
                    TimeUnit.SECONDS.sleep(15);  //等待查询结果
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                System.out.println("The state of query : " + getQueryResultsResult.getStatus().toString());
            } while ("Running".equals(getQueryResultsResult.getStatus().toString()));

            results = getQueryResultsResult.getResults();
            System.out.println("The size of results is " + results.size());
        } while (results.size() / 2 / repeatedTimes  != 1);

        ArrayList<Long> timeStampOfExecutionSucceed = new ArrayList<Long>();
        ArrayList<Long> timeStampOfExecutionStarted = new ArrayList<Long>();
        for (List<ResultField> resultFields : results) {
            if (resultFields.get(0).getValue().equals("ExecutionSucceeded")) {
                timeStampOfExecutionSucceed.add(Long.valueOf(resultFields.get(1).getValue()));
            } else if (resultFields.get(0).getValue().equals("ExecutionStarted")) {
                timeStampOfExecutionStarted.add(Long.valueOf(resultFields.get(1).getValue()));
            }
        }
        Collections.sort(timeStampOfExecutionStarted);
        Collections.sort(timeStampOfExecutionSucceed);
        for(int i=0;i<20;i++){
            timeStampOfExecutionStarted.remove(0);
            timeStampOfExecutionSucceed.remove(0);
        }

        Tools.generateTimeStampOfStateMachineInvokation(timeStampOfExecutionStarted, timeStampOfExecutionSucceed, logGroupName);
        return results.size() / 2;
    }
}
