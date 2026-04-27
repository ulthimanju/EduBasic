package com.nexora.courseservice.controller;

import com.nexora.courseservice.dto.response.CatalogCourseResponse;
import com.nexora.courseservice.dto.response.CourseOutlineResponse;
import com.nexora.courseservice.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<Page<CatalogCourseResponse>> browseCatalog(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(catalogService.browseCatalog(keyword, pageable));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseOutlineResponse> getCoursePreview(@PathVariable UUID courseId) {
        return ResponseEntity.ok(catalogService.getCoursePreview(courseId));
    }
}
