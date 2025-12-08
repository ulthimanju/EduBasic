package com.edubas.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.edubas.backend.dto.AnnouncementDTO;
import com.edubas.backend.entity.Announcement;
import com.edubas.backend.entity.User;
import com.edubas.backend.repository.AnnouncementRepository;
import com.edubas.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public AnnouncementDTO createAnnouncement(String title, String description, String type, String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();
        Announcement announcement = new Announcement(title, description, type, user);

        // Regular save - Spring Data Neo4j should persist the relationship
        Announcement saved = announcementRepository.save(announcement);

        // Manually set the user since the saved object might not have it hydrated
        saved.setCreatedBy(user);

        return convertToDTO(saved);
    }

    public AnnouncementDTO updateAnnouncement(String id, String title, String description, String type) {
        Optional<Announcement> announcementOpt = announcementRepository.findById(id);
        if (!announcementOpt.isPresent()) {
            throw new RuntimeException("Announcement not found");
        }

        Announcement announcement = announcementOpt.get();
        announcement.setTitle(title);
        announcement.setDescription(description);
        announcement.setType(type);
        announcement.setUpdatedAt(System.currentTimeMillis());

        Announcement saved = announcementRepository.save(announcement);
        return convertToDTO(saved);
    }

    public void deleteAnnouncement(String id) {
        announcementRepository.deleteById(id);
    }

    public AnnouncementDTO getAnnouncementById(String id) {
        Optional<Announcement> announcementOpt = announcementRepository.findById(id);
        if (!announcementOpt.isPresent()) {
            throw new RuntimeException("Announcement not found");
        }
        return convertToDTO(announcementOpt.get());
    }

    public List<AnnouncementDTO> getAllAnnouncements() {
        return announcementRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AnnouncementDTO> getAnnouncementsByUser(String userId) {
        return announcementRepository.findByCreatedByUserId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AnnouncementDTO convertToDTO(Announcement announcement) {
        AnnouncementDTO dto = new AnnouncementDTO();
        dto.setId(announcement.getId());
        dto.setTitle(announcement.getTitle());
        dto.setDescription(announcement.getDescription());
        dto.setType(announcement.getType());
        dto.setCreatedAt(announcement.getCreatedAt());
        dto.setUpdatedAt(announcement.getUpdatedAt());

        if (announcement.getCreatedBy() != null) {
            dto.setCreatedByUserId(announcement.getCreatedBy().getId());
            dto.setCreatedByUsername(announcement.getCreatedBy().getUsername());
        }
        return dto;
    }
}
