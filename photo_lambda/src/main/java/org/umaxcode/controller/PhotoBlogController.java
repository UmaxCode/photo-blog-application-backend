package org.umaxcode.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public SuccessResponse uploadPhoto(@RequestPart MultipartFile pic, @AuthenticationPrincipal Jwt jwt) {

        String uploadPicResponse = photoBlogService.upload(pic, jwt);

        return SuccessResponse.builder()
                .message(uploadPicResponse)
                .build();
    }

    @GetMapping("/{id}/generate-pre-signed-url")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse generatePreSignedUrl(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {

        PhotoUploadDTo generatedUrl = photoBlogService.generatePreSignedUrl(id, jwt);

        return SuccessResponse.builder()
                .message("Pre-signed url generated successfully")
                .data(generatedUrl)
                .build();
    }

    @GetMapping("/{ownership-type}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse getAllPhotos(@PathVariable("ownership-type") String ownership, @AuthenticationPrincipal Jwt jwt) {

        List<GetPhotoDto> images = photoBlogService.getImages(ownership, jwt);
        return SuccessResponse.builder()
                .message("All photos retrieved successfully")
                .data(images)
                .build();
    }

    @PatchMapping("/{id}/recycle-bin")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse movePhotoToRecycleBin(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {

        GetPhotoDto updateResponse = photoBlogService.moveToRecycleBin(id, jwt);
        return SuccessResponse.builder()
                .message("Photo moved to recycling bin successfully")
                .data(updateResponse)
                .build();
    }

    @GetMapping("/recycle-bin")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse getAllPhotosInRecyclingBin(@AuthenticationPrincipal Jwt jwt){

        List<GetPhotoDto> recycledImages = photoBlogService.retrieveAllImagesInRecyclingBin(jwt);
        return SuccessResponse.builder()
                .message("Photos in recycling bin retrieved successfully")
                .data(recycledImages)
                .build();
    }

    @PatchMapping("/{id}/recycle-bin/restore")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse restorePhotoFromRecycleBin(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {

        GetPhotoDto updateResponse = photoBlogService.restoreFromRecycleBin(id, jwt);
        return SuccessResponse.builder()
                .message("Photo has been restored successfully")
                .data(updateResponse)
                .build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {

        photoBlogService.deleteImage(id, jwt);
    }

}
