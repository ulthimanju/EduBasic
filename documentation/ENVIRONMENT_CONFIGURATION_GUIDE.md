# Environment-Based URL Configuration Guide

## Overview

Both the frontend and backend have been refactored to use environment-based URL configuration instead of hardcoded URLs. This enables seamless deployment across different environments (development, staging, production) without code changes.

---

## Backend Configuration

### 1. **Application Properties** (`backend/src/main/resources/application.properties`)

Added new environment-configurable properties:

```properties
# Frontend URL Configuration
app.frontend.url=${FRONTEND_URL:http://localhost:5173}

# Gemini API Base URL Configuration
app.gemini.api.base-url=${GEMINI_API_URL:https://generativelanguage.googleapis.com/v1beta}
```

**Environment Variables:**
- `FRONTEND_URL` – Frontend application URL (default: `http://localhost:5173`)
- `GEMINI_API_URL` – Gemini API base URL (default: `https://generativelanguage.googleapis.com/v1beta`)

### 2. **AppUrlConfig Component** (`backend/src/main/java/com/edubas/backend/config/AppUrlConfig.java`)

New centralized configuration class that reads environment properties:

```java
@Component
public class AppUrlConfig {
    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    @Value("${app.gemini.api.base-url}")
    private String geminiApiBaseUrl;
    
    public String getFrontendUrl() { return frontendUrl; }
    public String getGeminiApiBaseUrl() { return geminiApiBaseUrl; }
    public String getAllowedOrigin() { return frontendUrl; }
}
```

### 3. **CORS Configuration Updates** (`backend/src/main/java/com/edubas/backend/config/CorsConfig.java`)

CorsConfig now uses AppUrlConfig instead of hardcoded `http://localhost:5173`:

```java
@Configuration
public class CorsConfig {
    @Autowired
    private AppUrlConfig appUrlConfig;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
            java.util.Arrays.asList(appUrlConfig.getAllowedOrigin())
        );
        // ... rest of config
    }
}
```

### 4. **Removed Hardcoded @CrossOrigin Decorators**

Removed redundant `@CrossOrigin` annotations from all controllers:
- `AuthController`
- `ProfileController`
- `UploadController`
- `CourseController`
- `AnnouncementController`

The centralized CorsConfig now handles all CORS settings globally.

### 5. **Gemini Service Updates** (`backend/src/main/java/com/edubas/backend/service/GeminiService.java`)

Updated to use configurable Gemini API base URL:

**Before:**
```java
String urlWithKey = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName
    + ":generateContent?key=" + apiKey;
```

**After:**
```java
String urlWithKey = appUrlConfig.getGeminiApiBaseUrl() + "/models/" + modelName
    + ":generateContent?key=" + apiKey;
```

---

## Frontend Configuration

### 1. **API Client Utility** (`frontend/src/utils/apiClient.js`)

Centralized fetch wrapper that handles:
- Relative URLs (no hardcoded domains)
- Auto-injected Bearer token from localStorage
- Environment variable support (`VITE_API_BASE_URL`)
- SSR-safe window checks

```javascript
export const apiFetch = async (endpoint, options = {}) => {
  const baseUrl = getBaseUrl();
  const token = getAuthToken();
  
  const finalUrl = baseUrl + endpoint;
  const headers = {
    ...getDefaultHeaders(),
    ...options.headers
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  return fetch(finalUrl, { ...options, headers });
};
```

### 2. **Vite Dev Proxy** (`frontend/vite.config.js`)

Dev server automatically routes `/api/*` to backend:

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

### 3. **Migrated Pages to Use apiFetch**

All pages updated to use relative URLs via `apiFetch()`:

| Page | Changes |
|------|---------|
| LoginPage.jsx | `http://localhost:8080/api/auth/login` → `apiFetch(apiEndpoints.auth.login)` |
| RegisterPage.jsx | `http://localhost:8080/api/auth/register` → `apiFetch(apiEndpoints.auth.register)` |
| DashboardPage.jsx | Profile fetch → `apiFetch(apiEndpoints.profile.settings)` |
| FeedPage.jsx | Announcements → `apiFetch(apiEndpoints.announcements.*)` |
| AdminPage.jsx | Admin announcements → `apiFetch(apiEndpoints.announcements.*)` |
| UploadContentPage.jsx | File upload → `apiFetch(apiEndpoints.upload)` |
| ViewContentPage.jsx | Courses/modules → `apiFetch(apiEndpoints.courses.*)` |
| AppLayout.jsx | Logout → `apiFetch(apiEndpoints.auth.logout)` |

