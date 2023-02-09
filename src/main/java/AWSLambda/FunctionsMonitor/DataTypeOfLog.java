package AWSLambda.FunctionsMonitor;

public class DataTypeOfLog {
    private String requestedId;
    private double duration;
    private int billedDuration;
    private int memorySize;
    private int maxMemoryUsed;
    private String functionState;
    private String functionName;

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

    public DataTypeOfLog(String requestedId, double duration, int billedDuration, int memorySize, int maxMemoryUsed, String functionState, String functionName) {
        this.requestedId = requestedId;
        this.duration = duration;
        this.billedDuration = billedDuration;
        this.memorySize = memorySize;
        this.maxMemoryUsed = maxMemoryUsed;
        this.functionState = functionState;
        this.functionName = functionName;
    }

    public DataTypeOfLog() {
        this.requestedId = "null";
        this.duration = 0;
        this.billedDuration = 0;
        this.memorySize = 0;
        this.maxMemoryUsed = 0;
        this.functionState = "null";
        this.functionName = "null";
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
