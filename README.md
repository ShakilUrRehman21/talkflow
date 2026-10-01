# Talk Flow — Real-Time Chat Workspace

Talk Flow is a modern, real-time multi-channel chat application built with **Spring Boot 3**, **Spring Security 6**, **Spring WebSocket (STOMP / SockJS)**, **Thymeleaf**, and **Spring Data JPA**.

It features a minimal dark slate interface, WhatsApp-style speech bubble alignment, active user presence synchronization, persistent message history, and multi-channel rooms.

---

## Features

- **WhatsApp-Style Messaging**: Active user messages align neatly to the right (`#1e3a5f`), incoming messages from other members align to the left (`#181d28`), with timestamps in the bottom-right corner.
- **Real-Time Presence**: Instant member join, leave, and online count updates powered by server-side WebSocket session tracking.
- **Multi-Channel Rooms**: Seamlessly switch between channels (`# design-trends`, `# general`, `# night-talk`, `# deep-talk`) with isolated messaging and member rosters.
- **Message Persistence**: Chat messages persist across sessions and server restarts using Spring Data JPA.
- **Dual-Mode Database**:
  - **Local Development**: Embedded file-based H2 database (`./data/chatdb`) with zero configuration required.
  - **Cloud Production**: Out-of-the-box support for PostgreSQL (Neon, Supabase, Railway, Render) via environment variables.
- **Secure Authentication**: User registration and login protected by Spring Security 6 with BCrypt password hashing.
- **Minimal, Clean UI**: Thoughtfully designed dark slate interface with no cartoonish emojis or cluttered layouts.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | Spring Boot 3.2.5 (Java 21 LTS) |
| **Security** | Spring Security 6 (Component-based `SecurityFilterChain`, BCrypt) |
| **Real-Time Protocol** | Spring WebSocket, STOMP, SockJS |
| **Persistence** | Spring Data JPA / Hibernate ORM |
| **Databases** | H2 (Local file) / PostgreSQL (Cloud production) |
| **Frontend** | Thymeleaf, Vanilla CSS, Vanilla JavaScript (STOMP.js + SockJS) |
| **Build & Container** | Maven Wrapper, Docker (Multi-stage Eclipse Temurin JRE 21) |

---

## Getting Started Locally

### Prerequisites
- **Java JDK 21+** installed and available on your `PATH`.
- Git installed.

### Run the App
```bash
# Clone the repository
git clone https://github.com/<your-username>/talk-flow.git
cd talk-flow

# Run with Maven Wrapper (Windows)
.\mvnw.cmd spring-boot:run

# Run with Maven Wrapper (macOS / Linux)
./mvnw spring-boot:run
```

Once started, open your browser:
```
http://localhost:8080
```

1. **Sign Up**: Create an account at `/registration`.
2. **Log In**: Sign in at `/login`.
3. **Chat**: You will be redirected to `/chat`. Open an Incognito window or second browser tab with another account to test real-time messaging across tabs!

---

## How to Push to GitHub

If you haven't initialized Git yet:

```bash
# 1. Initialize git
git init

# 2. Stage all project files (.gitignore excludes build artifacts & local DB files)
git add .

# 3. Create initial commit
git commit -m "feat: initial release of Talk Flow chat application"

# 4. Set default branch to main
git branch -M main

# 5. Link your GitHub remote repository (replace with your repo URL)
git remote add origin https://github.com/<your-username>/<your-repo-name>.git

# 6. Push to GitHub
git push -u origin main
```

---

## Free Cloud Deployment Guide

You can deploy Talk Flow completely free using **Render** or **Railway** paired with a free **Neon** PostgreSQL database.

### Step 1: Create a Free PostgreSQL Database (Neon.tech)
1. Go to [Neon.tech](https://neon.tech) and create a free account.
2. Create a new project (e.g., `talk-flow-db`).
3. Under **Dashboard > Connection Details**, copy the connection string.
   - It will look like:
     ```
     postgresql://username:password@ep-xyz.us-east-2.aws.neon.tech/neondb?sslmode=require
     ```
   - Convert it to JDBC format:
     ```
     jdbc:postgresql://ep-xyz.us-east-2.aws.neon.tech/neondb?sslmode=require
     ```

### Step 2: Deploy on Render.com (Free)
1. Go to [Render.com](https://render.com) and create a free account.
2. Click **New +** > **Web Service**.
3. Connect your GitHub repository.
4. Set the following configuration:
   - **Environment**: `Docker` (Render will automatically detect the included `Dockerfile`)
   - **Plan**: `Free`
5. Under **Environment Variables**, add:
   | Variable | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<neon-host>/neondb?sslmode=require` |
   | `SPRING_DATASOURCE_USERNAME` | `<neon-username>` |
   | `SPRING_DATASOURCE_PASSWORD` | `<neon-password>` |
6. Click **Create Web Service**.
7. Render will build the Docker container and provide a live URL (`https://talk-flow-xyz.onrender.com`).

---

## Project Structure

```
.
├── Dockerfile                        # Multi-stage container build for deployment
├── pom.xml                           # Maven dependencies (Spring Boot, WebSocket, H2, Postgres)
├── src/
│   ├── main/
│   │   ├── java/net/javaguides/springboot/
│   │   │   ├── config/
│   │   │   │   ├── PasswordEncoderConfig.java   # BCrypt bean definition
│   │   │   │   ├── SecurityConfiguration.java   # Filter chain, CSRF & authentication rules
│   │   │   │   ├── WebSocketConfig.java         # STOMP broker registration
│   │   │   │   └── WebSocketEventListener.java  # Session disconnect & presence listener
│   │   │   ├── model/
│   │   │   │   ├── ChatMessage.java             # JPA Entity & WebSocket payload DTO
│   │   │   │   ├── Role.java                    # Authority model
│   │   │   │   └── User.java                    # User credentials & profile entity
│   │   │   ├── repository/
│   │   │   │   ├── ChatMessageRepository.java   # Channel history JPA repository
│   │   │   │   └── UserRepository.java          # User lookup repository
│   │   │   ├── service/
│   │   │   │   ├── UserService.java             # Service interface
│   │   │   │   └── UserServiceImpl.java         # UserDetailsService implementation
│   │   │   └── web/
│   │   │       ├── ChatController.java          # WebSocket STOMP mappings & REST APIs
│   │   │       ├── MainController.java          # View navigation controller
│   │   │       └── UserRegistrationController.java # Registration controller
│   │   └── resources/
│   │       ├── application.properties           # Dual-mode local/cloud database config
│   │       └── templates/
│   │           ├── chat.html                    # Real-time chat workspace
│   │           ├── login.html                   # Sign-in page
│   │           └── registration.html            # Registration page
└── README.md
```

---

## License

This project is licensed under the MIT License.
