package org.umaxcode.lambda_apiGateway.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.lambda_apiGateway.domain.dto.response.PhotoUploadDTo;
import org.umaxcode.lambda_apiGateway.repository.PhotoBlogRepository;
import org.umaxcode.lambda_apiGateway.service.PhotoBlogService;
import org.umaxcode.lambda_apiGateway.service.S3Service;

@Service
@RequiredArgsConstructor
public class PhotoBlogServiceImpl implements PhotoBlogService {

    private final PhotoBlogRepository photoBlogRepository;
    private final S3Service s3Service;

    @Override
    public PhotoUploadDTo upload(MultipartFile pic) {

        String uploadedPicUrl = s3Service.upload(pic);
        photoBlogRepository.createItem(uploadedPicUrl, "example@gmail.com");
        return PhotoUploadDTo.builder()
                .picUrl(uploadedPicUrl)
                .build();
    }
}
