package org.umaxcode.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.umaxcode.domain.enums.OwnershipType;
import org.umaxcode.repository.PhotoBlogRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

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
    public List<String> getItemsPicUrlKey(String email, OwnershipType ownershipType) {
        if (OwnershipType.OWN_PHOTO.equals(ownershipType)) {
            return getByOwner(email).stream()
                    .map(photo -> extractObjectKey(photo.get("picUrl").s()))
                    .toList();
        }

        return getByOthers(email).stream()
                .map(photo -> extractObjectKey(photo.get("picUrl").s()))
                .toList();
    }

    private String extractObjectKey(String s3Url) {
        return s3Url.substring(s3Url.lastIndexOf("/") + 1);
    }

    private List<Map<String, AttributeValue>> getByOwner(String email) {
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .indexName("ownerIndex")
                .keyConditionExpression("owner = :email")
                .expressionAttributeValues(Map.of(
                        ":email", AttributeValue.builder().s(email).build()
                ))
                .build();

        return dynamoDbClient.query(queryRequest).items();
    }

    private List<Map<String, AttributeValue>> getByOthers(String email) {
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .indexName("ownerIndex")
                .keyConditionExpression("owner <> :email")
                .expressionAttributeValues(Map.of(
                        ":email", AttributeValue.builder().s(email).build()
                ))
                .build();

        return dynamoDbClient.query(queryRequest).items();
    }

    @Override
    public Map<String, AttributeValue> deleteItem(String id) {

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("picId", AttributeValue.builder().s(id).build());

        DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .returnValues("ALL_OLD")
                .build();

        DeleteItemResponse deleteResponse = dynamoDbClient.deleteItem(deleteRequest);

        return deleteResponse.attributes();
    }
}
