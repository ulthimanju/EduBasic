# EduBas - Adaptive AI-Powered Learning Platform

An intelligent learning management system with AI-powered code execution, course management, and personalized learning paths.

---

## 📚 Table of Contents

- [Project Overview](#project-overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Backend Architecture](#backend-architecture)
- [Frontend Architecture](#frontend-architecture)
- [Key Features](#key-features)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Environment Configuration](#environment-configuration)
- [Deployment](#deployment)

---

## 🎯 Project Overview

**EduBas** is a full-stack adaptive learning platform that combines:
- **Graph-based course management** using Neo4j
- **AI-powered code execution** with Google Gemini API
- **Real-time announcements** and user feedback
- **Responsive, accessible UI** with React and TailwindCSS
- **Secure authentication** with JWT tokens

The platform enables educators to upload structured course content and students to learn through interactive lessons, practice problems, and code execution.

---

## ⚙️ Tech Stack

### Backend
- **Framework**: Spring Boot 3.4.0 (Java 21)
- **Database**: Neo4j Graph Database (Cloud-hosted)
- **Authentication**: JWT (JJWT 0.12.3)
- **Security**: Spring Security
- **AI Integration**: Google Gemini API (v1beta)
- **Build Tool**: Maven
- **Additional**: Jackson JSON, Lombok

### Frontend
- **Framework**: React 19
- **Build Tool**: Vite 7.2.4
- **Styling**: TailwindCSS 3.4 + PostCSS
- **Code Editor**: Monaco Editor
- **Markdown Rendering**: React Markdown + Mermaid
- **State Management**: React Context API

---

## 📁 Project Structure

```
WIN_SEM_PRO_V2/
├── backend/                          # Spring Boot application
│   ├── src/main/java/com/edubas/
│   │   ├── config/                   # Security, CORS, Jackson configs
│   │   ├── controller/               # REST endpoints
│   │   ├── service/                  # Business logic
│   │   ├── dto/                      # Data transfer objects
│   │   ├── model/                    # Neo4j entities
│   │   └── repository/               # Data access layer
│   ├── src/main/resources/
│   │   ├── application.properties    # Backend configuration
│   │   └── static/                   # Production frontend (after build)
│   └── pom.xml                       # Maven dependencies
│
├── frontend/                         # React application
│   ├── src/
│   │   ├── pages/                    # Page components
│   │   ├── components/
│   │   │   ├── atoms/                # Basic UI elements
│   │   │   ├── molecules/            # Composite components
│   │   │   └── organisms/            # Complex components
│   │   ├── layouts/                  # Layout wrappers
│   │   ├── hooks/                    # Custom React hooks
│   │   ├── context/                  # React Context (Auth, Theme)
│   │   ├── utils/                    # Helper functions
│   │   ├── App.jsx                   # Main app component
│   │   └── main.jsx                  # Entry point
│   ├── package.json                  # Dependencies
│   ├── vite.config.js                # Vite configuration with proxy
│   └── tailwind.config.js            # TailwindCSS settings
│
└── documentation/                    # Implementation guides
```

---

## 🔧 Backend Architecture

### Controllers (REST API)

| Controller | Endpoints | Purpose |
|---|---|---|
| `AuthController` | `/api/auth/*` | Login, register, token validation, user info |
| `CourseController` | `/api/courses/*` | Fetch courses, levels, modules, lessons |
| `UploadController` | `/api/upload` | Upload course datasets (JSON format) |
| `ProfileController` | `/api/profile/*` | User settings, preferences |
| `AnnouncementController` | `/api/announcements/*` | CRUD announcements |
| `CodeExecutionController` | `/api/execute/*` | Execute code with test cases |
| `HealthController` | `/api/health` | Server health check |

### Data Models (Neo4j Graph Nodes)

```
User
├── id (UUID)
├── username
├── email
├── password (hashed)
├── role (USER/ADMIN)
├── avatar
└── settings (profileVisibility, emailNotifications)

Course
├── id
├── courseId (unique)
├── title
├── description
└── HAS_LEVEL → Level

Level
├── id
├── levelId
├── levelName
├── summary
└── HAS_MODULE → Module

Module
├── id
├── moduleId
├── moduleTitle
├── description
├── estimatedTimeMinutes
└── HAS_LESSON → Lesson

Lesson
├── id
├── lessonId
├── title
├── objectives
└── theoryMarkdown (content)

Announcement
├── id
├── title
├── content
├── createdAt
└── CREATED_BY → User

PracticeProblem
├── id
├── title
├── description
├── testCases (JSON)
└── BELONGS_TO → Course
```

### Service Layer

- **AuthService** – JWT token generation, user validation
- **CourseService** – Dataset upload, validation, Neo4j transactions
- **ProfileService** – User settings management
- **GeminiService** – AI code execution via Google Gemini API
- **AnnouncementService** – CRUD operations for announcements

### Configuration Classes

- **SecurityConfig** – JWT authentication filter, role-based access control
- **CorsConfig** – Environment-based CORS configuration
- **JacksonConfig** – JSON serialization with custom settings
- **AppUrlConfig** – Centralized URL management for deployment flexibility
- **ProfileConfig** – Environment profiles (dev/production)

---

## 🎨 Frontend Architecture

### Page Components

| Page | Route | Purpose |
|---|---|---|
| `LoginPage` | `/login` | User authentication |
| `RegisterPage` | `/register` | New account creation |
| `DashboardPage` | `/dashboard` | Course overview, learning progress |
| `ViewContentPage` | `/courses/:courseId` | View course content with markdown |
| `ProblemPage` | `/problems/:problemId` | Practice problems with code editor |
| `UploadContentPage` | `/upload` | Admin: Upload course datasets |
| `AdminPage` | `/admin` | Admin: Manage announcements |
| `FeedPage` | `/feed` | View announcements feed |

### Component Hierarchy

```
Atoms (Basic Elements)
├── Button, Input, Badge, Checkbox, Toggle
├── Radio, Logo, Skeleton, Arrow

Molecules (Composite)
├── Alert, Modal, Tabs, FileUpload
├── LoadingSpinner, Rating, SelectMenu
├── Slider, TagsInput, CircularProgress

Organisms (Complex)
├── Accordion, Calendar, Cards
├── IconButton, Navbar, Sidebar
└── Forms (Login, Register, Upload)
```

### State Management

- **AuthContext** – User authentication state (currentUser, token)
- **ThemeContext** – Light/dark theme switching
- Custom Hooks:
  - `useAuth()` – Access auth state & methods
  - `useTheme()` – Access theme state & toggle
  - `useThemeState()` – Internal theme management

### API Communication

**`apiClient.js`** – Centralized HTTP client that:
- Adds Bearer token from localStorage automatically
- Handles relative URLs with environment-based base URLs
- Provides `apiEndpoints` object for all API routes
- Includes SSR safety guards

---

## 🌟 Key Features

### 🛡️ Authentication
- JWT token-based authentication
- Persistent sessions with localStorage
- Role-based access control (USER/ADMIN)

### 📖 Course Management
- Hierarchical structure: Course → Level → Module → Lesson
- Markdown content support with code blocks
- Bulk upload via JSON datasets

### 💾 Code Practice
- Monaco Editor for code writing
- AI-powered problem statements via Gemini API
- Piston for reliable code execution across multiple languages
- Test case validation and execution
- Output visualization

### 📣 Announcements
- Real-time platform updates
- Admin creation and management
- Feed viewing for all users

### 👥 User Profiles
- Customizable profile settings
- Avatar generation
- Theme preferences
- Notification settings
- Profile visibility controls

### 📱 Responsive Design
- Mobile-first approach with TailwindCSS
- Dark/Light theme support
- Accessible UI components
- Cross-browser compatibility

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** (Backend)
- **Node.js 18+** (Frontend)
- **Neo4j Cloud Account** (or local Neo4j instance)
- **Google Gemini API Key**

### Backend Setup

```bash
# Navigate to backend directory
cd backend

# Install dependencies (Maven)
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Server starts on http://localhost:8080
```

### Frontend Setup

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Run development server
npm run dev

# Frontend available at http://localhost:5173
# API proxy automatically routes /api/* to http://localhost:8080
```

### Configuration

Create a `.env` file in the `frontend/` directory (optional):

```env
VITE_API_BASE_URL=http://localhost:8080
```

---

## 🔗 API Endpoints

### Authentication
```
POST   /api/auth/register        # Register new user
POST   /api/auth/login           # Login user
POST   /api/auth/logout          # Logout user
GET    /api/auth/me              # Get current user info
```

### Courses
```
GET    /api/courses              # List all courses
GET    /api/courses/:courseId    # Get course details
GET    /api/courses/:courseId/levels    # Get course levels
GET    /api/levels/:levelId/modules     # Get modules in level
GET    /api/modules/:moduleId/lessons   # Get lessons in module
```

### Upload
```
POST   /api/upload               # Upload course dataset (JSON file)
```

### Profile
```
GET    /api/profile/settings     # Get user settings
PUT    /api/profile/settings     # Update user settings
```

### Announcements
```
GET    /api/announcements        # List announcements
POST   /api/announcements        # Create announcement (ADMIN)
PUT    /api/announcements/:id    # Update announcement (ADMIN)
DELETE /api/announcements/:id    # Delete announcement (ADMIN)
```

### Code Execution
```
POST   /api/execute/code         # Execute code snippet
POST   /api/execute/with-tests   # Execute code with test cases
```

---

## 🔑 Environment Configuration

### Backend (application.properties)

```properties
# Server
server.port=8080
spring.application.name=edubasic-backend

# Database (Neo4j)
spring.neo4j.uri=neo4j+s://your-neo4j-uri
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=your-password

# CORS
spring.web.cors.allowed-origins=http://localhost:5173
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS

# Gemini API
gemini.api.key=${GOOGLE_API_KEY}
gemini.model=gemini-2.5-flash
app.gemini.api.base-url=${GEMINI_API_URL:https://generativelanguage.googleapis.com/v1beta}

# Frontend URL
app.frontend.url=${FRONTEND_URL:http://localhost:5173}

# Logging
logging.level.com.edubas=INFO
logging.level.org.springframework.data.neo4j.cypher=ERROR
```

### Frontend (Environment Variables)

```bash
# Optional: Custom API base URL (defaults to /api proxy)
VITE_API_BASE_URL=http://localhost:8080
```

---

## 🚢 Deployment

### Option 1: Single Server (Same Origin)

```bash
# Build frontend
cd frontend
npm run build

# Copy to Spring Boot static directory
cp -r dist/* ../backend/src/main/resources/static/

# Build and run backend
cd ../backend
./mvnw clean package
java -jar target/edubas-backend-0.0.1-SNAPSHOT.jar
```

Access via: `http://localhost:8080`

### Option 2: Separate Domains

```bash
# Frontend deployment (Vercel, Netlify, etc.)
cd frontend
npm run build
# Deploy dist/ directory

# Backend deployment (Docker, AWS, Azure, etc.)
cd backend
./mvnw clean package
java -jar target/edubas-backend-0.0.1-SNAPSHOT.jar
```

Set environment variables:
- Backend: `FRONTEND_URL=https://yourdomain.com`, `GEMINI_API_URL=...`
- Frontend build: `VITE_API_BASE_URL=https://api.yourdomain.com`

### Docker Deployment

```bash
# Build and run backend in Docker
cd backend
docker build -t edubas-backend .
docker run -p 8080:8080 -e GOOGLE_API_KEY=... edubas-backend

# Frontend (as static files in Spring Boot or separate CDN)
```

---

## 👨‍💻 About

**EduBas** is developed as a comprehensive learning platform integrating modern web technologies with AI capabilities.
