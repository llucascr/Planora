# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.


## Project Overview

Planora is a full-stack project management tool with Kanban boards, GitHub issue integration, and AI-powered assistance. It consists of three main components: a Spring Boot backend, a React/TypeScript frontend, and an AI/ML module with fine-tuned Llama models.

## Commands

### Backend (Spring Boot — `backend/`)
```bash
./mvnw spring-boot:run      # Run the application (port 8080)
./mvnw clean install        # Build
./mvnw test                 # Run all tests
./mvnw test -Dtest=ClassName#methodName  # Run a single test
```

### Frontend (React — `frontend/`)
```bash
npm run dev      # Start dev server (port 3000)
npm run build    # TypeScript compile + Vite build
npm run lint     # ESLint
npm run preview  # Preview production build
```

### Environment Setup
- Backend requires a `.env` or environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`
- RSA key files must exist at `backend/src/main/resources/app.pub` and `app.key`
- Frontend requires a `.env` file based on `frontend/.env.example`:
  ```
  VITE_PORT=3000
  VITE_API_URL=http://localhost:8080/
  VITE_URL=http://localhost:3000
  VITE_TOKEN_COOKIE_NAME=token
  ```

## Architecture

### Data Flow
```
React Frontend → (JWT in cookie) → Spring Boot REST API → PostgreSQL
                                                         → GitHub API (via GithubClient)
                                                         → LangChain4J / Gemini AI
```

### Authentication
GitHub OAuth2 → `CustomOAuth2SuccessHandler` → `OauthService` generates RS256 JWT → stored as browser cookie. The backend is stateless; all requests are authenticated via JWT resource server. RSA key pair (`app.pub`/`app.key`) signs and verifies tokens.

### Backend Structure (`backend/src/main/java/com/planora/backend/`)
- **controller/** — REST endpoints (`KanbanController`, `GithubController`)
- **service/** — Business logic (`KanbanBoardService`, `KanbanColumnService`, `GithubService`, `OauthService`, `UserService`)
- **model/** — JPA entities: `User`, `Role`, `KanbanBoard`, `KanbanColumn`, `KanbanMember`, `Issue`, `Label`
- **repository/** — Spring Data JPA interfaces
- **dto/** — Request/response DTOs (never expose entities directly)
- **config/** — `SecurityConfig`, `JwtConfig`, `GithubClientConfig`, `AdminUserConfig`
- **client/** — `GithubClient`: declarative HTTP client for the GitHub API
- **exception/** — `DataNotFoundException`, `DataAlreadyExistException`, `CustomEntityResponseHandler`

Key entity relationships: `KanbanBoard` has many `KanbanColumn`s and `KanbanMember`s; `KanbanBoard` references a GitHub repo (`githubRepository`, `githubOwnerName`); `Issue` is linked to a board and has `Label`s.

### Frontend Structure (`frontend/src/`)
- **pages/** — `HomePage`, `LoginPage`, `ErrorPage`, `LayoutPage`
- **components/** — Reusable UI: `Button`, `Input`, `Modal`, `Select`, `Sidebar`, `DataTable`
- **contexts/** — `UIContext` (modal state), `NotificationContext` (toasts)
- **hooks/** — `useFetch`, `useCookie`
- **utils/** — `ApiFetch` (HTTP client wrapper), `ApplyMask`

API calls go through `ApiFetch`, which reads the base URL from `VITE_API_URL` and attaches the JWT cookie automatically.

### AI/ML (`IA/`)
Fine-tuned Meta-Llama-3-8B models for backlog/issue generation, trained with TRL + LoRA (PEFT). Integrated into the backend via LangChain4J (with Gemini AI and minilm-l6-v2 embeddings). Training notebooks and datasets live in `IA/`.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, React Router DOM |
| Backend | Java 21, Spring Boot 4, Spring Security OAuth2, Spring Data JPA |
| Database | PostgreSQL |
| AI | LangChain4J 1.0.0-beta2, Gemini, Llama-3-8B (LoRA fine-tuned) |
| Build | Maven (backend), npm (frontend) |

## Coding Standards

All code written in this repository must follow these principles. These are non-negotiable requirements, not suggestions.

### Clean Code
- Use meaningful, intention-revealing names for variables, methods, and classes
- Functions/methods must do one thing only and do it well
- Keep functions short — prefer small, focused methods over long procedural blocks
- Avoid magic numbers and strings; use named constants
- Remove dead code, commented-out code, and unused imports
- Write self-documenting code; add comments only when the logic cannot speak for itself

### SOLID Principles
- **S** — Single Responsibility: each class/module has one reason to change
- **O** — Open/Closed: open for extension, closed for modification
- **L** — Liskov Substitution: subtypes must be substitutable for their base types
- **I** — Interface Segregation: prefer small, focused interfaces over fat ones
- **D** — Dependency Inversion: depend on abstractions, not concrete implementations; inject dependencies

### Clean Architecture
- Keep layers separate: controllers must not contain business logic; services must not contain persistence logic
- DTOs cross layer boundaries — never expose JPA entities through the API
- Business rules live in the service layer, independent of frameworks
- Dependencies point inward: outer layers (controllers, repositories) depend on inner layers (services, domain), never the reverse
- Avoid coupling between unrelated modules; communicate through well-defined interfaces