### 4. **API Endpoints Object**

```javascript
export const apiEndpoints = {
  auth: {
    login: '/api/auth/login',
    register: '/api/auth/register',
    logout: '/api/auth/logout'
  },
  profile: {
    settings: '/api/profile/settings'
  },
  announcements: {
    list: '/api/announcements',
    get: (id) => `/api/announcements/${id}`,
    delete: (id) => `/api/announcements/${id}`,
    create: '/api/announcements',
    update: (id) => `/api/announcements/${id}`
  },
  courses: {
    list: '/api/courses',
    levels: (courseId) => `/api/courses/${courseId}/levels`,
    modules: (courseId, levelId) => `/api/courses/${courseId}/levels/${levelId}/modules`,
    lessons: (courseId, levelId, moduleId) => `/api/courses/${courseId}/levels/${levelId}/modules/${moduleId}/lessons`,
    lesson: (courseId, levelId, moduleId, lessonId) => `/api/courses/${courseId}/levels/${levelId}/modules/${moduleId}/lessons/${lessonId}`,
    fixVisualization: (courseId, levelId, moduleId, lessonId) => `/api/courses/${courseId}/levels/${levelId}/modules/${moduleId}/lessons/${lessonId}/fix-visualization`
  },
  upload: '/api/upload'
};
```

---

## Deployment Scenarios

### Scenario 1: Development (Local)

**Backend Setup:**
```bash
cd backend
./mvnw spring-boot:run
# Runs on http://localhost:8080
```

**Frontend Setup:**
```bash
cd frontend
npm run dev
# Runs on http://localhost:5173
# Vite proxy automatically routes /api/* to http://localhost:8080
```

**No environment variables needed** – uses defaults from application.properties and vite.config.js

---

### Scenario 2: Production – React from Spring Boot

**Package React as static assets in Spring Boot:**

```bash
cd frontend
npm run build
cp -r dist/* ../backend/src/main/resources/static/
```

**Backend Setup:**
```bash
cd backend
./mvnw clean package
# java -jar target/edubas-backend-0.0.1-SNAPSHOT.jar
```

**Environment Variables (optional):**
```bash
# Not needed – frontend and backend are co-located
export FRONTEND_URL=https://yourdomain.com
export GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta
```

**Why it works:**
- Frontend served from `http://localhost:8080` or production domain
- Frontend makes requests to `/api/*` (relative URLs)
- Backend serves these requests automatically
- CORS allows same-origin requests

---

### Scenario 3: Production – Separate Domains

**Frontend on CDN/separate domain, Backend on different domain:**

**Setup:**
```bash
# Deploy frontend to Vercel/Netlify/CloudFlare
cd frontend
npm run build
# Deploy dist/ to your CDN

# Deploy backend to AWS/Azure/GCP
cd backend
./mvnw clean package
java -jar target/edubas-backend-0.0.1-SNAPSHOT.jar
```

**Environment Variables (required):**
```bash
# On frontend (environment file or build-time)
VITE_API_BASE_URL=https://api.yourdomain.com

# On backend
export FRONTEND_URL=https://yourdomain.com
export GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta
```

**Result:**
- Frontend: `https://yourdomain.com`
- Backend: `https://api.yourdomain.com`
- Frontend requests: `https://api.yourdomain.com/api/courses`
- CORS configured to allow `https://yourdomain.com`

---

### Scenario 4: Staging

**Separate staging servers:**

```bash
# On staging backend
export FRONTEND_URL=https://staging.yourdomain.com
export GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta
java -jar edubas-backend-0.0.1-SNAPSHOT.jar

# On staging frontend
export VITE_API_BASE_URL=https://staging-api.yourdomain.com
npm run build
# Deploy to staging CDN
```

---

## Build & Runtime Configuration

### Frontend Build-Time Configuration

Set `VITE_API_BASE_URL` before building:

```bash
cd frontend

# Development (uses dev proxy)
VITE_API_BASE_URL=/api npm run dev

# Production with same-origin backend
VITE_API_BASE_URL=/api npm run build

# Production with separate API domain
VITE_API_BASE_URL=https://api.yourdomain.com npm run build
```

### Backend Runtime Configuration

Pass environment variables at startup:

```bash
cd backend

# Development
./mvnw spring-boot:run

# Production with custom URLs
FRONTEND_URL=https://yourdomain.com \
GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta \
java -jar target/edubas-backend-0.0.1-SNAPSHOT.jar
```

