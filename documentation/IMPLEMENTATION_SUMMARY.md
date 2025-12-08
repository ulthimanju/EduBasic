# Environment-Based API Configuration - Implementation Summary

## What Was Implemented

### 1. API Client Utility (`src/utils/apiClient.js`)
- **`apiFetch()`** – Wrapper around `fetch()` that:
  - Handles relative URLs (no hardcoded `http://localhost:8080`)
  - Automatically adds `Authorization` header with Bearer token from localStorage
  - Reads `VITE_API_BASE_URL` env var for custom base URLs (prod with separate domains)
  - Includes SSR safety guard for `typeof window`
  
- **`apiEndpoints`** – Object mapping for all API routes:
  - Auth: login, register, logout
  - Profile: settings
  - Announcements: list, create, update, delete
  - Courses: list, levels, modules, lessons

### 2. Vite Dev Proxy (`vite.config.js`)
```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // Spring Boot
      changeOrigin: true,
    },
  },
},
```
- Routes all `/api/*` requests to `http://localhost:8080` in dev
- Developers don't need to configure anything—just run `npm run dev`

### 3. Updated Files with Relative URLs

#### Already Updated (✅)
- `src/layouts/AppLayout.jsx` – Logout uses `apiFetch` + `apiEndpoints.auth.logout`
- `src/pages/LoginPage.jsx` – Uses `apiEndpoints.auth.login`
- `src/pages/RegisterPage.jsx` – Uses `apiEndpoints.auth.register`
- `src/pages/DashboardPage.jsx` – Uses `apiEndpoints.profile.settings`
- `src/pages/FeedPage.jsx` – Uses `apiEndpoints.announcements.*`

#### Partial/Todo (Update as needed)
- `src/pages/AdminPage.jsx` – Announcements CRUD
- `src/pages/UploadContentPage.jsx` – File upload
- `src/pages/ViewContentPage.jsx` – Courses, levels, modules, lessons

---

## How It Works

### Development
```
React (localhost:5173)
      ↓ `/api/auth/login`
Vite Proxy (5173)
      ↓ [rewrite to `http://localhost:8080/api/auth/login`]
Spring Boot (8080)
```

### Production
**Option 1: Serve React from Spring Boot** (Recommended)
```
Spring Boot (https://edubas.com)
  ├─ /static/index.html
  ├─ /static/assets/...
  └─ /api/* (backend routes)

React calls: `/api/auth/login` → Spring Boot `/api/auth/login` (same origin)
```

**Option 2: Separate Domains** (Set `.env`)
```
VITE_API_BASE_URL=https://api.edubas.com

React (https://edubas.com)
      ↓ `/api/auth/login`
`${VITE_API_BASE_URL}/api/auth/login` → https://api.edubas.com/api/auth/login
```

---

## Quick Start

### Dev
```bash
npm run dev
```
Vite proxy automatically routes `/api/*` to `http://localhost:8080`.

### Prod - Option 1 (React from Spring Boot)
```bash
# Build frontend
npm run build

# Copy dist/ to Spring Boot
cp -r dist/* ../backend/src/main/resources/static/

# Spring Boot serves everything
```

### Prod - Option 2 (Separate Domains)
```bash
# Set env var
export VITE_API_BASE_URL=https://api.edubas.com

# Build
npm run build

# Deploy React to https://edubas.com (static hosting)
# Deploy backend to https://api.edubas.com
```

---

## Migration Checklist

- [x] Create `apiClient.js` utility
- [x] Add Vite proxy config
- [x] Update AppLayout (logout)
- [x] Update LoginPage
- [x] Update RegisterPage
- [x] Update DashboardPage
- [x] Update FeedPage
- [ ] Update AdminPage (follow same pattern)
- [ ] Update UploadContentPage (follow same pattern)
- [ ] Update ViewContentPage (follow same pattern)

### Pattern for Remaining Files
Replace all `fetch('http://localhost:8080/api/...')` with:
```js
import apiFetch, { apiEndpoints } from '../utils/apiClient';

await apiFetch(apiEndpoints.auth.login, {
  method: 'POST',
  body: JSON.stringify({ ... }),
});
```

---

## Benefits

✅ **No hardcoded URLs** in React code  
✅ **Same code** works in dev, staging, prod  
✅ **Environment-agnostic** – routing handled by dev proxy or env vars  
✅ **CORS-friendly** – Vite proxy eliminates CORS issues in dev  
✅ **Scalable** – easily support multiple backend domains
