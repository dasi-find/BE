package com.dasifind.backend.domain.home.controller;

import com.dasifind.backend.domain.home.dto.response.HomeResDTO;
import com.dasifind.backend.domain.home.service.HomeQueryService;
import com.dasifind.backend.global.api.ApiResDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    private final HomeQueryService service;

    public HomeController(HomeQueryService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/home")
    public ApiResDTO<HomeResDTO> home(@AuthenticationPrincipal Jwt jwt) {
        return ApiResDTO.success(service.getHome(Long.valueOf(jwt.getSubject())));
    }
}
