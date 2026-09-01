package com.dasifind.backend.domain.searchcard.image.controller;

import com.dasifind.backend.domain.searchcard.image.dto.response.SearchCardImageUploadResponse;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.service.SearchCardImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchCardImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchCardImageService searchCardImageService;

    @Test
    void 수색카드_이미지를_업로드한다() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "wallet.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );
        when(searchCardImageService.upload(any(), any(), any())).thenReturn(
                new SearchCardImageUploadResponse(
                        501L,
                        "https://presigned.example/image",
                        SearchCardImageType.REFERENCE
                )
        );

        mockMvc.perform(multipart("/api/v1/search-card-images")
                        .file(file)
                        .param("imageType", "REFERENCE")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2001"))
                .andExpect(jsonPath("$.result.imageId").value(501))
                .andExpect(jsonPath("$.result.imageUrl").value("https://presigned.example/image"))
                .andExpect(jsonPath("$.result.imageType").value("REFERENCE"));

        verify(searchCardImageService).upload(7L, file, SearchCardImageType.REFERENCE);
    }

    @Test
    void 파일이_누락되면_필수값_누락으로_응답한다() throws Exception {
        mockMvc.perform(multipart("/api/v1/search-card-images")
                        .param("imageType", "ACTUAL")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4004"));
    }

    @Test
    void 이미지_유형이_누락되면_필수값_누락으로_응답한다() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "wallet.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/search-card-images")
                        .file(file)
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4004"));
    }

    @Test
    void 지원하지_않는_이미지_유형값은_잘못된_요청으로_응답한다() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "wallet.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/search-card-images")
                        .file(file)
                        .param("imageType", "UNKNOWN")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void 이미지_업로드는_인증이_필요하다() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "wallet.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/search-card-images")
                        .file(file)
                        .param("imageType", "ACTUAL"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }
}
