package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public String upload(MultipartFile pic, Jwt jwt) {

        List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/gif");

        if (pic.isEmpty() || !allowedMimeTypes.contains(pic.getContentType())) {
            throw new PhotoBlogException("Invalid file type. Only JPEG, PNG, and GIF are allowed.");
        }

        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        String email = jwt.getClaimAsString("email");
        return s3Service.upload(pic,
                email,
                firstName,
                lastName);
    }

    @Override
    public PhotoUploadDTo generatePreSignedUrl(String id, Jwt jwt) {

        String email = jwt.getClaimAsString("email");
        Map<String, AttributeValue> item = photoBlogRepository.getItem(id);
        System.out.println("Results: " + item.size() + "values " + item);
        if (!item.isEmpty()) {

            if (item.get("isPlacedInRecycleBin").n().equals("1")) {
                throw new PhotoBlogException("This image cannot be shared");
            }

            if (!item.get("owner").s().equalsIgnoreCase(email)) {
                throw new PhotoBlogException("Unauthorized operation on this image");
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
    public List<GetPhotoDto> getImages(String ownership, Jwt jwt) {
        OwnershipType type = OwnershipType.fromString(ownership);
        String email = jwt.getClaimAsString("email");
        List<Map<String, String>> details = photoBlogRepository.getItemsDetails(email, type);
        if (details.isEmpty()) {
            return List.of();
        }

        System.out.println("For testing " + details.size());

        return s3Service.getObjects(details);
    }

    @Override
    public void deleteImage(String id, Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Map<String, AttributeValue> deleteResponse = photoBlogRepository.deleteItem(id);
        if (!deleteResponse.isEmpty()) {
            String objectURL = deleteResponse.get("picUrl").s();
            s3Service.deleteObject(RECYCLE_BIN_PATH + email + "/" + extractObjectKey(objectURL));
            return;
        }

        throw new PhotoBlogException("Image with id = " + id + " does not exist.");
    }

    @Override
    public GetPhotoDto moveToRecycleBin(String id, Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Map<String, AttributeValue> returnedAttribute = photoBlogRepository.addItemToRecycleBin(id);
        String objectKey = extractObjectKey(returnedAttribute.get("picUrl").s());
        s3Service.moveObject(objectKey, RECYCLE_BIN_PATH + email + "/" + objectKey);
        return GetPhotoDto.builder()
                .imgId(returnedAttribute.get("picId").s())
                .build();
    }

    @Override
    public GetPhotoDto restoreFromRecycleBin(String id, Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Map<String, AttributeValue> returnedAttribute = photoBlogRepository.restoreFromRecycleBin(id);
        String objectKey = extractObjectKey(returnedAttribute.get("picUrl").s());
        s3Service.moveObject(RECYCLE_BIN_PATH + email + "/"
                + objectKey, objectKey);
        return GetPhotoDto.builder()
                .imgId(returnedAttribute.get("picId").s())
                .build();
    }

    @Override
    public List<GetPhotoDto> retrieveAllImagesInRecyclingBin(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        List<Map<String, String>> recycledItemsDetails = photoBlogRepository.getAllItemsInRecycleBin(email);
        if (recycledItemsDetails.isEmpty()) {
            return List.of();
        }

        return s3Service.getObjects(recycledItemsDetails);
    }

    private String extractObjectKey(String s3Url) {
        return s3Url.substring(s3Url.lastIndexOf("/") + 1);
    }
}
