package org.umaxcode.lambda_apiGateway.service;

import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.lambda_apiGateway.domain.dto.response.PhotoUploadDTo;

public interface PhotoBlogService {

    PhotoUploadDTo upload(MultipartFile pic);
}
