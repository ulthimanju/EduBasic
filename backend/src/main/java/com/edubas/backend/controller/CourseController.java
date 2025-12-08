package com.edubas.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edubas.backend.dto.MermaidFixResponse;
import com.edubas.backend.dto.PracticeProblemRequest;
import com.edubas.backend.dto.PracticeProblemResponse;
import com.edubas.backend.entity.Course;
import com.edubas.backend.entity.Lesson;
import com.edubas.backend.entity.Level;
import com.edubas.backend.entity.Module;
import com.edubas.backend.entity.PracticeProblem;
import com.edubas.backend.entity.User;
import com.edubas.backend.repository.CourseRepository;
import com.edubas.backend.repository.LessonRepository;
import com.edubas.backend.repository.PracticeProblemRepository;
import com.edubas.backend.repository.UserRepository;
import com.edubas.backend.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    private final CourseRepository courseRepository;
    private final ObjectMapper objectMapper;
    private final GeminiService geminiService;
    private final PracticeProblemRepository practiceProblemRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    public CourseController(CourseRepository courseRepository, ObjectMapper objectMapper, GeminiService geminiService,
            PracticeProblemRepository practiceProblemRepository, UserRepository userRepository,
            LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.objectMapper = objectMapper;
        this.geminiService = geminiService;
        this.practiceProblemRepository = practiceProblemRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllCourses() {
        try {
            List<Course> courses = courseRepository.findAll();
            List<Map<String, Object>> courseList = new ArrayList<>();

            for (Course course : courses) {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("courseId", course.getCourseId());
                courseMap.put("title", course.getTitle());
                courseMap.put("description", course.getDescription());
                courseMap.put("uploadedBy", course.getUploadedBy());
                courseMap.put("uploadedByUserId", course.getUploadedByUserId());
                courseMap.put("uploadedOn", course.getUploadedOn());
                courseMap.put("ipAddress", course.getIpAddress());

                courseList.add(courseMap);
            }

            return ResponseEntity.ok(courseList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<Map<String, Object>> getCourseById(@PathVariable String courseId) {
        try {
            var course = courseRepository.findByCourseId(courseId);

            if (course.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashMap<>());
            }

            Course c = course.get();
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("courseId", c.getCourseId());
            courseMap.put("title", c.getTitle());
            courseMap.put("description", c.getDescription());
            courseMap.put("uploadedBy", c.getUploadedBy());
            courseMap.put("uploadedByUserId", c.getUploadedByUserId());
            courseMap.put("uploadedOn", c.getUploadedOn());
            courseMap.put("ipAddress", c.getIpAddress());

            return ResponseEntity.ok(courseMap);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<>());
        }
    }

    @GetMapping("/{courseId}/levels")
    public ResponseEntity<List<Map<String, Object>>> getLevelsByCourseId(@PathVariable String courseId) {
        try {
            var course = courseRepository.findByCourseId(courseId);

            if (course.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
            }

            List<Map<String, Object>> levelList = new ArrayList<>();
            List<Level> levels = course.get().getLevels();

            if (levels != null) {
                for (Level level : levels) {
                    Map<String, Object> levelMap = new HashMap<>();
                    levelMap.put("levelId", level.getLevelId());
                    levelMap.put("levelName", level.getLevelName());
                    levelMap.put("summary", level.getSummary());
                    levelList.add(levelMap);
                }
            }

            return ResponseEntity.ok(levelList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    @GetMapping("/{courseId}/levels/{levelId}/modules")
    public ResponseEntity<List<Map<String, Object>>> getModulesByLevelId(@PathVariable String courseId,
            @PathVariable String levelId) {
        try {
            var course = courseRepository.findByCourseId(courseId);

            if (course.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
            }

            List<Map<String, Object>> moduleList = new ArrayList<>();
            List<Level> levels = course.get().getLevels();

            if (levels != null) {
                for (Level level : levels) {
                    if (levelId.equals(level.getLevelId())) {
                        List<Module> modules = level.getModules();
                        if (modules != null) {
                            for (Module module : modules) {
                                Map<String, Object> moduleMap = new HashMap<>();
                                moduleMap.put("moduleId", module.getModuleId());
                                moduleMap.put("moduleTitle", module.getModuleTitle());
                                moduleList.add(moduleMap);
                            }
                        }
                        break;
                    }
                }
            }

            return ResponseEntity.ok(moduleList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    @GetMapping("/{courseId}/levels/{levelId}/modules/{moduleId}/lessons")
    public ResponseEntity<List<Map<String, Object>>> getLessonsByModuleId(@PathVariable String courseId,
            @PathVariable String levelId, @PathVariable String moduleId) {
        try {
            var course = courseRepository.findByCourseId(courseId);

            if (course.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
            }

            List<Map<String, Object>> lessonList = new ArrayList<>();
            List<Level> levels = course.get().getLevels();

            if (levels != null) {
                for (Level level : levels) {
                    if (levelId.equals(level.getLevelId())) {
                        List<Module> modules = level.getModules();
                        if (modules != null) {
                            for (Module module : modules) {
                                if (moduleId.equals(module.getModuleId())) {
                                    List<Lesson> lessons = module.getLessons();
                                    if (lessons != null) {
                                        for (Lesson lesson : lessons) {
                                            Map<String, Object> lessonMap = new HashMap<>();
                                            lessonMap.put("lessonId", lesson.getLessonId());
                                            lessonMap.put("title", lesson.getTitle());
                                            lessonList.add(lessonMap);
                                        }
                                    }
                                    return ResponseEntity.ok(lessonList);
                                }
                            }
                        }
                    }
                }
            }

            return ResponseEntity.ok(lessonList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    @GetMapping("/{courseId}/levels/{levelId}/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<Map<String, Object>> getLessonContent(@PathVariable String courseId,
            @PathVariable String levelId, @PathVariable String moduleId, @PathVariable String lessonId) {
        try {
            var course = courseRepository.findByCourseId(courseId);

            if (course.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashMap<>());
            }

            List<Level> levels = course.get().getLevels();

            if (levels != null) {
                for (Level level : levels) {
                    if (levelId.equals(level.getLevelId())) {
                        List<Module> modules = level.getModules();
                        if (modules != null) {
                            for (Module module : modules) {
                                if (moduleId.equals(module.getModuleId())) {
                                    List<Lesson> lessons = module.getLessons();
                                    if (lessons != null) {
                                        for (Lesson lesson : lessons) {
                                            if (lessonId.equals(lesson.getLessonId())) {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> lessonMap = (Map<String, Object>) objectMapper
                                                        .convertValue(lesson, Map.class);
                                                return ResponseEntity.ok(lessonMap);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashMap<>());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<>());
        }
    }

    @PostMapping("/{courseId}/levels/{levelId}/modules/{moduleId}/lessons/{lessonId}/fix-visualization")
    public ResponseEntity<MermaidFixResponse> fixVisualization(
            @PathVariable String courseId,
            @PathVariable String levelId,
            @PathVariable String moduleId,
            @PathVariable String lessonId) {
        try {
            var courseOpt = courseRepository.findByCourseId(courseId);

            if (courseOpt.isEmpty()) {
                return ResponseEntity.ok(new MermaidFixResponse(false, null, "Course not found"));
            }

            Course course = courseOpt.get();
            Lesson targetLesson = null;

            // Find the lesson
            if (course.getLevels() != null) {
                for (Level level : course.getLevels()) {
                    if (levelId.equals(level.getLevelId())) {
                        if (level.getModules() != null) {
                            for (Module module : level.getModules()) {
                                if (moduleId.equals(module.getModuleId())) {
                                    if (module.getLessons() != null) {
                                        for (Lesson lesson : module.getLessons()) {
                                            if (lessonId.equals(lesson.getLessonId())) {
                                                targetLesson = lesson;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (targetLesson == null) {
                return ResponseEntity.ok(new MermaidFixResponse(false, null, "Lesson not found"));
            }

            // Get lesson context for better fixes
            String lessonTitle = targetLesson.getTitle() != null ? targetLesson.getTitle() : "Lesson";

            // Log with meaningful information
            logger.info("FIX MERMAID CODE REQUEST - Course: '{}', Lesson: '{}' (ID: {})",
                    course.getTitle() != null ? course.getTitle() : courseId,
                    lessonTitle,
                    lessonId);

            // Get visualization JSON
            String visualizationJson = targetLesson.getVisualizationJson();
            if (visualizationJson == null || visualizationJson.trim().isEmpty()) {
                return ResponseEntity.ok(new MermaidFixResponse(false, null, "No visualization found"));
            }

            // Parse visualization JSON to extract mermaid code
            @SuppressWarnings("unchecked")
            Map<String, Object> visualization = objectMapper.readValue(visualizationJson, Map.class);
            String mermaidCode = (String) visualization.get("mermaid");

            if (mermaidCode == null || mermaidCode.trim().isEmpty()) {
                return ResponseEntity.ok(new MermaidFixResponse(false, null, "No mermaid code found"));
            }

            String lessonContext = targetLesson.getTheoryMarkdown() != null
                    ? targetLesson.getTheoryMarkdown().substring(0,
                            Math.min(targetLesson.getTheoryMarkdown().length(), 300))
                    : "";

            // Call Gemini to fix the code
            String fixedCode = geminiService.fixMermaidCode(mermaidCode, lessonTitle, lessonContext);

            // Update the visualization JSON with fixed code
            visualization.put("mermaid", fixedCode);
            String updatedVisualizationJson = objectMapper.writeValueAsString(visualization);
            targetLesson.setVisualizationJson(updatedVisualizationJson);

            // Save the updated course
            courseRepository.save(course);

            return ResponseEntity.ok(new MermaidFixResponse(true, fixedCode, "Visualization fixed successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in fixVisualization: " + e.getMessage());
            e.printStackTrace(System.err);
            return ResponseEntity
                    .ok(new MermaidFixResponse(false, null, "Error fixing visualization: " + e.getMessage()));
        }
    }

    @PostMapping("/{courseId}/levels/{levelId}/modules/{moduleId}/lessons/{lessonId}/practice-problem")
    public ResponseEntity<PracticeProblemResponse> generatePracticeProblem(
            @PathVariable String courseId,
            @PathVariable String levelId,
            @PathVariable String moduleId,
            @PathVariable String lessonId,
            @RequestBody PracticeProblemRequest request) {
        try {
            if (request == null || request.getCourseTitle() == null || request.getLevelType() == null
                    || request.getModuleName() == null || request.getLessonTitle() == null
                    || request.getUsername() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new PracticeProblemResponse(false,
                                "courseTitle, levelType, moduleName, lessonTitle, and username are required",
                                null, null, new ArrayList<>(), null, null, new ArrayList<>(), null));
            }

            // Resolve user early for reuse and filtering
            User user = userRepository.findByUsername(request.getUsername()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new PracticeProblemResponse(false,
                                "User not found for username: " + request.getUsername(),
                                null, null, new ArrayList<>(), null, null, new ArrayList<>(), null));
            }

            // Check if a practice problem already exists for this lesson
            Optional<PracticeProblem> existingProblem = practiceProblemRepository
                    .findLatestByLessonIdAndUser(lessonId, request.getUsername());

            if (existingProblem.isPresent()) {
                // Return existing problem from database
                PracticeProblem problem = existingProblem.get();
                List<String> hints = objectMapper.readValue(problem.getHintsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                List<com.edubas.backend.dto.TestCase> testCases = objectMapper.readValue(problem.getTestCasesJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class,
                                com.edubas.backend.dto.TestCase.class));

                PracticeProblemResponse response = new PracticeProblemResponse(
                        true,
                        "Practice problem retrieved from database",
                        problem.getTitle(),
                        problem.getStatement(),
                        hints,
                        problem.getInputFormat(),
                        problem.getOutputFormat(),
                        testCases,
                        problem.getConstraints());

                logger.info("Retrieved existing practice problem for lesson: {}", lessonId);
                return ResponseEntity.ok(response);
            }

            // Generate new problem if none exists
            PracticeProblemResponse response = geminiService.generatePracticeProblem(
                    request.getCourseTitle(),
                    request.getLevelType(),
                    request.getModuleName(),
                    request.getLessonTitle());

            if (response.isSuccess()) {
                // Find the lesson using repository (supports lessonId or id)
                Lesson lesson = lessonRepository.findByLessonIdOrId(lessonId).orElse(null);

                if (lesson != null) {
                    // Convert hints and testCases to JSON strings
                    String hintsJson = objectMapper.writeValueAsString(response.getHints());
                    String testCasesJson = objectMapper.writeValueAsString(response.getTestCases());

                    PracticeProblem problem = new PracticeProblem(
                            response.getTitle(),
                            response.getStatement(),
                            response.getInputFormat(),
                            response.getOutputFormat(),
                            response.getConstraints(),
                            hintsJson,
                            testCasesJson,
                            user,
                            lesson);

                    practiceProblemRepository.save(problem);
                    logger.info("New practice problem generated and saved for lesson: {} by user: {}", lessonId,
                            request.getUsername());
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PracticeProblemResponse(false,
                            "Failed to generate practice problem: " + e.getMessage(),
                            null, null, new ArrayList<>(), null, null, new ArrayList<>(), null));
        }
    }
}
