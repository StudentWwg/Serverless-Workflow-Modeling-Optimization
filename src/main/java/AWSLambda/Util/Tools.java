package AWSLambda.Util;

import AWSLambda.FunctionsMonitor.DataTypeOfLog;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.ListStateMachinesRequest;
import com.amazonaws.services.stepfunctions.model.ListStateMachinesResult;
import com.amazonaws.services.stepfunctions.model.StateMachineListItem;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;

public class Tools {
    private static String AWS_ACCESS_KEY;
    private static String AWS_SECRET_KEY;
    private static String regionName;  //香港地区
    private static AWSCredentials credentials;
    private static AWSLambda lambdaClient;
    private static AWSStepFunctions stepFunctionsClient;
    private static AWSLogs logsClient;

    public static AWSLambda getAWSLambdaClient() {
        getCredentials();
        AWSLambdaClientBuilder lambdaBuilder = AWSLambdaClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(Tools.credentials));
        lambdaBuilder.setRegion(Tools.regionName); //设置服务器的区域
        Tools.lambdaClient = lambdaBuilder.build();
        return Tools.lambdaClient;
    }

    public static AWSStepFunctions getAWSStepFunctionsClient() {
        getCredentials();
        AWSStepFunctionsClientBuilder awsStepFunctionsClientBuilder = AWSStepFunctionsClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(Tools.credentials));
        awsStepFunctionsClientBuilder.setRegion(Tools.regionName);
        Tools.stepFunctionsClient = awsStepFunctionsClientBuilder.build();
        return Tools.stepFunctionsClient;
    }

    public static AWSLogs getAWSLogsClient() {
        getCredentials();
        AWSLogsClientBuilder awsLogsClientBuilder = AWSLogsClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(Tools.credentials));
        awsLogsClientBuilder.setRegion(Tools.regionName);
        Tools.logsClient = awsLogsClientBuilder.build();
        return Tools.logsClient;
    }

    private static void getAWSLambdaAccessConfig() {
        try {
            String parametersConfigFilePath = new File("").getCanonicalPath() + "/src/main/resources/AWSLambda_config/AwsLambdaParameters.json";
            String jsonContent = FileUtils.readFileToString(new File(parametersConfigFilePath), "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonContent);
            Tools.AWS_ACCESS_KEY = jsonObject.getString("AWS_ACCESS_KEY");
            Tools.AWS_SECRET_KEY = jsonObject.getString("AWS_SECRET_KEY");
            Tools.regionName = jsonObject.getString("REGION_NAME");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] getFunctionNames() {
        lambdaClient = getAWSLambdaClient();
        ListFunctionsResult functionsResult = lambdaClient.listFunctions();
        List<FunctionConfiguration> functions = functionsResult.getFunctions();
        String[] functionNames = new String[functions.size()];
        for (int i = 0; i < functions.size(); i++) {
            functionNames[i] = functions.get(i).getFunctionName();
        }
        lambdaClient.shutdown();
        return functionNames;
    }

    public static List<StateMachineListItem> getStateMachineList() {
        stepFunctionsClient = getAWSStepFunctionsClient();
        ListStateMachinesResult listStateMachinesResult = stepFunctionsClient.listStateMachines(new ListStateMachinesRequest());
        List<StateMachineListItem> stateMachines = listStateMachinesResult.getStateMachines();
        return stateMachines;
    }

    public static AWSCredentials getCredentials() {
        getAWSLambdaAccessConfig();
        Tools.credentials = new BasicAWSCredentials(Tools.AWS_ACCESS_KEY, Tools.AWS_SECRET_KEY);
        return Tools.credentials;
    }

    public static String getFileContent(String path) {   //get event in the file
        StringBuffer stringBuffer = new StringBuffer();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
            String line = "";
            while (true) {
                line = bufferedReader.readLine();
                if (line == null)
                    break;
                stringBuffer.append(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuffer.toString();
    }

    public static void generateFunctionPerfProfile(Map<Integer, Integer> perfProfile, String functionName) {
        String filePrefix = null;
        try {
            filePrefix = new File("").getCanonicalFile() + "/src/main/resources/AWSLambda_functions_perf_profile/";
            File file = new File(filePrefix + functionName + "_perf_profile.json");
            JSONObject jsonObject = new JSONObject();
            for (Integer item : perfProfile.keySet())
                jsonObject.put(String.valueOf(item), perfProfile.get(item));
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            String s = jsonObject.toString();
            fileOutputStream.write(s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void generateFunctionInvokeResult(ArrayList<DataTypeOfLog> logVector, String type, String APPName) {
        String filePath = null;
        try {
            if(type.equals("FunctionInvoke")) {
                filePath = new File("").getCanonicalFile() + "/src/main/resources/AWSLambda_functions_invoke_results_got_by_function/AWSLambda_" +
                        logVector.get(0).getFunctionName() + "_Logs.xls";
            }
            else if(type.equals("CloudWatchLog")){
                filePath = new File("").getCanonicalFile() + "/src/main/resources/AWSLambda_functions_invoke_results_got_by_cloudwatchlog/"+
                        APPName+"/AWSLambda_" + logVector.get(0).getFunctionName() + "_Logs.xls";
            }

            File file = new File(filePath);
            FileOutputStream outputStream = new FileOutputStream(file);

            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet(logVector.get(logVector.size() - 1).getFunctionName() + "_logs");
            HSSFRow row0 = sheet.createRow(0); //表头行
            row0.createCell(0).setCellValue("FunctionName");
            row0.createCell(1).setCellValue("FunctionState");
            row0.createCell(2).setCellValue("MemorySize");
            row0.createCell(3).setCellValue("MaxMemoryUsed");
            row0.createCell(4).setCellValue("Duration");
            row0.createCell(5).setCellValue("BilledDuration");
            row0.createCell(6).setCellValue("RequestedId");
            for (int i = 0; i < logVector.size(); i++) {
                HSSFRow row = sheet.createRow(i + 1);
                DataTypeOfLog log = logVector.get(i);
                row.createCell(0).setCellValue(log.getFunctionName());
                row.createCell(1).setCellValue(log.getFunctionState());
                row.createCell(2).setCellValue(log.getMemorySize());
                row.createCell(3).setCellValue(log.getMaxMemoryUsed());
                row.createCell(4).setCellValue(log.getDuration());
                row.createCell(5).setCellValue(log.getBilledDuration());
                row.createCell(6).setCellValue(log.getRequestedId());
            }
            workbook.write(outputStream);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void generateTimeStampOfStateMachineInvokation(ArrayList<Long> timeStampOfExecutionStarted, ArrayList<Long> timeStampOfExecutionSucceed, String logGroupName) {
        String filePath = null;
        String APPName = logGroupName.replace("/aws/vendedlogs/states/SLA-opt-", "").replace("-StateMachine-Logs", "");
        try {
            filePath = new File("").getCanonicalPath() + "/src/main/resources/AWSLambda_StateMachine_invoke_results/AWSLambda_StateMachine_" +
                    APPName + "_Logs.xls";
            File file = new File(filePath);
            FileOutputStream outputStream = new FileOutputStream(file);

            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("StateMachine_" + APPName + "_logs");
            HSSFRow row0 = sheet.createRow(0); //表头行
            row0.createCell(0).setCellValue("Start");
            row0.createCell(1).setCellValue("End");
            row0.createCell(2).setCellValue("Duration");

            int size = Math.min(timeStampOfExecutionStarted.size(),timeStampOfExecutionSucceed.size());
            for (int i = 0; i < size; i++) {
                HSSFRow aRow = sheet.createRow(i + 1);
                long startTime = timeStampOfExecutionStarted.get(i);
                long endTime = timeStampOfExecutionSucceed.get(i);
                aRow.createCell(0).setCellValue(startTime);
                aRow.createCell(1).setCellValue(endTime);
                aRow.createCell(2).setCellValue(endTime - startTime);
            }
            workbook.write(outputStream);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static String getStateMachineExecutionEvent(String APPName) {
        JSONObject event = new JSONObject();

        int randInt = new Random().nextInt(100);
        if (randInt < 60) event.put("para1", "f4");
        else event.put("para1", "f5");

        randInt = new Random().nextInt(100);
        if (randInt < 80) event.put("para2", "f9");
        else event.put("para2", "f10");

        randInt = new Random().nextInt(100);
        if (randInt < 70) event.put("para3", "f13");
        else event.put("para3", "f14");

        randInt = new Random().nextInt(100);
        if (randInt < 95) event.put("para4", "f16");
        else event.put("para4", "f15");

        if (APPName.equals("APP8")) {
            event.remove("para2");
            event.remove("para3");
            event.remove("para4");
        } else if (APPName.equals("APP10") || APPName.equals("APP12")) {
            event.remove("para3");
            event.remove("para4");
        } else if (APPName.equals("APP14")) {
            event.remove("para4");
        }
        return event.toString();
    }

    public static double[] generateAvgDurationAndNEOfFunction(String functionName){
        double[] durationAndNE = new double[2];
        try{
            String filePath = new File("").getCanonicalPath() + "/src/main/resources/AWSLambda_functions_invoke_results_got_by_cloudwatchlog/AWSLambda_" +
                    functionName + "_Logs.xls";
            FileInputStream inputStream = new FileInputStream(filePath);
            HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
            HSSFSheet sheet = workbook.getSheet(functionName+"_logs");
            int rowNum = sheet.getLastRowNum();
            double[] billedDuration = new double[rowNum];
            for(int i=1;i<=rowNum;i++){
                HSSFRow aRow = sheet.getRow(i);
                billedDuration[i-1] = aRow.getCell(5).getNumericCellValue();  //获取billed Duration 字段
            }
            double avgBilledDuration = Arrays.stream(billedDuration).average().getAsDouble();
            double NE = billedDuration.length;
            durationAndNE[0] = avgBilledDuration;
            durationAndNE[1] = NE;
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
        return durationAndNE;
    }

    public static void generateAccuracy(String APPName, double StateMachineDuration,
                                        double StateMachineCost, double PerfCostModelDuration, double PerfCostModelCost){
        try{
            String filePath = new File("").getCanonicalPath() + "/src/main/resources/accuracy/"+APPName+"_Accuracy.xls";
            File accuracyFile = new File(filePath);
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("Accuracy");

            HSSFRow head = sheet.createRow(0);
            head.createCell(0).setCellValue("APPName");
            head.createCell(1).setCellValue("StateMachineDuration");
            head.createCell(2).setCellValue("StateMachineCost");
            head.createCell(3).setCellValue("PerfCostModelDuration");
            head.createCell(4).setCellValue("PerfCostModelCost");
            head.createCell(5).setCellValue("PerfAccuracy");
            head.createCell(6).setCellValue("CostAccuracy");

            HSSFRow aRow = sheet.createRow(1);
            aRow.createCell(0).setCellValue(APPName);
            aRow.createCell(1).setCellValue(StateMachineDuration);
            aRow.createCell(2).setCellValue(StateMachineCost);
            aRow.createCell(3).setCellValue(PerfCostModelDuration);
            aRow.createCell(4).setCellValue(PerfCostModelCost);

            DecimalFormat decimalFormat = new DecimalFormat("0.00");
            decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
            Double PerfAccuracy = 100- Math.abs(PerfCostModelDuration-StateMachineDuration)/PerfCostModelDuration * 100;
            aRow.createCell(5).setCellValue(decimalFormat.format(PerfAccuracy) + "%");
            double CostAccuracy = 100 - Math.abs(PerfCostModelCost-StateMachineCost) / PerfCostModelCost * 100;
            aRow.createCell(6).setCellValue(decimalFormat.format(CostAccuracy) +"%");

            FileOutputStream outputStream  = new FileOutputStream(accuracyFile);
            workbook.write(outputStream);
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }
}
