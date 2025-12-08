# Environment-Based URL Configuration - Quick Reference

## What Changed?

✅ **Backend**: Hardcoded URLs replaced with environment variables  
✅ **Frontend**: All fetch calls use centralized `apiFetch()` with relative URLs  
✅ **Both**: Ready for any deployment scenario without code changes

---

## Key Files

### Backend
- `application.properties` – Now has `app.frontend.url` and `app.gemini.api.base-url`
- `AppUrlConfig.java` – NEW class that reads environment properties
- `CorsConfig.java` – Updated to use AppUrlConfig (no more hardcoded origins)
- `GeminiService.java` – Uses configurable Gemini API URL

### Frontend
- `apiClient.js` – NEW centralized API client with Bearer token injection
- `vite.config.js` – Has dev proxy for `/api` routes
- All pages – Now import and use `apiFetch()` and `apiEndpoints`

---

## How It Works

### Development
```bash
# Terminal 1
cd backend && ./mvnw spring-boot:run

# Terminal 2
cd frontend && npm run dev

# Frontend makes requests to /api/... 
# Vite proxy automatically routes to http://localhost:8080
```

### Production (Same Origin)
```bash
# Frontend built as React assets in Spring Boot
npm run build  # in frontend
cp dist/* ../backend/src/main/resources/static/

# Backend serves everything
./mvnw clean package
java -jar edubas-backend-0.0.1-SNAPSHOT.jar

# Access: http://localhost:8080 (frontend)
# API: http://localhost:8080/api/... (same server)
```

### Production (Separate Domains)
```bash
# Set environment variables
export VITE_API_BASE_URL=https://api.yourdomain.com  # for frontend build
export FRONTEND_URL=https://yourdomain.com            # for backend runtime
export GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta

# Build and deploy
cd frontend && npm run build  # dist/ → CDN
cd ../backend && ./mvnw clean package  # → API Server

# Frontend: https://yourdomain.com → CDN
# Backend: https://api.yourdomain.com → API Server
```

---

## Environment Variables

| Variable | Default | Where Used |
|----------|---------|-----------|
| `FRONTEND_URL` | `http://localhost:5173` | Backend (CORS) |
| `GEMINI_API_URL` | `https://generativelanguage.googleapis.com/v1beta` | Backend (Gemini API) |
| `VITE_API_BASE_URL` | Not set (uses relative `/api`) | Frontend build-time |

---

## API Endpoints

All endpoints are defined in `frontend/src/utils/apiClient.js`:

```javascript
// Usage examples:
apiFetch(apiEndpoints.auth.login, { method: 'POST', body: ... })
apiFetch(apiEndpoints.profile.settings, { method: 'GET' })
apiFetch(apiEndpoints.announcements.list, { method: 'GET' })
apiFetch(apiEndpoints.courses.levels(courseId), { method: 'GET' })
apiFetch(apiEndpoints.courses.lesson(cId, lId, mId, lId), { method: 'GET' })
```

---

## Deployment Checklist

- [ ] Backend builds successfully: `./mvnw clean package -DskipTests`
- [ ] Frontend builds successfully: `npm run build`
- [ ] Set `FRONTEND_URL` on backend runtime
- [ ] Set `VITE_API_BASE_URL` during frontend build (if separate domains)
- [ ] Verify CORS headers in responses
- [ ] Test API calls work from frontend
- [ ] Verify token authentication works

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| CORS errors in dev | Ensure `npm run dev` is running and backend on `:8080` |
| API 404 in prod | Check `VITE_API_BASE_URL` was set during build |
| API 403 in prod | Verify `FRONTEND_URL` matches actual frontend domain |
| Gemini API fails | Check `GEMINI_API_URL` is correct and API key is set |

---

## Migration Summary

**Pages Updated**: 8  
- LoginPage, RegisterPage, DashboardPage, FeedPage
- AdminPage, UploadContentPage, ViewContentPage, AppLayout

**Hardcoded URLs Removed**: ~20

**Controllers Updated**: 5  
- AuthController, ProfileController, UploadController
- CourseController, AnnouncementController

**Build Status**: ✅ All builds pass
