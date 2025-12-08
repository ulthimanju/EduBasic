package com.edubas.backend.controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.edubas.backend.dto.CourseDatasetDTO;
import com.edubas.backend.entity.Course;
import com.edubas.backend.service.CourseService;
import com.edubas.backend.util.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final CourseService courseService;
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public UploadController(CourseService courseService, ObjectMapper objectMapper,
            JwtTokenProvider jwtTokenProvider) {
        this.courseService = courseService;
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadJsonFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Extract user info from JWT
            String token = authHeader.substring(7);
            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);
            String username = claims.get("username", String.class);
            String userId = claims.get("userId", String.class);

            // Get IP address
            String ipAddress = getClientIpAddress(request);

            // Validate file
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "File is empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Validate file type
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            if ((filename == null || !filename.endsWith(".json")) &&
                    !(contentType != null && contentType.contains("application/json"))) {
                response.put("status", "error");
                response.put("message", "File must be JSON format");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Parse JSON file - expect array of courses
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            CourseDatasetDTO[] coursesArray = objectMapper.readValue(jsonContent, CourseDatasetDTO[].class);

            if (coursesArray == null || coursesArray.length == 0) {
                response.put("status", "error");
                response.put("message", "No courses found in the uploaded file");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            List<Map<String, Object>> uploadedCourses = new ArrayList<>();
            int successCount = 0;

            for (CourseDatasetDTO courseDataset : coursesArray) {
                // Validate dataset
                if (courseDataset.getCourse_id() == null || courseDataset.getCourse_id().isEmpty()) {
                    response.put("status", "error");
                    response.put("message", "Course ID (course_id) is required for all courses");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                // Save to database
                Course savedCourse = courseService.uploadCourseDataset(courseDataset, username, userId, ipAddress);

                Map<String, Object> courseInfo = new HashMap<>();
                courseInfo.put("courseId", savedCourse.getCourseId());
                courseInfo.put("courseTitle", savedCourse.getTitle());
                uploadedCourses.add(courseInfo);
                successCount++;
            }

            response.put("status", "success");
            response.put("message", "Course dataset(s) uploaded and saved successfully");
            response.put("uploadedCourses", uploadedCourses);
            response.put("recordsProcessed", successCount);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to process file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
