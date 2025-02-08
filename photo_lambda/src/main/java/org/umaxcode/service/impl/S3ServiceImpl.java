package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.domain.dto.response.GetPhotoDto;
import org.umaxcode.service.S3Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
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
    public List<GetPhotoDto> getObjects(List<Map<String, String>> objectDetails) {
        ExecutorService executor = Executors.newFixedThreadPool(objectDetails.size());

        // Use CompletableFuture for parallel execution
        List<CompletableFuture<GetPhotoDto>> futures = objectDetails.stream()
                .map(detail -> CompletableFuture.supplyAsync(() -> getObjectBytes(detail), executor))
                .toList();

        // Wait for all tasks to complete and collect results
        List<GetPhotoDto> result = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        executor.shutdown();
        return result;
    }

    private GetPhotoDto getObjectBytes(Map<String, String> objectDetail) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(primaryBucketName)
                .key(objectDetail.get("objectKey"))
                .build();

        byte[] byteArray = s3Client.getObject(request, ResponseTransformer.toBytes()).asByteArray();

        return GetPhotoDto.builder()
                .imgId(objectDetail.get("picId"))
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

    @Override
    public void moveObject(String sourceObjectKey, String destinationObjectKey) {
        // Copy to recycled path
        CopyObjectRequest copyReq = CopyObjectRequest.builder()
                .sourceBucket(primaryBucketName)
                .sourceKey(sourceObjectKey)
                .destinationBucket(primaryBucketName)
                .destinationKey(destinationObjectKey)
                .build();
        s3Client.copyObject(copyReq);

        // Delete original
        DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                .bucket(primaryBucketName)
                .key(sourceObjectKey)
                .build();
        s3Client.deleteObject(deleteReq);
    }
}
