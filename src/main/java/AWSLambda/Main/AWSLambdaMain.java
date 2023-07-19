package AWSLambda.Main;

import AWSLambda.FunctionsCreator.Creator;
import AWSLambda.FunctionsInvoker.Invoker;
import AWSLambda.PerfCostModelAccuracy.APP10;
import AWSLambda.PerfCostModelAccuracy.APP16;
import AWSLambda.PerfCostModelAccuracy.APP22;
import AWSLambda.PerfCostModelAccuracy.Accuracy;
import AWSLambda.Util.Tools;
import org.json.JSONObject;

import javax.tools.Tool;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


//Java Runtime ：Java 11
public class AWSLambdaMain {
    public static void main(String[] args) throws IOException {
        Creator.createFunctions();
        String[] functionsNames = Tools.getFunctionNames();
        AWSLambda.FunctionsInvoker.Invoker invoker = new Invoker();
        invoker.invokeFunctions(functionsNames);
        APP10.getAccuracyOfAPP10();
        APP16.getAccuracyOfAPP16();
        APP22.getAccuracyOfAPP22();
        Accuracy.getAvgAccuracy();
    }
}
