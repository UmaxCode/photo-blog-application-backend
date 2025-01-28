package org.umaxcode.service;

import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.dto.response.PhotoUploadDTo;

public interface PhotoBlogService {

    PhotoUploadDTo upload(MultipartFile pic);
}