---

## Summary of Changes

### Backend Files Modified:
1. ✅ `application.properties` – Added app.frontend.url and app.gemini.api.base-url
2. ✅ `AppUrlConfig.java` – NEW centralized configuration class
3. ✅ `CorsConfig.java` – Uses AppUrlConfig instead of hardcoded URL
4. ✅ `GeminiService.java` – Uses configurable Gemini API base URL
5. ✅ `AuthController.java` – Removed @CrossOrigin decorator
6. ✅ `ProfileController.java` – Removed @CrossOrigin decorator
7. ✅ `UploadController.java` – Removed @CrossOrigin decorator
8. ✅ `CourseController.java` – Removed @CrossOrigin decorator
9. ✅ `AnnouncementController.java` – Removed @CrossOrigin decorator

### Frontend Files Modified:
1. ✅ `vite.config.js` – Added dev proxy configuration
2. ✅ `apiClient.js` – NEW centralized API client utility
3. ✅ `LoginPage.jsx` – Uses apiFetch
4. ✅ `RegisterPage.jsx` – Uses apiFetch
5. ✅ `DashboardPage.jsx` – Uses apiFetch
6. ✅ `FeedPage.jsx` – Uses apiFetch
7. ✅ `AdminPage.jsx` – Uses apiFetch
8. ✅ `UploadContentPage.jsx` – Uses apiFetch
9. ✅ `ViewContentPage.jsx` – Uses apiFetch
10. ✅ `AppLayout.jsx` – Uses apiFetch for logout

---

## Testing Deployment Scenarios

### Test Dev Setup:
```bash
# Terminal 1: Backend
cd backend && ./mvnw spring-boot:run

# Terminal 2: Frontend
cd frontend && npm run dev

# Access: http://localhost:5173
# All API calls route via Vite proxy to localhost:8080
```

### Test Prod Setup (Co-located):
```bash
# Build frontend
cd frontend && npm run build

# Copy to Spring Boot resources
cp -r dist/* ../backend/src/main/resources/static/

# Build and run backend
cd ../backend && ./mvnw clean package
java -jar target/edubas-backend-0.0.1-SNAPSHOT.jar

# Access: http://localhost:8080
```

### Test Separate Domains:
```bash
# Build frontend for separate API
cd frontend
VITE_API_BASE_URL=http://localhost:8080 npm run build

# Serve frontend on different port (e.g., 3000)
cd dist && python -m http.server 3000

# Run backend on 8080
cd ../../backend && ./mvnw spring-boot:run

# Set CORS origin
export FRONTEND_URL=http://localhost:3000
# (Then restart backend)

# Access: http://localhost:3000
# All API calls go to http://localhost:8080
```

---

## Key Benefits

✅ **No Code Changes for Different Environments** – Same code works in dev, staging, production  
✅ **Flexible Deployment Options** – Co-located backend+frontend OR separate domains  
✅ **Environment-Specific Configuration** – Use environment variables to customize URLs  
✅ **CORS Centralized** – All CORS settings in one place, easy to manage  
✅ **Token Management Centralized** – No scattered Bearer token injection code  
✅ **Type-Safe API Endpoints** – `apiEndpoints` object provides autocomplete support  
✅ **Scalable** – Easy to add new endpoints to `apiEndpoints`  
✅ **Dev Experience** – Vite proxy eliminates CORS issues in development  

---

## Troubleshooting

### Frontend getting CORS errors in development:
- **Ensure** Vite dev server is running (`npm run dev`)
- **Ensure** backend is running on `http://localhost:8080`
- **Check** `vite.config.js` has proxy configured

### API calls fail in production:
- **Check** `VITE_API_BASE_URL` is set correctly during build
- **Check** `FRONTEND_URL` is set correctly on backend
- **Verify** CORS headers in response: `Access-Control-Allow-Origin: $FRONTEND_URL`

### Backend not accepting requests from frontend:
- **Check** `app.frontend.url` matches actual frontend URL
- **Check** `AppUrlConfig.getAllowedOrigin()` returns correct value
- **Restart** backend after changing environment variables

---

## Next Steps

1. **Deploy to Azure/AWS/GCP** – Use environment variables for URLs
2. **Configure CI/CD** – Set `VITE_API_BASE_URL` during frontend build
3. **Monitor Logs** – Track CORS and auth errors
4. **Document URLs** – Keep record of all deployed URLs per environment
