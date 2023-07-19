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
            Optimizer.setNumOfGenesOfEASW(jsonObject.getInt("numOfGenesOfEASW"));
            Optimizer.setCrossRateOfEASW(jsonObject.getDouble("crossRateOfEASW"));
            Optimizer.setMutationRateOfEASW(jsonObject.getDouble("mutationRateOfEASW"));
            Optimizer.setNumOfGenerationsOfEASW(jsonObject.getInt("numOfGenerationsOfEASW"));
            Optimizer.setETA_M_OfEASW(jsonObject.getDouble("ETA_M_Of_EASW"));
            Optimizer.setBudgetNumberOfEASW(jsonObject.getInt("budgetNumberOfEASW"));
            Optimizer.setPerformanceNumberOfEASW(jsonObject.getInt("performanceNumberOfEASW"));
            Optimizer.setBCRthresholdOfPRCP(jsonObject.getDouble("BCRthresholdOfPRCP"));
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
}
