package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.dto.response.PhotoUploadDTo;
import org.umaxcode.repository.PhotoBlogRepository;
import org.umaxcode.service.PhotoBlogService;
import org.umaxcode.service.S3Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.net.URL;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PhotoBlogServiceImpl implements PhotoBlogService {

    private final PhotoBlogRepository photoBlogRepository;
    private final S3Service s3Service;

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

        String objectKey = item.get("picUrl").s();
        URL url = s3Service.generatePreSignedUrl(extractObjectKey(objectKey), 60);

        return PhotoUploadDTo.builder()
                .picUrl(url.getPath())
                .build();
    }

    private String extractObjectKey(String s3Url) {
        return s3Url.substring(s3Url.lastIndexOf("/") + 1);
    }
}
