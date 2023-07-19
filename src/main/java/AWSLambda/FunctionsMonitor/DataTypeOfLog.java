package AWSLambda.FunctionsMonitor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataTypeOfLog {
    private String requestedId;
    private double duration;
    private int billedDuration;
    private int memorySize;
    private int maxMemoryUsed;
    private String functionState;
    private String functionName;
    private long UTCTimeStamp;

    public long getUTCTimeStamp() {return UTCTimeStamp;}

    public void setUTCTimeStamp(String UTCTimeStamp) {
        try{
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = dateFormat.parse(UTCTimeStamp);
            this.UTCTimeStamp = date.getTime();
        }catch (ParseException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String getRequestedId() {
        return requestedId;
    }

    public void setRequestedId(String requestedId) {
        this.requestedId = requestedId;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getBilledDuration() {
        return billedDuration;
    }

    public void setBilledDuration(int billedDuration) {
        this.billedDuration = billedDuration;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    public int getMaxMemoryUsed() {
        return maxMemoryUsed;
    }

    public void setMaxMemoryUsed(int maxMemoryUsed) {
        this.maxMemoryUsed = maxMemoryUsed;
    }

    public String getFunctionState() {
        return functionState;
    }

    public void setFunctionState(String functionState) {
        this.functionState = functionState;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public DataTypeOfLog(String requestedId, double duration, int billedDuration, int memorySize, int maxMemoryUsed, String functionState, String functionName, String simpleFormatTime) {
        this.requestedId = requestedId;
        this.duration = duration;
        this.billedDuration = billedDuration;
        this.memorySize = memorySize;
        this.maxMemoryUsed = maxMemoryUsed;
        this.functionState = functionState;
        this.functionName = functionName;
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = dateFormat.parse(simpleFormatTime);
            this.UTCTimeStamp = date.getTime();
        }catch (ParseException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public DataTypeOfLog() {
        this.requestedId = "null";
        this.duration = 0;
        this.billedDuration = 0;
        this.memorySize = 0;
        this.maxMemoryUsed = 0;
        this.functionState = "null";
        this.functionName = "null";
        this.UTCTimeStamp=0;
    }

    @Override
    public String toString() {
        return "functionName='" + functionName + '\'' +
                ", requestedId='" + requestedId + '\'' +
                ", duration=" + duration +
                " ms, billedDuration=" + billedDuration +
                " ms, memorySize=" + memorySize +
                " MB, maxMemoryUsed=" + maxMemoryUsed +
                " MB, functionState='" + functionState + '\'';
    }
}
