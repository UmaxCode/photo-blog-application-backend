package org.umaxcode.service;

import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

public interface S3Service {

    String upload(MultipartFile pic, String email, String firstName, String lastName);

    URL generatePreSignedUrl( String objectKey, int expirationInHours);
}
