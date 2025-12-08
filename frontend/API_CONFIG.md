# API Configuration

## Development
When running `npm run dev`, Vite's proxy configuration forwards all `/api/*` requests to `http://localhost:8080` (Spring Boot).

No hardcoded URLs in React code—just use `/api/...` endpoints.

## Production
Two recommended setups:

### Option 1: Serve React from Spring Boot (Recommended)
1. Build frontend: `npm run build`
2. Copy `dist/` to Spring Boot's `src/main/resources/static/`
3. Configure Spring to serve `index.html` for SPA routing
4. React and backend share the same origin
5. React's `/api/*` calls go to the same host

### Option 2: Separate Domains
If frontend and backend are on different domains, set:

```bash
# .env
VITE_API_BASE_URL=https://api.edubas.com
```

React will prepend this to all API calls.

## API Client
Use `apiFetch()` from `src/utils/apiClient.js` instead of raw `fetch()`:

```js
import apiFetch, { apiEndpoints } from '../utils/apiClient';

// Login
await apiFetch(apiEndpoints.auth.login, {
  method: 'POST',
  body: JSON.stringify({ email, password }),
});

// Get course
await apiFetch(apiEndpoints.courses.get(courseId));
```

The utility handles:
- Base URL (env var or relative)
- Auth token from localStorage
- Standard headers
- SSR safety
