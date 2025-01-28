package org.umaxcode.lambda_apiGateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.lambda_apiGateway.domain.dto.response.PhotoUploadDTo;
import org.umaxcode.lambda_apiGateway.domain.dto.response.SuccessResponse;
import org.umaxcode.lambda_apiGateway.service.PhotoBlogService;

@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
public class PhotoBlogController {

    private final PhotoBlogService photoBlogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse uploadPhoto(@RequestPart MultipartFile pic) {

        PhotoUploadDTo uploadPicResponse = photoBlogService.upload(pic);

        return SuccessResponse.builder()
                .message("Photo uploaded successfully")
                .data(uploadPicResponse)
                .build();
    }
}
