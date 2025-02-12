package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.domain.dto.response.GetPhotoDto;
import org.umaxcode.domain.dto.response.PhotoUploadDTo;
import org.umaxcode.domain.enums.OwnershipType;
import org.umaxcode.exception.PhotoBlogException;
import org.umaxcode.repository.PhotoBlogRepository;
import org.umaxcode.service.PhotoBlogService;
import org.umaxcode.service.S3Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.net.URL;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PhotoBlogServiceImpl implements PhotoBlogService {

    private final PhotoBlogRepository photoBlogRepository;
    private final S3Service s3Service;
    private final String RECYCLE_BIN_PATH = "recycled/";


    @Override
    public String upload(MultipartFile pic) {

        return s3Service.upload(pic,
                "example@gmail.com",
                "firstName",
                "lastName");
    }

    @Override
    public PhotoUploadDTo generatePreSignedUrl(String id) {

        Map<String, AttributeValue> item = photoBlogRepository.getItem(id);

        if (!item.isEmpty()) {

            if (item.get("isPlacedInRecycleBin").s().equals("1")) {
                throw new PhotoBlogException("This image cannot be shared");
            }

            String objectURL = item.get("picUrl").s();
            URL url = s3Service.generatePreSignedUrl(extractObjectKey(objectURL), 3);

            return PhotoUploadDTo.builder()
                    .picUrl(url.toString())
                    .build();
        }

        throw new PhotoBlogException("Image with id = " + id + " does not exist.");
    }

    @Override
    public List<GetPhotoDto> getImages(String ownership) {
        OwnershipType type = OwnershipType.fromString(ownership);
        List<Map<String, String>> details = photoBlogRepository.getItemsDetails("example@gmail.com", type);
        if (details.isEmpty()) {
            return List.of();
        }

        return s3Service.getObjects(details);
    }

    @Override
    public void deleteImage(String id) {

        Map<String, AttributeValue> deleteResponse = photoBlogRepository.deleteItem(id);
        if (!deleteResponse.isEmpty()) {
            String objectURL = deleteResponse.get("picUrl").s();
            s3Service.deleteObject(RECYCLE_BIN_PATH + extractObjectKey(objectURL));
            return;
        }

        throw new PhotoBlogException("Image with id = " + id + " does not exist.");
    }

    @Override
    public GetPhotoDto moveToRecycleBin(String id) {
        Map<String, AttributeValue> returnedAttribute = photoBlogRepository.addItemToRecycleBin(id);
        String objectKey = extractObjectKey(returnedAttribute.get("picUrl").s());
        s3Service.moveObject(objectKey, RECYCLE_BIN_PATH + objectKey);
        return GetPhotoDto.builder()
                .imgId(returnedAttribute.get("picId").s())
                .build();
    }

    @Override
    public GetPhotoDto restoreFromRecycleBin(String id) {
        Map<String, AttributeValue> returnedAttribute = photoBlogRepository.restoreFromRecycleBin(id);
        String objectKey = extractObjectKey(returnedAttribute.get("picUrl").s());
        s3Service.moveObject(RECYCLE_BIN_PATH + objectKey, objectKey);
        return GetPhotoDto.builder()
                .imgId(returnedAttribute.get("picId").s())
                .build();
    }

    private String extractObjectKey(String s3Url) {
        return s3Url.substring(s3Url.lastIndexOf("/") + 1);
    }
}
