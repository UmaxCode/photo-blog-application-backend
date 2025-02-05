package org.umaxcode.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.umaxcode.repository.PhotoBlogRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.util.HashMap;
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
