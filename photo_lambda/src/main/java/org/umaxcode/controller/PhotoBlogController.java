package org.umaxcode.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.dto.response.SuccessResponse;
import org.umaxcode.service.PhotoBlogService;

@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
public class PhotoBlogController {

    private final PhotoBlogService photoBlogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse uploadPhoto(@RequestPart MultipartFile pic) {

        String uploadPicResponse = photoBlogService.upload(pic);

        return SuccessResponse.builder()
                .message(uploadPicResponse)
                .build();
    }
}
