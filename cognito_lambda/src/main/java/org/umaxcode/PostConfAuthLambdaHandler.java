package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostAuthenticationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


public class PostConfAuthLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final ObjectMapper objectMapper;
    private final SnsClient snsClient;
    private final String topicArn;

    public PostConfAuthLambdaHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.snsClient = SnsClient.create();
        this.topicArn = System.getenv("SNS_NOTIFICATION_TOPIC_ARN");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Event" + event);
        String triggerSource = (String) event.get("triggerSource");

        switch (triggerSource) {
            case "PostConfirmation_ConfirmSignUp":
                CognitoUserPoolPostConfirmationEvent postConfirmationEvent = objectMapper.convertValue(event, CognitoUserPoolPostConfirmationEvent.class);
                handlePostConfirmation(postConfirmationEvent, context);
                break;
            case "PostAuthentication_Authentication":
                CognitoUserPoolPostAuthenticationEvent postAuthenticationEvent = objectMapper.convertValue(event, CognitoUserPoolPostAuthenticationEvent.class);
                handlePostAuthentication(postAuthenticationEvent, context);
                break;
            default:
                context.getLogger().log("Unhandled Cognito event trigger: " + triggerSource);
                break;
        }

        return null;
    }

    private void handlePostConfirmation(CognitoUserPoolPostConfirmationEvent event, Context context) {
        String email = event.getRequest().getUserAttributes().get("email");

        SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .protocol("email")
                .endpoint(email)
                .returnSubscriptionArn(true)
                .topicArn(topicArn)
                .build();

        try {
            SubscribeResponse response = snsClient.subscribe(subscribeRequest);
            context.getLogger().log("Subscription result: " + response);

            // Define a filter policy
            String filterPolicy = String.format("{ \"endpointEmail\": [\"%s\"] }", email);

            // Set the filter policy for the subscription
            String subscriptionArn = response.subscriptionArn();

            System.out.println("SubscriptionArn" + subscriptionArn);
            SetSubscriptionAttributesRequest filterPolicyRequest = SetSubscriptionAttributesRequest.builder()
                    .subscriptionArn(subscriptionArn)
                    .attributeName("FilterPolicy")
                    .attributeValue(filterPolicy)
                    .build();

            snsClient.setSubscriptionAttributes(filterPolicyRequest);

            context.getLogger().log("Filter policy set for subscription: " + subscriptionArn);
            context.getLogger().log("Successfully subscribed " + email + " to the SNS topic with filter policy: " + topicArn);

        } catch (Exception e) {
            context.getLogger().log("Error subscribing user: " + e.getMessage());
        }

    }

    private void handlePostAuthentication(CognitoUserPoolPostAuthenticationEvent event, Context context) {
        String email = event.getRequest().getUserAttributes().get("email");
        String firstName = event.getRequest().getUserAttributes().get("given_name");

        String subject = "Successful Login Notification";

        String htmlMessage = String.format("""
                   <html>
                             <body>
                                 <h2>Login Notification</h2>
                                 <p>Hello <strong>%s</strong>,</p>
                                 <p>We noticed a login to your account on <b>%s</b>.</p>
                                 <p><strong>Login Details:</strong></p>
                                 <ul>
                                     <li><b>Date & Time:</b> %s</li
                                 </ul>
                             </body>
                         </html>
                """, firstName, "UmaxPhotoShare", LocalDateTime.now());

        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("endpointEmail", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(email)
                .build());

        PublishRequest publishRequest = PublishRequest.builder()
                .subject(subject)
                .topicArn(topicArn)
                .message(htmlMessage)
                .messageAttributes(messageAttributes)
                .build();

        try {
            snsClient.publish(publishRequest);
        } catch (Exception ex) {
            context.getLogger().log("Error publishing sns to user: " + ex.getMessage());
        }

        String userId = event.getUserName();
        context.getLogger().log("User {} successfully authenticated: " + userId);
    }
}
