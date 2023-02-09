package util;

import Main.Optimizer;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class Parameters {
    public static void GetParameters() {
        try {
            String parametersConfigFilePath = new File("").getCanonicalPath() + "/src/main/resources/algorithms_parameter_config/algorithmsParameters.json";
            String jsonContent = FileUtils.readFileToString(new File(parametersConfigFilePath), "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonContent);
            Optimizer.setServerlessWorkflowDelayType(jsonObject.getString("serverlessWorkflowDelayType"));
            Optimizer.setServerlessWorkflowPlatform(jsonObject.getString("serverlessWorkflowPlatform"));
            Optimizer.setNumOfGenesOfCPOGA(jsonObject.getInt("numOfGenesOfGA"));
            Optimizer.setCrossRateOfCPOGA(jsonObject.getDouble("crossRateOfGA"));
            Optimizer.setMutateRateOfCPOGA(jsonObject.getDouble("mutateRateOfGA"));
            Optimizer.setNumOfGenerationsOfCPOGA(jsonObject.getInt("numOfGenerationsOfGA"));
            Optimizer.setETA_M_OfCPOGA(jsonObject.getDouble("ETA_M_Of_GA"));
            Optimizer.setBudgetNumberOfCPOGA(jsonObject.getInt("budgetNumberOfGA"));
            Optimizer.setPerformanceNumberOfCPOGA(jsonObject.getInt("performanceNumberOfGA"));
            Optimizer.setBCRthresholdOfPRCP(jsonObject.getDouble("BCRthresholdOfPRCP"));
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
}
