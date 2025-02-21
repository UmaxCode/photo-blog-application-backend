package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQSPostConfAuthLambdaHandler implements RequestHandler<SQSEvent, Void> {

    private final SnsClient snsClient;
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

    public SQSPostConfAuthLambdaHandler() {
        this.snsClient = SnsClient.create();
        this.cognitoIdentityProviderClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(System.getenv("SECONDARY_REGION")))
                .build();
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            // Process each message
            String triggerSource = message.getMessageAttributes().get("triggerSource").getStringValue();
            String email = message.getMessageAttributes().get("email").getStringValue();
            String topicArn = message.getMessageAttributes().get("topicArn").getStringValue();
            String firstName = message.getMessageAttributes().get("firstname").getStringValue();
            switch (triggerSource) {
                case "PostConfirmation_ConfirmSignUp":
                    String lastName = message.getMessageAttributes().get("lastname").getStringValue();
                    String username = message.getMessageAttributes().get("username").getStringValue();
                    String secondaryUserPoolId = message.getMessageAttributes().get("secondaryUserPoolId").getStringValue();
                    handlePostConfirmation(email, username, firstName,
                            lastName, secondaryUserPoolId, topicArn);
                    break;
                case "PostAuthentication_Authentication":
                    handlePostAuthentication(email, firstName, topicArn);
                    break;
                default:
                    context.getLogger().log("Unhandled Cognito event trigger: " + triggerSource);
                    break;
            }

        }

        return null;
    }

    private void handlePostConfirmation(
            String email,
            String username,
            String firstname,
            String lastname,
            String secondaryUserPoolId,
            String topicArn
    ) {

        sendSNSNotificationSubscription(email, topicArn);
        replicateUserDetailInSecondaryRegion(username, firstname, lastname,
                email, secondaryUserPoolId);
    }

    private void sendSNSNotificationSubscription(String email, String topicArn) {

        SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .protocol("email")
                .endpoint(email)
                .returnSubscriptionArn(true)
                .topicArn(topicArn)
                .build();

        try {
            SubscribeResponse response = snsClient.subscribe(subscribeRequest);

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

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void replicateUserDetailInSecondaryRegion(String username,
                                                      String firstname,
                                                      String lastname,
                                                      String email,
                                                      String secondaryUserPoolId) {
        if (secondaryUserPoolId.isEmpty()) { // avoid replicating user pool details from
            // secondary region to primary region
            return;
        }

        try {

            List<AttributeType> userAttributes = new ArrayList<>();
            userAttributes.add(AttributeType.builder().name("email").value(email).build());
            userAttributes.add(AttributeType.builder().name("family_name").value(firstname).build());
            userAttributes.add(AttributeType.builder().name("given_name").value(lastname).build());
            userAttributes.add(AttributeType.builder().name("email_verified").value("true").build());

            AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                    .userPoolId(secondaryUserPoolId)
                    .username(username)
                    .userAttributes(userAttributes)
                    .messageAction("SUPPRESS")
                    .build();

            cognitoIdentityProviderClient.adminCreateUser(createUserRequest);
        } catch (Exception ex) {
            System.out.println("Error replicating user detail: " + ex.getMessage());
        }
    }

    private void handlePostAuthentication(String email, String firstName, String topicArn) {

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
            System.out.println("Error publishing sns to user: " + ex.getMessage());
        }

        System.out.println("User {} successfully authenticated: ");
    }
}
