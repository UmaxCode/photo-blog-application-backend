package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.dto.response.PhotoUploadDTo;
import org.umaxcode.repository.PhotoBlogRepository;
import org.umaxcode.service.PhotoBlogService;
import org.umaxcode.service.S3Service;

@Service
@RequiredArgsConstructor
public class PhotoBlogServiceImpl implements PhotoBlogService {

    private final PhotoBlogRepository photoBlogRepository;
    private final S3Service s3Service;

    @Override
    public PhotoUploadDTo upload(MultipartFile pic) {

        String uploadedPicUrl = s3Service.upload(pic, "example@gmail.com");
        return PhotoUploadDTo.builder()
                .picUrl(uploadedPicUrl)
                .build();
    }
}
