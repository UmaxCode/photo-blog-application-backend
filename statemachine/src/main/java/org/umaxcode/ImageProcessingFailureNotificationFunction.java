package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.HashMap;
import java.util.Map;

public class ImageProcessingFailureNotificationFunction implements RequestHandler<Map<String, Object>, Void> {

    private final String topicArn;
    private final SnsClient snsClient;

    public ImageProcessingFailureNotificationFunction() {
        this.topicArn = System.getenv("IMG_PRO_NOTIFICATION_TOPIC_ARN");
        this.snsClient = SnsClient.create();
    }

    @Override
    public Void handleRequest(Map<String, Object> event, Context context) {

        String email = (String) event.get("email");
        String objectKey = (String) event.get("objectKey");

        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("endpointEmail", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue("xidide7271@eluxeer.com")
                .build());

        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)
                .subject("Image processing Failed")
                .message("There was an error processing the image: " + objectKey)
                .messageAttributes(messageAttributes)
                .build();

        // Publish the message
        try {
            snsClient.publish(publishRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        context.getLogger().log("Event" + event);
        context.getLogger().log("Send processing failed notification");
        return null;
    }
}
