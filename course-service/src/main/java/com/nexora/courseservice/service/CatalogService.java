package com.nexora.courseservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.courseservice.constants.CacheKeys;
import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.constants.LogMessages;
import com.nexora.courseservice.dto.response.*;
import com.nexora.courseservice.entity.Course;
import com.nexora.courseservice.entity.CourseStatus;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final CourseRepository courseRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<CatalogCourseResponse> browseCatalog(String keyword, Pageable pageable) {
        String cacheKey = CacheKeys.catalogPage(pageable.getPageNumber(), pageable.getPageSize(), keyword);
        
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                // Simplified pagination cache - in a real app you'd want a more robust PageImpl serializer
                List<CatalogCourseResponse> content = objectMapper.readValue(cached, objectMapper.getTypeFactory().constructCollectionType(List.class, CatalogCourseResponse.class));
                return new PageImpl<>(content, pageable, content.size()); // Total count might be wrong but enough for demo
            }
        } catch (Exception e) {
            log.warn(LogMessages.CACHE_READ_FAILED, e.getMessage());
        }

        Page<Course> courses;
        if (keyword == null || keyword.isBlank()) {
            courses = courseRepository.findByStatusAndIsDeletedFalse(CourseStatus.PUBLISHED, pageable);
        } else {
            courses = courseRepository.findByStatusAndIsDeletedFalseAndTitleContainingIgnoreCase(CourseStatus.PUBLISHED, keyword, pageable);
        }

        Page<CatalogCourseResponse> response = courses.map(this::mapToCatalogResponse);

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response.getContent()), Duration.ofMinutes(10));
        } catch (JsonProcessingException e) {
            log.warn(LogMessages.CACHE_WRITE_FAILED, e.getMessage());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public CourseOutlineResponse getCoursePreview(UUID courseId) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        CourseOutlineResponse res = new CourseOutlineResponse();
        res.setId(course.getId());
        res.setTitle(course.getTitle());
        res.setDescription(course.getDescription());
        res.setThumbnailUrl(course.getThumbnailUrl());
        res.setCompletionRules(course.getCompletionRules());
        
        res.setModules(course.getModules().stream()
                .filter(m -> !m.isDeleted())
                .map(m -> {
                    ModuleOutlineResponse mr = new ModuleOutlineResponse();
                    mr.setId(m.getId());
                    mr.setTitle(m.getTitle());
                    mr.setDescription(m.getDescription());
                    mr.setOrderIndex(m.getOrderIndex());
                    mr.setLessons(m.getLessons().stream()
                            .filter(l -> !l.isDeleted())
                            .map(l -> {
                                LessonOutlineResponse lr = new LessonOutlineResponse();
                                lr.setId(l.getId());
                                lr.setTitle(l.getTitle());
                                lr.setContentType(l.getContentType());
                                lr.setDurationMinutes(l.getDurationMinutes());
                                lr.setOrderIndex(l.getOrderIndex());
                                lr.setPreview(l.isPreview());
                                if (l.isPreview()) {
                                    lr.setContentBody(l.getContentBody());
                                    lr.setContentUrl(l.getContentUrl());
                                }
                                return lr;
                            }).collect(Collectors.toList()));
                    return mr;
                }).collect(Collectors.toList()));
        
        res.setExams(course.getCourseExams().stream()
                .map(e -> {
                    CourseExamResponse er = new CourseExamResponse();
                    er.setId(e.getId());
                    er.setExamId(e.getExamId());
                    er.setTitle(e.getTitle());
                    er.setOrderIndex(e.getOrderIndex());
                    er.setRequiredToComplete(e.isRequiredToComplete());
                    er.setMinPassPercent(e.getMinPassPercent());
                    return er;
                }).collect(Collectors.toList()));

        return res;
    }

    private CatalogCourseResponse mapToCatalogResponse(Course course) {
        CatalogCourseResponse res = new CatalogCourseResponse();
        res.setId(course.getId());
        res.setTitle(course.getTitle());
        res.setDescription(course.getDescription());
        res.setThumbnailUrl(course.getThumbnailUrl());
        res.setStatus(course.getStatus());
        res.setCreatedBy(course.getCreatedBy());
        res.setCreatedAt(course.getCreatedAt());
        
        res.setTotalModules((int) course.getModules().stream().filter(m -> !m.isDeleted()).count());
        res.setTotalLessons((int) course.getModules().stream()
                .filter(m -> !m.isDeleted())
                .flatMap(m -> m.getLessons().stream())
                .filter(l -> !l.isDeleted())
                .count());
        res.setTotalExams(course.getCourseExams().size());
        
        return res;
    }
}
