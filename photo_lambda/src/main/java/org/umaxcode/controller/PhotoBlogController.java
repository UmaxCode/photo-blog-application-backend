package org.umaxcode.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.umaxcode.domain.dto.response.GetPhotoDto;
import org.umaxcode.domain.dto.response.PhotoUploadDTo;
import org.umaxcode.domain.dto.response.SuccessResponse;
import org.umaxcode.service.PhotoBlogService;

import java.util.List;

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

    @GetMapping("/{objectKey}/generate-pre-signed-url")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse generatePreSignedUrl(@PathVariable("objectKey") String key) {

        PhotoUploadDTo generatedUrl = photoBlogService.generatePreSignedUrl(key);

        return SuccessResponse.builder()
                .message("Pre-signed url generated successfully")
                .data(generatedUrl)
                .build();
    }

    @GetMapping("/{ownership-type}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse getAllPhotos(@PathVariable("ownership-type") String ownership) {

        List<GetPhotoDto> images = photoBlogService.getImages(ownership);
        return SuccessResponse.builder()
                .message("All photos retrieved successfully")
                .data(images)
                .build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@PathVariable String id) {

        photoBlogService.deleteImage(id);
    }

}
