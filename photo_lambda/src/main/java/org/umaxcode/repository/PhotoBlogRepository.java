package org.umaxcode.repository;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface PhotoBlogRepository {

    Map<String, AttributeValue> getItem(String id);
}
