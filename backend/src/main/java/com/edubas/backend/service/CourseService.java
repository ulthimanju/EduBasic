package com.edubas.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edubas.backend.dto.CourseDatasetDTO;
import com.edubas.backend.dto.LevelDTO;
import com.edubas.backend.dto.ModuleDTO;
import com.edubas.backend.dto.TopicDTO;
import com.edubas.backend.entity.Course;
import com.edubas.backend.entity.Lesson;
import com.edubas.backend.entity.Level;
import com.edubas.backend.entity.Module;
import com.edubas.backend.repository.CourseRepository;
import com.edubas.backend.repository.LessonRepository;
import com.edubas.backend.repository.LevelRepository;
import com.edubas.backend.repository.ModuleRepository;

@Service
public class CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final LevelRepository levelRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    public CourseService(CourseRepository courseRepository, LevelRepository levelRepository,
            ModuleRepository moduleRepository, LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.levelRepository = levelRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
    }

    @Transactional
    public Course uploadCourseDataset(CourseDatasetDTO datasetDTO, String username, String userId, String ipAddress) {
        logger.info("Insertion started...");

        try {
            // Check if course already exists
            if (courseRepository.existsByCourseId(datasetDTO.getCourse_id())) {
                throw new IllegalArgumentException("Course with ID " + datasetDTO.getCourse_id() + " already exists");
            }

            // Convert DTOs to entities
            List<Level> levels = new ArrayList<>();
            if (datasetDTO.getLevels() != null) {
                for (LevelDTO levelDTO : datasetDTO.getLevels()) {
                    Level level = convertLevelDTOToEntity(levelDTO);
                    levels.add(level);
                }
            }

            // Create and save course
            Course course = new Course(
                    datasetDTO.getCourse_id(),
                    datasetDTO.getCourse_id(), // Use course_id as title for now
                    "Course: " + datasetDTO.getCourse_id(), // Generate description
                    levels,
                    username,
                    userId,
                    LocalDateTime.now(),
                    ipAddress);

            Course savedCourse = courseRepository.save(course);
            logger.info("Insertion completed successfully");

            return savedCourse;
        } catch (Exception e) {
            logger.error("Insertion failed: {}", e.getMessage());
            throw e;
        }
    }

    private Level convertLevelDTOToEntity(LevelDTO levelDTO) {
        List<Module> modules = new ArrayList<>();
        if (levelDTO.getModules() != null) {
            for (ModuleDTO moduleDTO : levelDTO.getModules()) {
                Module module = convertModuleDTOToEntity(moduleDTO);
                modules.add(module);
            }
        }

        return new Level(
                levelDTO.getLevel(),
                levelDTO.getLevel_title(),
                "", // No summary in new structure
                modules);
    }

    private Module convertModuleDTOToEntity(ModuleDTO moduleDTO) {
        List<Lesson> lessons = new ArrayList<>();
        if (moduleDTO.getTopics() != null) {
            for (TopicDTO topicDTO : moduleDTO.getTopics()) {
                Lesson lesson = convertTopicDTOToLesson(topicDTO);
                lessons.add(lesson);
            }
        }

        return new Module(
                moduleDTO.getModule_id(),
                moduleDTO.getModule_title(),
                moduleDTO.getModule_description(),
                0, // No estimated time in new structure
                lessons);
    }

    private Lesson convertTopicDTOToLesson(TopicDTO topicDTO) {
        String examplesJson = null;

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            if (topicDTO.getExamples() != null && !topicDTO.getExamples().isEmpty()) {
                // Serialize all examples with their visualizations intact
                examplesJson = mapper.writeValueAsString(topicDTO.getExamples());
            }
        } catch (Exception e) {
            logger.warn("Failed to serialize topic data to JSON: {}", e.getMessage());
        }

        return new Lesson(
                topicDTO.getTopic_id(),
                topicDTO.getTopic_title(),
                new ArrayList<>(), // No objectives in new structure
                topicDTO.getTopic_description(), // Use description as theory
                examplesJson,
                null, // Visualizations are now embedded in examples
                null); // No quiz in new structure
    }

    public Course getCourseByCourseid(String courseId) {
        return courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));
    }
}
