package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.umaxcode.exception.ImageProcessingException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


public class ImageProcessorLambdaHandler implements RequestHandler<Map<String, Object>, Void> {

    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String websocketMessageEndpoint;
    private final String primaryBucketName;
    private final S3Presigner s3Presigner;
    private final ObjectMapper objectMapper;
    private final String connectTableName;

    public ImageProcessorLambdaHandler() {
        this.s3Presigner = S3Presigner.create();
        this.s3Client = S3Client.create();
        this.dynamoDbClient = DynamoDbClient.create();
        this.tableName = System.getenv("AWS_DYNAMODB_TABLE_NAME");
        this.primaryBucketName = System.getenv("AWS_S3_PRIMARY_BUCKET_NAME");
        this.objectMapper = new ObjectMapper();
        this.websocketMessageEndpoint = System.getenv("API_GATEWAY_WEBSOCKET_ENDPOINT");
        this.connectTableName = System.getenv("WEBSOCKET_CON_TABLE_NAME");
    }

    @Override
    public Void handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Processing event: " + event);

        // Extract S3 bucket name and object key from event
        Map<String, Object> detail = (Map<String, Object>) event.get("detail");
        Map<String, Object> bucket = (Map<String, Object>) detail.get("bucket");
        Map<String, Object> object = (Map<String, Object>) detail.get("object");

        String objectKey = (String) object.get("key");
        String bucketName = (String) bucket.get("name");

        ResponseInputStream<GetObjectResponse> s3ObjectResponse = getS3Object(bucketName, objectKey, context);

        Map<String, String> metadata = s3ObjectResponse.response().metadata();
        String email = metadata.get("email");

        try {

            // Add watermark to image, save processed image and return url
            String processedImageUrl = processPhotoAndReturnUrl(s3ObjectResponse, bucketName, objectKey, context);

            context.getLogger().log("Processed image URL: " + processedImageUrl);

            notifyClientOfSuccessfulImageProcessing(email);

            deleteImageFromStagingBucket(bucketName, objectKey);

        } catch (Exception ex) {

            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("reason", ex.getMessage());
            errorDetails.put("email", email);
            errorDetails.put("objectKey", objectKey);
            try {
                throw new ImageProcessingException(objectMapper.writeValueAsString(errorDetails));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private String processPhotoAndReturnUrl(ResponseInputStream<GetObjectResponse> s3ObjectResponse, String bucketName, String objectKey, Context context) throws IOException {

        Map<String, String> metadata = s3ObjectResponse.response().metadata();
        String email = metadata.get("email");
        String firstName = metadata.get("firstname");
        String lastName = metadata.get("lastname");
        String fullName = firstName + " " + lastName;

        byte[] processedImageContent = addImageWatermark(s3ObjectResponse, fullName, context);

        uploadImageToPrimaryBucket(processedImageContent, objectKey, context);

        String preSignedUrl = generatePreSignedUrl(objectKey).toString();
        storePhotoUrl(preSignedUrl, email, context);
        return preSignedUrl;
    }

    private URL generatePreSignedUrl(String objectKey) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(primaryBucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest preSignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(24))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(preSignRequest).url();
    }

    private ResponseInputStream<GetObjectResponse> getS3Object(String bucketName, String objectKey, Context context) {

        // Query S3 metadata
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        ResponseInputStream<GetObjectResponse> objectResponse = s3Client.getObject(getObjectRequest);

        context.getLogger().log("Metadata: " + objectResponse.response().metadata());

        return objectResponse;
    }


    private byte[] addImageWatermark(ResponseInputStream<GetObjectResponse> s3ObjectResponse, String fullName, Context context) throws IOException {

        BufferedImage originalImage = ImageIO.read(s3ObjectResponse);

        // Create new watermarked image
        BufferedImage watermarkedImage = new BufferedImage(originalImage.getWidth(),
                originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = watermarkedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);

        // Set watermark properties
        Font font = new Font("Arial", Font.BOLD, 25); // Smaller font size
        g2d.setFont(font);
        g2d.setColor(new Color(255, 0, 0, 216)); // Red with transparency
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

        // Get the current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        // Create the watermark text
        String line1 = "Owner: " + fullName;
        String line2 = "Date of Upload: " + currentDate;

        // Get the FontMetrics to calculate the width and height of the text
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidthLine1 = fontMetrics.stringWidth(line1);
        int textWidthLine2 = fontMetrics.stringWidth(line2);
        int textHeight = fontMetrics.getHeight();

        // Calculate centered positions
        int xLine1 = (originalImage.getWidth() - textWidthLine1) / 2; // Center horizontally
        int xLine2 = (originalImage.getWidth() - textWidthLine2) / 2; // Center horizontally
        int yCenter = originalImage.getHeight() / 2; // Center vertically
        int yLine1 = yCenter - textHeight / 2; // Adjust first line
        int yLine2 = yCenter + textHeight; // Adjust second line below

        // Draw the text
        g2d.drawString(line1, xLine1, yLine1);
        g2d.drawString(line2, xLine2, yLine2);

        g2d.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(watermarkedImage, "png", outputStream);
        context.getLogger().log("Watermark added to image at bottom-right");
        return outputStream.toByteArray();
    }

    private void uploadImageToPrimaryBucket(byte[] imageContent, String objectKey, Context context) {

        String extension = getImageExtension(objectKey);
        String contentType = String.format("image/%s", extension);

        // Upload the image back to S3
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(primaryBucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageContent));

        context.getLogger().log("Watermarked image uploaded to: " + primaryBucketName + "/" + objectKey);
    }

