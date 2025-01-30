package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.util.Map;


public class ImageProcessorLambdaHandler implements RequestHandler<Map<String, Object>, Void> {

    private final S3Client s3Client;

    public ImageProcessorLambdaHandler() {
        this.s3Client = S3Client.create();
    }

    @Override
    public Void handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Processing event: " + event);

        try {
            // Extract S3 bucket name and object key from event
            Map<String, Object> detail = (Map<String, Object>) event.get("detail");
            Map<String, Object> bucket = (Map<String, Object>) detail.get("bucket");
            Map<String, Object> object = (Map<String, Object>) detail.get("object");

            String bucketName = (String) bucket.get("name");
            String objectKey = (String) object.get("key");

            Map<String, String> metadata = getS3ObjectMetadata(bucketName, objectKey, context);
            context.getLogger().log("Email: " + metadata.get("email"));
            context.getLogger().log("FirstName: " + metadata.get("firstname"));
            context.getLogger().log("LastName: " + metadata.get("lastname"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private Map<String, String> getS3ObjectMetadata(String bucketName, String objectKey, Context context) {

        // Query S3 metadata
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        HeadObjectResponse response = s3Client.headObject(headRequest);

        context.getLogger().log("Uploaded by metadata: " + response.metadata());

        return response.metadata();
    }
}
