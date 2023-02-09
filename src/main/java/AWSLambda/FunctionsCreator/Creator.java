package AWSLambda.FunctionsCreator;

import AWSLambda.Util.Tools;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.Runtime;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Creator {

    private static AWSLambda lambdaClient;
    private static String[] functionNames;

    public static void createFunctions() {
        File[] allFunctionsDirectory = getAllFunctionsCode();
        String[] packageNames = new String[allFunctionsDirectory.length];  //package名
        Creator.functionNames = new String[allFunctionsDirectory.length];  //Lambda 函数名 (与package名相同)
        File[] allFunctionsJar = new File[allFunctionsDirectory.length];  //jar包路径
        for (int i = 0; i < allFunctionsDirectory.length; i++) {
            packageNames[i] = allFunctionsDirectory[i].getName();
            Creator.functionNames[i] = allFunctionsDirectory[i].getName();
            //将所有的以jar结尾的部署文件包存储到allFunctionsJar中
            File[] jars = allFunctionsDirectory[i].listFiles((dir, name) -> {
                if (name.endsWith("jar")) return true;
                else return false;
            });
            if (jars.length != 1) {
                System.out.println("File operation error!");
                System.exit(1);
            }
            allFunctionsJar[i] = jars[0];
        }

        lambdaClient = Tools.getAWSLambdaClient();  //获取具有访问凭证的AWSLambda对象

        for (int i = 0; i < Creator.functionNames.length; i++) {
            try {
                CreateFunctionRequest createFunctionRequest = new CreateFunctionRequest().withFunctionName(Creator.functionNames[i]).withRuntime(Runtime.Java11).
                        withRole("arn:aws:iam::104150740987:role/serverless-opt-different-kinds-of-tasks").withMemorySize(128).
                        withHandler(packageNames[i] + "." + "Handler::handleRequest").
                        withTimeout(180).withCode(new FunctionCode().withZipFile(ByteBuffer.wrap(Files.readAllBytes(Path.of(allFunctionsJar[i].getPath()))))).
                        withDescription("Lambda function " + Creator.functionNames[i]);
                CreateFunctionResult createFunctionResult = Creator.lambdaClient.createFunction(createFunctionRequest);
                System.out.println(Creator.functionNames[i] + " has been created.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        lambdaClient.shutdown();
    }

    private static File[] getAllFunctionsCode() {
        File directory = null;
        try {
            directory = new File(new File("").getCanonicalFile().getPath() + "/src/main/java/AWSLambda/LambdaFunctions");
        } catch (IOException e) {
            e.printStackTrace();
        }
        File[] allFunctionsDirectory = directory.listFiles();
        return allFunctionsDirectory;
    }
}
