# Mermaid Diagram Auto-Fix Implementation

## Overview
Automated system to fix broken Mermaid diagrams using Google's Gemini API.

## Components Created

### Backend (Java/Spring Boot)

1. **GeminiService.java** - Service to interact with Gemini API
   - `fixMermaidCode()` - Sends broken Mermaid code to Gemini with context
   - Constructs detailed prompts for better fixes
   - Extracts and cleans the fixed code from API response

2. **MermaidFixResponse.java** - DTO for API responses
   - `success` - Whether fix was successful
   - `fixedCode` - The corrected Mermaid code
   - `message` - Status/error message

3. **CourseController.java** - Added endpoint:
   - `POST /api/courses/{courseId}/levels/{levelId}/modules/{moduleId}/lessons/{lessonId}/fix-visualization`
   - Retrieves lesson, extracts Mermaid code
   - Calls GeminiService to fix code
   - Updates database with fixed code

### Frontend (React)

1. **MermaidDiagram Component** - Enhanced with fix capability
   - Shows "Fix Diagram with AI" button on render errors
   - Calls backend endpoint to fix diagram
   - Reloads lesson content after successful fix

## Setup Instructions

### 1. Get Gemini API Key
- Visit: https://makersuite.google.com/app/apikey
- Create a new API key
- Copy the key

### 2. Configure Backend
Add to your environment or replace in `application.properties`:

```bash
# Windows (PowerShell)
$env:GEMINI_API_KEY="your-actual-api-key-here"

# Linux/Mac
export GEMINI_API_KEY="your-actual-api-key-here"
```

Or directly in `application.properties`:
```properties
gemini.api.key=your-actual-api-key-here
```

### 3. Start Backend
```bash
cd backend
mvn spring-boot:run
```

### 4. Frontend is Ready
No additional setup needed. The fix button will appear automatically when Mermaid diagrams fail to render.

## How It Works

1. **User selects lesson** → Mermaid diagram tries to render
2. **If rendering fails** → Error message shows with "Fix Diagram with AI" button
3. **User clicks button** → Frontend calls backend endpoint
4. **Backend:**
   - Retrieves lesson and Mermaid code
   - Sends to Gemini API with context (lesson title, theory excerpt)
   - Gemini fixes syntax errors
   - Updates database with fixed code
5. **Frontend:** Reloads lesson content → Diagram renders successfully

## API Endpoint Details

### Request
```
POST http://localhost:8080/api/courses/{courseId}/levels/{levelId}/modules/{moduleId}/lessons/{lessonId}/fix-visualization
Headers: Authorization: Bearer <token>
```

### Response
```json
{
  "success": true,
  "fixedCode": "flowchart TD\n    A[Start] --> B[End]",
  "message": "Visualization fixed successfully"
}
```

## Metadata Used for Fixing

1. **Lesson Title** - Helps Gemini understand the diagram's purpose
2. **Theory Markdown (first 300 chars)** - Provides context about the lesson content
3. **Broken Mermaid Code** - The invalid diagram that needs fixing

## Rate Limiting Considerations

- Currently no rate limiting implemented
- Gemini Free tier: 60 requests/minute
- Consider adding cooldown if needed:
  - Track fix attempts per lesson
  - Limit to 1 fix per hour per lesson

## Future Enhancements

1. **Validation Layer** - Validate fixed code before saving
2. **Admin Dashboard** - View all fix attempts and success rates
3. **Batch Processing** - Fix all broken diagrams in one go
4. **History Tracking** - Keep history of fixes for rollback

## Testing

1. Find a lesson with broken Mermaid code (look for syntax errors in console)
2. Click "Fix Diagram with AI" button
3. Wait for processing (usually 2-3 seconds)
4. Diagram should render successfully
5. Check database - visualizationJson should be updated

## Troubleshooting

**Button doesn't work:**
- Check browser console for errors
- Verify backend is running
- Check Gemini API key is set correctly

**Fix fails:**
- Check backend logs for Gemini API errors
- Verify API key has sufficient quota
- Check network connectivity to Gemini API

**Diagram still broken after fix:**
- Gemini may not understand complex diagrams
- Check backend logs for the fixed code
- May need manual intervention
