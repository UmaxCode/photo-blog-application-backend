package org.umaxcode.service;

import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.domain.dto.response.GetPhotoDto;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface S3Service {

    String upload(MultipartFile pic, String email, String firstName, String lastName);

    URL generatePreSignedUrl(String objectKey, int expirationInHours);

    List<GetPhotoDto> getObjects(List<Map<String, String>> objectKeys);

    void deleteObject(String objectKey);

    void moveObject(String sourceObjectKey, String destinationObjectKey);
}
