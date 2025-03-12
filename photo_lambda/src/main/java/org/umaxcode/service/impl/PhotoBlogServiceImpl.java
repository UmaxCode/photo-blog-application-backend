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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
        List<GetPhotoDto> photoDetails = photoBlogRepository.getItemsDetails(email, type);
        return checkAndRefreshPreSignedUrl(photoDetails);
    }

    private List<GetPhotoDto> checkAndRefreshPreSignedUrl(List<GetPhotoDto> photoDetails) {

        if (photoDetails.isEmpty()) {
            return List.of();
        }

        // Check if any preSigned URLs need regeneration (24 hours old)
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<GetPhotoDto> updatedDetails = new ArrayList<>();

        for (GetPhotoDto photo : photoDetails) {
            // Check if URL generation time is older than 24 hours
            // Parse the date string from DynamoDB
            LocalDateTime preSignedUrlGenDate = parseDateTime(photo.getResignUrlGenDateTime());

            if (preSignedUrlGenDate == null || preSignedUrlGenDate.isBefore(twentyFourHoursAgo)) {
                // URL has expired or is close to expiry, mark for regeneration
                String url = s3Service.generatePreSignedUrl(extractObjectKey(photo.getImage()), 24).toString();

                updatedDetails.add(GetPhotoDto.builder()
                        .imgId(photo.getImgId())
                        .image(url)
                        .uploadDateTime(photo.getUploadDateTime())
                        .build()
                );

                photoBlogRepository.updatePreSignedUrlsInDynamo(photo.getImgId(), url);

            } else {
                updatedDetails.add(GetPhotoDto.builder()
                        .imgId(photo.getImgId())
                        .image(photo.getImage())
                        .uploadDateTime(photo.getUploadDateTime())
                        .build()
                );
            }
        }

        return updatedDetails;
    }

    private LocalDateTime parseDateTime(String dateTime) {
        LocalDateTime urlGenerationTime = null;
        if (dateTime != null && !dateTime.isEmpty()) {
            try {
                urlGenerationTime = LocalDateTime.parse(dateTime);
            } catch (DateTimeParseException e) {
                System.err.println("Failed to parse datetime: " + dateTime);
                // Continue with null datetime which will trigger regeneration
            }
        }

        return urlGenerationTime;
    }

    @Override
    public void deleteImage(String id, Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Map<String, AttributeValue> deleteResponse = photoBlogRepository.deleteItem(id);
        if (!deleteResponse.isEmpty()) {
            String objectURL = deleteResponse.get("picUrl").s();
            s3Service.deleteObject(extractObjectKey(objectURL));
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
        ;
        String url = s3Service.generatePreSignedUrl(RECYCLE_BIN_PATH + email + "/" + objectKey,
                24).toString();
        photoBlogRepository.updatePreSignedUrlsInDynamo(id, url);

        return GetPhotoDto.builder()
                .imgId(returnedAttribute.get("picId").s())
                .build();
    }

    @Override
    public GetPhotoDto restoreFromRecycleBin(String id, Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Map<String, AttributeValue> returnedAttribute = photoBlogRepository.restoreFromRecycleBin(id);
        String objectKey = extractObjectKey(returnedAttribute.get("picUrl").s());
        String oldObjectKey = objectKey.substring(objectKey.lastIndexOf("/") + 1);
        s3Service.moveObject(objectKey, oldObjectKey);
        String url = s3Service.generatePreSignedUrl(objectKey,
                24).toString();
        photoBlogRepository.updatePreSignedUrlsInDynamo(id, url);
        return GetPhotoDto.builder()
                .imgId(returnedAttribute.get("picId").s())
                .build();
    }

    @Override
    public List<GetPhotoDto> retrieveAllImagesInRecyclingBin(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        List<GetPhotoDto> recycledItemsDetails = photoBlogRepository.getAllItemsInRecycleBin(email);
        return checkAndRefreshPreSignedUrl(recycledItemsDetails);
    }

    private String extractObjectKey(String s3Url) {
        try {
            URI uri = new URI(s3Url);

            // Get the path part of the URL (e.g., "/subfolder/file.txt")
            String path = uri.getPath();

            // Remove the leading slash to get the object key
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            return path;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + s3Url);
        }
    }
}
