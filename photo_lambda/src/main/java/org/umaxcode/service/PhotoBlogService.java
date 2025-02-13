package org.umaxcode.service;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.domain.dto.response.GetPhotoDto;
import org.umaxcode.domain.dto.response.PhotoUploadDTo;

import java.util.List;

public interface PhotoBlogService {

    String upload(MultipartFile pic, Jwt jwt);

    PhotoUploadDTo generatePreSignedUrl(String id);

    List<GetPhotoDto> getImages(String ownership, Jwt jwt);

    void deleteImage(String id, Jwt jwt);

    GetPhotoDto moveToRecycleBin(String id, Jwt jwt);

    GetPhotoDto restoreFromRecycleBin(String id, Jwt jwt);

    List<GetPhotoDto> retrieveAllImagesInRecyclingBin(Jwt jwt);
}
