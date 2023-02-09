package AWSLambda.Main;

import AWSLambda.FunctionsCreator.Creator;
import AWSLambda.Util.Tools;


//执行此类需使用Java Runtime ：Java 11
public class AWSLambdaMain {
    public static void main(String[] args) {
//        Creator.createFunctions();
        String[] functionsName = Tools.getFunctionNames();
        AWSLambda.FunctionsInvoker.Invoker.invokeFunctions(functionsName);
    }
}
