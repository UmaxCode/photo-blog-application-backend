package org.umaxcode.service;

import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.domain.dto.response.GetPhotoDto;
import org.umaxcode.domain.dto.response.PhotoUploadDTo;

import java.util.List;

public interface PhotoBlogService {

    String upload(MultipartFile pic);

    PhotoUploadDTo generatePreSignedUrl(String id);

    List<GetPhotoDto> getImages(String ownership);

    void deleteImage(String id);
}
