# Course Dataset Upload Feature

## Overview
This document describes the backend REST API endpoint for uploading JSON course datasets to the Neo4j database.

## API Endpoint

### POST /api/upload
Receives a JSON file containing course curriculum data and saves it to the Neo4j database.

**URL:** `http://localhost:8080/api/upload`

**Method:** `POST`

**Content-Type:** `multipart/form-data`

### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| file | File | Yes | JSON file containing course dataset |

### Request Body (JSON File Format)

The JSON file must follow the CourseDatasetDTO structure:

```json
{
  "courseId": "java-essentials-2025",
  "title": "Comprehensive Java Learning Path",
  "description": "A five-level production-grade Java course...",
  "levels": [
    {
      "levelId": "level1",
      "levelName": "BEGINNER",
      "summary": "Set up Java and learn primitives...",
      "modules": [
        {
          "moduleId": "level1-module1",
          "moduleTitle": "Java Setup & Entry",
          "description": "Understand how Java programs start...",
          "estimatedTimeMinutes": 30,
          "lessons": [
            {
              "lessonId": "level1-module1-lesson1",
              "title": "Program Startup Flow",
              "objectives": [
                "Understand entry point in Java programs",
                "Trace program execution..."
              ],
              "theory": {
                "markdown": "Java programs start when the JVM loads..."
              }
            }
          ]
        }
      ]
    }
  ]
}
```

### Response Format

#### Success Response (200 OK)
```json
{
  "status": "success",
  "message": "Course dataset uploaded and saved successfully",
  "courseId": "java-essentials-2025",
  "courseTitle": "Comprehensive Java Learning Path",
  "recordsProcessed": 1
}
```

#### Error Response (400/500)
```json
{
  "status": "error",
  "message": "Course with ID java-essentials-2025 already exists"
}
```

### Possible Responses

| Status Code | Description |
|-------------|-------------|
| 200 | Dataset uploaded and saved successfully |
| 400 | Bad request (empty file, invalid JSON, missing courseId, course already exists) |
| 500 | Internal server error |

## Validation Rules

1. **File Validation:**
   - File must not be empty
   - File must be JSON format (.json extension or application/json content type)

2. **Data Validation:**
   - `courseId` is required and must not be empty
   - `courseId` must be unique (no duplicate course IDs in database)

3. **JSON Structure:**
   - Must match CourseDatasetDTO schema
   - Nested levels, modules, and lessons are optional but recommended

## Database Schema

The course data is stored in Neo4j with the following nodes and relationships:

### Nodes
- **Course**: courseId, title, description
- **Level**: levelId, levelName, summary
- **Module**: moduleId, moduleTitle, description, estimatedTimeMinutes
- **Lesson**: lessonId, title, objectives[], theoryMarkdown

### Relationships
- Course -[HAS_LEVEL]-> Level
- Level -[HAS_MODULE]-> Module
- Module -[HAS_LESSON]-> Lesson

## Implementation Details

### Backend Classes

1. **Entity Classes** (`entity/`)
   - `Course.java`: Represents a course node
   - `Level.java`: Represents a level node
   - `Module.java`: Represents a module node
   - `Lesson.java`: Represents a lesson node

2. **DTO Classes** (`dto/`)
   - `CourseDatasetDTO.java`: Request body mapping
   - `LevelDTO.java`: Nested level data
   - `ModuleDTO.java`: Nested module data
   - `LessonDTO.java`: Nested lesson data

3. **Repository Classes** (`repository/`)
   - `CourseRepository.java`: Neo4j repository for Course
   - `LevelRepository.java`: Neo4j repository for Level
   - `ModuleRepository.java`: Neo4j repository for Module
   - `LessonRepository.java`: Neo4j repository for Lesson

4. **Service Class** (`service/`)
   - `CourseService.java`: Business logic for dataset upload and validation

5. **Controller Class** (`controller/`)
   - `UploadController.java`: REST endpoint handler

## Frontend Integration

The frontend `UploadContentPage.jsx` component:
1. Validates JSON files before upload (client-side)
2. Sends multipart file to `/api/upload` endpoint
3. Displays upload success/error messages
4. Shows records processed count

## Usage Example

### cURL Request
```bash
curl -X POST http://localhost:8080/api/upload \
  -F "file=@JAVA_DATASET.json"
```

### JavaScript/React Request
```javascript
const formData = new FormData();
formData.append('file', selectedFile);

const response = await fetch('http://localhost:8080/api/upload', {
  method: 'POST',
  body: formData
});

const data = await response.json();
console.log(data);
```

## Error Handling

The API provides detailed error messages:

| Error Scenario | Message | HTTP Status |
|---|---|---|
| Empty file | "File is empty" | 400 |
| Invalid file type | "File must be JSON format" | 400 |
| Missing courseId | "Course ID is required" | 400 |
| Duplicate courseId | "Course with ID [id] already exists" | 400 |
| Invalid JSON | "Failed to process file: ..." | 500 |
| Database error | "Failed to process file: ..." | 500 |

## CORS Configuration

The endpoint is configured to accept requests from:
- Origin: `http://localhost:5173` (Vite development server)
- Methods: `GET, POST, PUT, DELETE, OPTIONS`
- Headers: All headers allowed
- Credentials: Allowed

## Database Connection

Neo4j Aura Cloud:
- **URI:** `neo4j+s://48dea953.databases.neo4j.io`
- **Database:** `neo4j`
- **Authentication:** Username and password configured in `application.properties`

## Dependencies

- Spring Boot 3.4.0
- Spring Data Neo4j
- Jackson (JSON processing)
- Lombok (code generation)
- Java 21

## Notes

- IDs are automatically generated as UUID strings for all nodes
- All nested data is persisted in a single transaction
- If any part of the upload fails, the entire transaction is rolled back
