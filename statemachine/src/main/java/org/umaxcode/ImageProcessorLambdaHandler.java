package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ImageProcessorLambdaHandler implements RequestHandler<Map<String, Object>, Void> {

    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String stageBucketName;
    private final String primaryBucketName;

    public ImageProcessorLambdaHandler() {
        this.s3Client = S3Client.create();
        this.dynamoDbClient = DynamoDbClient.create();
        this.tableName = System.getenv("AWS_DYNAMODB_TABLE_NAME");
        this.stageBucketName = System.getenv("AWS_S3_STAGE_BUCKET_NAME");
        this.primaryBucketName = System.getenv("AWS_S3_PRIMARY_BUCKET_NAME");
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

            String email = metadata.get("email");
            String firstName = metadata.get("firstname");
            String lastName = metadata.get("lastname");

            // Add watermark to image
            String processedImageUrl = processPhoto(firstName, lastName, context);

            // Store processImage in dynamoDB
            storePhoto(processedImageUrl, email, context);

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

    private String processPhoto(String firstName, String lastName, Context context) {

        return "url-path";
    }

    private void storePhoto(String photoUrl, String owner, Context context) {

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("picId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("picUrl", AttributeValue.builder().s(photoUrl).build());
        item.put("owner", AttributeValue.builder().s(owner).build());
        item.put("date", AttributeValue.builder().s(LocalDateTime.now().toString()).build());

        PutItemRequest putRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(putRequest);
        context.getLogger().log("Stored photo: " + photoUrl);
    }
}
