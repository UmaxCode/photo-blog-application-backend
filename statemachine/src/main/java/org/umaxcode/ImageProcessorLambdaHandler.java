package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ImageProcessorLambdaHandler implements RequestHandler<Map<String, Object>, Void> {

    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String primaryBucketName;
    private final String awsRegion;

    public ImageProcessorLambdaHandler() {
        this.s3Client = S3Client.create();
        this.dynamoDbClient = DynamoDbClient.create();
        this.tableName = System.getenv("AWS_DYNAMODB_TABLE_NAME");
        this.primaryBucketName = System.getenv("AWS_S3_PRIMARY_BUCKET_NAME");
        this.awsRegion = System.getenv("AWS_REGION");
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

            // Add watermark to image, save processed image and return url
            String processedImageUrl = processPhotoAndReturnUrl(bucketName, objectKey, context);

            context.getLogger().log("Processed image URL: " + processedImageUrl);

            //:TODO delete unprocess image from staging bucket

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private String processPhotoAndReturnUrl(String bucketName, String objectKey, Context context) throws IOException {

        ResponseInputStream<GetObjectResponse> s3ObjectResponse = getS3Object(bucketName, objectKey, context);

        Map<String, String> metadata = s3ObjectResponse.response().metadata();
        String email = metadata.get("email");
        String firstName = metadata.get("firstname");
        String lastName = metadata.get("lastname");
        String fullName = firstName + " " + lastName;

        byte[] processedImageContent = addImageWatermark(s3ObjectResponse, fullName, context);

        uploadImageToPrimaryBucket(processedImageContent, objectKey, context);

        String imageUrl = "https://" + bucketName + ".s3." + this.awsRegion + ".amazonaws.com/" + objectKey;
        storePhotoUrl(imageUrl, email, context);
        return imageUrl;
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
        g2d.setColor(new Color(255, 0, 0, 100)); // Red with transparency
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

        // Calculate positions for bottom-right placement
        int margin = 30; // Margin from the bottom and right edges
        int xLine1 = originalImage.getWidth() - textWidthLine1 - margin; // Bottom-right for line 1
        int xLine2 = originalImage.getWidth() - textWidthLine2 - margin; // Bottom-right for line 2
        int yLine1 = originalImage.getHeight() - textHeight * 2 - margin; // First line position
        int yLine2 = originalImage.getHeight() - textHeight - margin; // Second line position

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

        // Upload the image back to S3
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(primaryBucketName)
                .key(objectKey)
                .contentType("image/png")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageContent));

        context.getLogger().log("Watermarked image uploaded to: " + primaryBucketName + "/" + objectKey);
    }

    private void storePhotoUrl(String photoUrl, String owner, Context context) {

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("picId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("picUrl", AttributeValue.builder().s(photoUrl).build());
        item.put("owner", AttributeValue.builder().s(owner).build());
        item.put("dateOfUpload", AttributeValue.builder().s(LocalDateTime.now().toString()).build());

        PutItemRequest putRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(putRequest);
        context.getLogger().log("Photo stored in dynamoDB");
    }
}
