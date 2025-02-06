package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.domain.dto.response.GetPhotoDto;
import org.umaxcode.service.S3Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static software.amazon.awssdk.core.sync.RequestBody.fromBytes;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    @Value("${application.aws.stageBucketName}")
    private String stageBucketName;
    @Value("${application.aws.primaryBucketName}")
    private String primaryBucketName;

    @Override
    public String upload(MultipartFile pic, String email, String firstName, String lastName) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("email", email);
        metadata.put("firstName", firstName);
        metadata.put("lastName", lastName);
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(stageBucketName)
                .key(pic.getOriginalFilename())
                .contentType(pic.getContentType())
                .metadata(metadata)
                .build();

        try {
            PutObjectResponse putResponse = s3Client.putObject(putRequest, fromBytes(pic.getBytes()));
            System.out.printf("Put response: %s", putResponse);
            return "Image uploaded for processing";
        } catch (IOException ex) {
            System.out.printf("error: %s", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    @Override
    public URL generatePreSignedUrl(String objectKey, int expirationInHours) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(primaryBucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest preSignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(expirationInHours))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(preSignRequest).url();
    }

    @Override
    public List<GetPhotoDto> getObjects(List<String> objectKeys) {
        ExecutorService executor = Executors.newFixedThreadPool(objectKeys.size());

        // Use CompletableFuture for parallel execution
        List<CompletableFuture<GetPhotoDto>> futures = objectKeys.stream()
                .map(key -> CompletableFuture.supplyAsync(() -> getObjectBytes(key), executor))
                .toList();

        // Wait for all tasks to complete and collect results
        List<GetPhotoDto> result = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        executor.shutdown();
        return result;
    }

    private GetPhotoDto getObjectBytes(String objectKey) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(primaryBucketName)
                .key(objectKey)
                .build();

        byte[] byteArray = s3Client.getObject(request, ResponseTransformer.toBytes()).asByteArray();

        return GetPhotoDto.builder()
                .objectKey(objectKey)
                .image(byteArray)
                .build();
    }

    @Override
    public void deleteObject(String objectKey) {

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(primaryBucketName)
                .key(objectKey)
                .build();

        s3Client.deleteObject(deleteRequest);
    }
}
