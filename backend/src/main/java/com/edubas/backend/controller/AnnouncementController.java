package com.edubas.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.edubas.backend.dto.AnnouncementDTO;
import com.edubas.backend.service.AnnouncementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementController.class);
    private final AnnouncementService announcementService;

    @PostMapping
    public ResponseEntity<?> createAnnouncement(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String type,
            @RequestParam String userId) {
        try {
            AnnouncementDTO announcement = announcementService.createAnnouncement(title, description, type, userId);
            logger.info("User (ID: {}) created announcement: '{}' [Type: {}]", userId, title, type);
            return ResponseEntity.status(HttpStatus.CREATED).body(announcement);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnnouncement(
            @PathVariable String id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String type) {
        try {
            AnnouncementDTO announcement = announcementService.updateAnnouncement(id, title, description, type);
            logger.info("Announcement (ID: {}) updated: '{}' [Type: {}]", id, title, type);
            return ResponseEntity.ok(announcement);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable String id) {
        try {
            announcementService.deleteAnnouncement(id);
            logger.info("Announcement deleted (ID: {})", id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Announcement deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAnnouncement(@PathVariable String id) {
        try {
            AnnouncementDTO announcement = announcementService.getAnnouncementById(id);
            return ResponseEntity.ok(announcement);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllAnnouncements() {
        try {
            List<AnnouncementDTO> announcements = announcementService.getAllAnnouncements();
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getAnnouncementsByUser(@PathVariable String userId) {
        try {
            List<AnnouncementDTO> announcements = announcementService.getAnnouncementsByUser(userId);
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
