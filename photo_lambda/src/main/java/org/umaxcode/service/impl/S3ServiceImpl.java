package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.service.S3Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static software.amazon.awssdk.core.sync.RequestBody.fromBytes;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;
    @Value("${application.aws.stageBucketName}")
    private String stageBucketName;

    @Override
    public String upload(MultipartFile pic, String uploadBy) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("uploadBy", uploadBy);
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(stageBucketName)
                .key(pic.getOriginalFilename())
                .contentType(pic.getContentType())
                .metadata(metadata)
                .build();

        try {
            PutObjectResponse putResponse = s3Client.putObject(putRequest, fromBytes(pic.getBytes()));
            System.out.printf("Put response: %s", putResponse);
            return putResponse.requestChargedAsString();
        } catch (IOException ex) {
            System.out.printf("error: %s", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
