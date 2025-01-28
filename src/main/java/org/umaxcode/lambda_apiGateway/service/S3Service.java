package org.umaxcode.lambda_apiGateway.service;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

    String upload(MultipartFile file);
}
