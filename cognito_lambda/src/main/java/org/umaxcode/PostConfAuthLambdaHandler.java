package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostAuthenticationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;


public class PostConfAuthLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final ObjectMapper objectMapper;
    private final String topicArn;
    private final String secondaryUserPoolId;
    private final SqsClient sqsClient;
    private final String queueUrl;

    public PostConfAuthLambdaHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.sqsClient = SqsClient.create();
        this.topicArn = System.getenv("SNS_NOTIFICATION_TOPIC_ARN");
        this.secondaryUserPoolId = System.getenv("SECONDARY_USER_POOL_ID");
        this.queueUrl = System.getenv("QUEUE_URL");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Event" + event);
        String triggerSource = (String) event.get("triggerSource");

        // Add attributes to the message
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

        switch (triggerSource) {
            case "PostConfirmation_ConfirmSignUp":
                CognitoUserPoolPostConfirmationEvent postConfirmationEvent = objectMapper.convertValue(event, CognitoUserPoolPostConfirmationEvent.class);
                handlePostConfirmation(postConfirmationEvent, messageAttributes, context);
                break;
            case "PostAuthentication_Authentication":
                CognitoUserPoolPostAuthenticationEvent postAuthenticationEvent = objectMapper.convertValue(event, CognitoUserPoolPostAuthenticationEvent.class);
                handlePostAuthentication(postAuthenticationEvent, messageAttributes, context);
                break;
            default:
                context.getLogger().log("Unhandled Cognito event trigger: " + triggerSource);
                return event;
        }

        messageAttributes.put("triggerSource", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(triggerSource)
                .build());

        messageAttributes.put("topicArn", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(topicArn)
                .build());

        // Send the message to SQS with attributes
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody("Cognito message for sqs")
                .messageAttributes(messageAttributes)
                .build();

        sqsClient.sendMessage(sendMessageRequest);

        return event;
    }

    private void handlePostAuthentication(CognitoUserPoolPostAuthenticationEvent event, Map<String, MessageAttributeValue> sqsMessage, Context context) {
        String email = event.getRequest().getUserAttributes().get("email");
        String firstname = event.getRequest().getUserAttributes().get("given_name");
        sqsMessage.put("email", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(email)
                .build());

        sqsMessage.put("firstname", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(firstname)
                .build());

        context.getLogger().log("Preparing post authentication sqs message");
    }

    private void handlePostConfirmation(CognitoUserPoolPostConfirmationEvent event, Map<String, MessageAttributeValue> sqsMessage, Context context) {
        String email = event.getRequest().getUserAttributes().get("email");
        String username = event.getUserName();
        String given_name = event.getRequest().getUserAttributes().get("given_name");
        String family_name = event.getRequest().getUserAttributes().get("family_name");

        sqsMessage.put("email", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(email)
                .build());

        sqsMessage.put("username", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(username)
                .build());

        sqsMessage.put("firstname", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(given_name)
                .build());

        sqsMessage.put("lastname", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(family_name)
                .build());

        sqsMessage.put("secondaryCognitoPoolId", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(this.secondaryUserPoolId)
                .build());

        context.getLogger().log("Preparing post confirmation sqs message");
    }
}
