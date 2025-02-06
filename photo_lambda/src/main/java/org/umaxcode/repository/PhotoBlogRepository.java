package org.umaxcode.repository;

import org.umaxcode.domain.enums.OwnershipType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface PhotoBlogRepository {

    Map<String, AttributeValue> getItem(String id);

    Map<String, AttributeValue> deleteItem(String id);

    List<String> getItemsPicUrlKey(String email, OwnershipType ownershipType);

}
