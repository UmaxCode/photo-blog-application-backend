package org.umaxcode.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.dto.response.PhotoUploadDTo;
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

    @GetMapping("/{id}/generate-pre-signed-url}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse generatePreSignedUrl(@PathVariable String id) {

        PhotoUploadDTo generatedUrl = photoBlogService.generatePreSignedUrl(id);

        return SuccessResponse.builder()
                .message("Pre-signed url generated successfully")
                .data(generatedUrl)
                .build();
    }

}