    private String getImageExtension(String objectKey) {

        Set<String> VALID_IMAGE_EXTENSIONS = Set.of("jpeg", "jpg", "png", "gif", "bmp", "webp", "tiff", "svg");
        if (objectKey == null) {
            throw new ImageProcessingException("No object key");
        }

        String extension = objectKey.substring(objectKey.lastIndexOf('.') + 1).toLowerCase();

        if (VALID_IMAGE_EXTENSIONS.contains(extension)) {
            return extension; // Valid image extension
        }

        throw new ImageProcessingException("Unsupported image extension: " + extension);
    }

    private void storePhotoUrl(String photoUrl, String owner, Context context) {

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("picId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("picUrl", AttributeValue.builder().s(photoUrl).build());
        item.put("owner", AttributeValue.builder().s(owner).build());
        item.put("isPlacedInRecycleBin", AttributeValue.builder().n("0").build());
        item.put("dateOfUpload", AttributeValue.builder().s(LocalDateTime.now().toString()).build());
        item.put("preSignedUrlGenDate", AttributeValue.builder().s(LocalDateTime.now().toString()).build());

        PutItemRequest putRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(putRequest);
        context.getLogger().log("Photo stored in dynamoDB");
    }

    private void notifyClientOfSuccessfulImageProcessing(String email) throws JsonProcessingException {

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(connectTableName)
                .indexName("emailIndex")
                .keyConditionExpression("#email = :email")
                .expressionAttributeValues(Map.of(
                        ":email", AttributeValue.builder().s(email).build()
                ))
                .expressionAttributeNames(Map.of(
                        "#email", "email"
                ))
                .build();

        QueryResponse queryResult = dynamoDbClient.query(queryRequest);

        if (queryResult == null || !queryResult.hasItems()) { // don't notify frontend when websocket connection is not established
            return;
        }

        for (Map<String, AttributeValue> connection : queryResult.items()) {

            String connectionId = connection.get("connectionId").s();

            ApiGatewayManagementApiClient client = ApiGatewayManagementApiClient.builder()
                    .endpointOverride(URI.create(websocketMessageEndpoint))
                    .build();

            Map<String, String> response = new HashMap<>();
            response.put("message", "Image processed successfully");

            PostToConnectionRequest postRequest = PostToConnectionRequest.builder()
                    .connectionId(connectionId)
                    .data(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(response)))
                    .build();

            try {
                client.postToConnection(postRequest);
                System.out.println("Message sent successfully!");
            } catch (GoneException e) {
                System.err.println("Connection is stale: ");
            } catch (Exception e) {
                System.err.println("General error: " + e.getMessage());
            }
        }

    }

    private void deleteImageFromStagingBucket(String bucketName, String objectKey) {

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        s3Client.deleteObject(deleteRequest);
    }
}
