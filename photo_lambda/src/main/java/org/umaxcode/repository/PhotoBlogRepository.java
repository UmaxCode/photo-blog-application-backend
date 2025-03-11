package org.umaxcode.repository;

import org.umaxcode.domain.dto.response.GetPhotoDto;
import org.umaxcode.domain.enums.OwnershipType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface PhotoBlogRepository {

    Map<String, AttributeValue> getItem(String id);

    Map<String, AttributeValue> deleteItem(String id);

    List<GetPhotoDto> getItemsDetails(String email, OwnershipType ownershipType);

    Map<String, AttributeValue>  addItemToRecycleBin(String id);

    void updatePreSignedUrlsInDynamo(String id, String imageUrl);

    Map<String, AttributeValue> restoreFromRecycleBin(String id);

    List<GetPhotoDto> getAllItemsInRecycleBin(String email);
}
