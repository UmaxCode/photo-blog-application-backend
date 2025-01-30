package org.umaxcode.service;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

    String upload(MultipartFile pic, String email, String firstName, String lastName);
}
