package org.umaxcode.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.umaxcode.domain.dto.response.GetPhotoDto;
import org.umaxcode.domain.enums.OwnershipType;
import org.umaxcode.exception.PhotoBlogException;
import org.umaxcode.repository.PhotoBlogRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PhotoBlogRepositoryImpl implements PhotoBlogRepository {

    private final DynamoDbClient dynamoDbClient;
    @Value("${application.aws.tableName}")
    private String tableName;

    @Override
    public Map<String, AttributeValue> getItem(String id) {

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("picId", AttributeValue.builder().s(id).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        return dynamoDbClient.getItem(request).item();
    }

    @Override
    public List<GetPhotoDto> getItemsDetails(String email, OwnershipType ownershipType) {

        if (OwnershipType.OWN_PHOTO.equals(ownershipType)) {
            return getByOwner(email).stream()
                    .map(photo -> GetPhotoDto.builder()
                            .imgId(photo.get("picId").s())
                            .image(photo.get("picUrl").s())
                            .uploadDateTime(photo.get("dateOfUpload").s())
                            .resignUrlGenDateTime(photo.get("preSignedUrlGenDate").s())
                            .build())
                    .toList();
        }

        return getByOthers(email).stream()
                .map(photo -> GetPhotoDto.builder()
                        .imgId(photo.get("picId").s())
                        .image(photo.get("picUrl").s())
                        .uploadDateTime(photo.get("dateOfUpload").s())
                        .resignUrlGenDateTime(photo.get("preSignedUrlGenDate").s())
                        .build())
                .toList();
    }

    private List<Map<String, AttributeValue>> getByOwner(String email) {
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .indexName("ownerIndex")
                .keyConditionExpression("#owner = :email")
                .filterExpression("isPlacedInRecycleBin = :false")
                .expressionAttributeValues(Map.of(
                        ":email", AttributeValue.builder().s(email).build(),
                        ":false", AttributeValue.builder().n("0").build()
                ))
                .expressionAttributeNames(Map.of(
                        "#owner", "owner"
                ))
                .build();

        return dynamoDbClient.query(queryRequest).items();
    }

    private List<Map<String, AttributeValue>> getByOthers(String email) {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("#owner <> :email AND isPlacedInRecycleBin = :false")
                .expressionAttributeValues(Map.of(
                        ":email", AttributeValue.builder().s(email).build(),
                        ":false", AttributeValue.builder().n("0").build()
                ))
                .expressionAttributeNames(Map.of(
                        "#owner", "owner"
                ))
                .build();

        return dynamoDbClient.scan(scanRequest).items();
    }

    @Override
    public Map<String, AttributeValue> deleteItem(String id) {

        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("picId", AttributeValue.builder().s(id).build());

            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .conditionExpression("isPlacedInRecycleBin = :true")
                    .expressionAttributeValues(Map.of(
                            ":true", AttributeValue.builder().n("1").build()
                    ))
                    .returnValues("ALL_OLD")
                    .build();

            DeleteItemResponse deleteResponse = dynamoDbClient.deleteItem(deleteRequest);

            return deleteResponse.attributes();
        } catch (ConditionalCheckFailedException ex) {
            throw new PhotoBlogException("Photo in recycling bin can only be permanently deleted");
        }
    }

    @Override
    public Map<String, AttributeValue> addItemToRecycleBin(String id) {

        try {
            Map<String, AttributeValue> key = Map.of(
                    "picId", AttributeValue.builder().s(id).build()
            );

            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET isPlacedInRecycleBin = :true")
                    .conditionExpression("isPlacedInRecycleBin = :false")
                    .expressionAttributeValues(Map.of(
                            ":true", AttributeValue.builder().n("1").build(),
                            ":false", AttributeValue.builder().n("0").build()
                    ))
                    .returnValues("ALL_NEW")
                    .build();


            UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);
            return updateItemResponse.attributes();
        } catch (ConditionalCheckFailedException ex) {
            throw new PhotoBlogException("Photo is already added to recycle bin");
        }

    }

    @Override
    public Map<String, AttributeValue> restoreFromRecycleBin(String id) {
        try {
            Map<String, AttributeValue> key = Map.of(
                    "picId", AttributeValue.builder().s(id).build()
            );

            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET isPlacedInRecycleBin = :false")
                    .conditionExpression("isPlacedInRecycleBin = :true")
                    .expressionAttributeValues(Map.of(
                            ":false", AttributeValue.builder().n("0").build(),
                            ":true", AttributeValue.builder().n("1").build()
                    ))
                    .returnValues("ALL_NEW")
                    .build();

            UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);
            return updateItemResponse.attributes();
        } catch (ConditionalCheckFailedException ex) {
            throw new PhotoBlogException("Photo has already been restored");
        }
    }

    @Override
    public List<GetPhotoDto> getAllItemsInRecycleBin(String email) {

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("#owner = :email AND isPlacedInRecycleBin = :true")
                .expressionAttributeValues(Map.of(
                        ":email", AttributeValue.builder().s(email).build(),
                        ":true", AttributeValue.builder().n("1").build()
                ))
                .expressionAttributeNames(Map.of(
                        "#owner", "owner"
                ))
                .build();

        List<Map<String, AttributeValue>> items = dynamoDbClient.scan(scanRequest).items();

        return items.stream()
                .map(photo -> GetPhotoDto.builder()
                        .imgId(photo.get("picId").s())
                        .image(photo.get("picUrl").s())
                        .uploadDateTime(photo.get("dateOfUpload").s())
                        .build())
                .toList();
    }

    @Override
    public void updatePreSignedUrlsInDynamo(String id, String imageUrl) {
        try {
            Map<String, AttributeValue> key = Map.of(
                    "picId", AttributeValue.builder().s(id).build()
            );

            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET picUrl = :preSignedUrl, preSignedUrlGenDate = :signedDate")
                    .expressionAttributeValues(Map.of(
                            ":preSignedUrl", AttributeValue.builder().s(imageUrl).build(),
                            ":signedDate", AttributeValue.builder().s(LocalDateTime.now().toString()).build()
                    ))
                    .build();

            dynamoDbClient.updateItem(updateItemRequest);
        } catch (Exception ex) {
            throw new PhotoBlogException(ex.getMessage());
        }
    }
}
