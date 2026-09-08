package com.dasifind.backend.domain.searchcard.image.controller;

import com.dasifind.backend.domain.searchcard.image.dto.response.SearchCardImageUploadResDTO;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.service.SearchCardImageService;
import com.dasifind.backend.global.api.ApiResDTO;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/search-card-images")
public class SearchCardImageController {

    private final SearchCardImageService searchCardImageService;

    public SearchCardImageController(SearchCardImageService searchCardImageService) {
        this.searchCardImageService = searchCardImageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResDTO<SearchCardImageUploadResDTO> upload(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file,
            @RequestParam("imageType") SearchCardImageType imageType
    ) {
        SearchCardImageUploadResDTO response = searchCardImageService.upload(
                Long.valueOf(jwt.getSubject()),
                file,
                imageType
        );
        return ApiResDTO.success(response);
    }

    @DeleteMapping("/{imageId}")
    public ApiResDTO<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long imageId
    ) {
        searchCardImageService.delete(Long.valueOf(jwt.getSubject()), imageId);
        return ApiResDTO.success();
    }
}
