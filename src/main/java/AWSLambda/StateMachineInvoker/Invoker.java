package AWSLambda.StateMachineInvoker;

import AWSLambda.Util.Tools;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.amazonaws.services.stepfunctions.model.StateMachineListItem;

import java.util.Date;
import java.util.List;

public class Invoker {
    private static AWSStepFunctions stepFunctionsClient;

    public static void invokeStateMachine(String APPName, int repeatedTimes) {
        stepFunctionsClient = Tools.getAWSStepFunctionsClient();
        List<StateMachineListItem> stateMachines = Tools.getStateMachineList();
        StateMachineListItem stateMachine = null;
        for(StateMachineListItem aStateMachine : stateMachines){
            if(aStateMachine.getName().indexOf(APPName)!=-1)
                stateMachine = aStateMachine;
        }

        if(stateMachine!=null){
            String stateMachineArn = stateMachine.getStateMachineArn();
            String inputEvent = Tools.getStateMachineExecutionEvent(APPName);
            StartExecutionRequest startExecutionRequest = new StartExecutionRequest().withStateMachineArn(stateMachineArn).withInput(inputEvent);
            StartExecutionResult startExecutionResult = stepFunctionsClient.startExecution(startExecutionRequest);
            System.out.println("Iteration : "+ repeatedTimes+ ", " + APPName+" execution complete! Time: " + new Date().toString());
        }
    }
}
