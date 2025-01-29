package org.umaxcode.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.umaxcode.repository.PhotoBlogRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PhotoBlogRepositoryImpl implements PhotoBlogRepository {

    private final DynamoDbClient dynamoDbClient;

    @Value("${application.aws.tableName}")
    private String tableName;


    @Override
    public void createItem(String photoUrl, String owner) {

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("picId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("picUrl", AttributeValue.builder().s(photoUrl).build());
        item.put("owner", AttributeValue.builder().s(owner).build());

        PutItemRequest putRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(putRequest);
    }
}
