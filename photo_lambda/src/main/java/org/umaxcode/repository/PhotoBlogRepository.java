package org.umaxcode.repository;

import org.umaxcode.domain.enums.OwnershipType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface PhotoBlogRepository {

    Map<String, AttributeValue> getItem(String id);

    Map<String, AttributeValue> deleteItem(String id);

    List<Map<String, String>> getItemsDetails(String email, OwnershipType ownershipType);

    Map<String, AttributeValue>  addItemToRecycleBin(String id);

    Map<String, AttributeValue> restoreFromRecycleBin(String id);
}
