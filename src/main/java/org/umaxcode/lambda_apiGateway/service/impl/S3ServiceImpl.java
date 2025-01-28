package org.umaxcode.lambda_apiGateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.lambda_apiGateway.service.S3Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;

import static software.amazon.awssdk.core.sync.RequestBody.fromBytes;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;
    @Value("${application.aws.stageBucketName}")
    private String stageBucketName;

    @Override
    public String upload(MultipartFile pic) {

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(stageBucketName)
                .key(pic.getOriginalFilename())
                .contentType(pic.getContentType())
                .build();

        try {
            PutObjectResponse putResponse = s3Client.putObject(putRequest, fromBytes(pic.getBytes()));
            log.info("Put response: {}", putResponse);
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }

        return null;
    }
}
