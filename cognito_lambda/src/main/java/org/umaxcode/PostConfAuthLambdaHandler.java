package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent;


public class PostConfAuthLambdaHandler implements RequestHandler<CognitoUserPoolEvent, CognitoUserPoolEvent> {

    @Override
    public CognitoUserPoolEvent handleRequest(CognitoUserPoolEvent event, Context context) {


        String triggerSource = event.getTriggerSource();
        System.out.println("Event data: " + event);
        switch (triggerSource) {
            case "PostConfirmation_ConfirmSignUp":
                handlePostConfirmation(event, context);
                break;
            case "PostAuthentication_Authentication":
                handlePostAuthentication(event, context);
                break;
            default:
                context.getLogger().log("Unhandled Cognito event trigger: " + triggerSource);
                break;
        }

        return event;
    }

    private void handlePostConfirmation(CognitoUserPoolEvent event, Context context) {
        String userId = event.getUserName();
        context.getLogger().log("User {} successfully signup: " + userId);
    }

    private void handlePostAuthentication(CognitoUserPoolEvent event, Context context) {
        String userId = event.getUserName();
        context.getLogger().log("User {} successfully authenticated: " + userId);
    }
}
