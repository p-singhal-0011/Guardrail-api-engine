# Guardrail API Engine

A high-performance Spring Boot microservice that acts as a central API gateway and guardrail system for managing social media interactions. It uses Redis for real-time concurrency control and PostgreSQL as the persistent source of truth.

> The application is fully stateless. All runtime control data such as counters, cooldowns, and notification queues are stored in Redis.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Redis Key Design](#redis-key-design)
- [Thread Safety & Concurrency](#thread-safety--concurrency)
- [Quick Test Guide](#quick-test-guide)
- [Future Improvements](#future-improvements)
- [Author](#author)

---

## Overview

Guardrail API Engine is a backend system that simulates a social media platform with strict bot-control mechanisms. The system distinguishes between human users and bots, enforces interaction limits, tracks post virality, and delivers smart batched notifications — all without storing any state in application memory.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot 3.x |
| Database | PostgreSQL |
| Cache & Guardrails | Redis (Spring Data Redis) |
| Containerization | Docker & Docker Compose |
| API Testing | Postman |

---

## Architecture

```
Client (Postman)
       │
       ▼
Spring Boot API Layer
       │
       ├──► Redis (Guardrails, Counters, Cooldowns, Notifications)
       │         └── Acts as the Gatekeeper
       │
       └──► PostgreSQL (Posts, Comments, Users, Bots)
                 └── Acts as the Source of Truth
```

> Redis checks always run **before** any database write. If Redis rejects the request, the database is never touched.

---

## Features

### Phase 1 — Core API

- Create posts and comments
- Like a post
- Full JPA entity relationships (User, Bot, Post, Comment)

### Phase 2 — Redis Guardrails

| Guardrail | Rule | Redis Key |
|---|---|---|
| Horizontal Cap | Max 100 bot comments per post | `post:{id}:bot_count` |
| Vertical Cap | Max comment depth of 20 levels | Checked via `depth_level` |
| Cooldown Cap | Bot cannot interact with same user within 10 minutes | `cooldown:bot_{id}:human_{id}` |

### Phase 3 — Virality Engine

Each interaction increases the post's virality score stored in Redis:

| Action | Points |
|---|---|
| Like | +20 |
| Comment | +50 |

Redis Key: `post:{id}:virality_score`

### Phase 4 — Smart Notification System

- First bot interaction → immediate console log notification
- Subsequent interactions within 15 minutes → queued in Redis List
- A scheduled CRON job runs every 5 minutes and sends a summarized notification:

```
"Summarized Push Notification: Bot X and [N] others interacted with your posts."
```

---

## Project Structure

```
guardrail-api-engine/
│
├── src/main/java/com/priyansh/
│   │
│   ├── GuardrailApiEngineApplication.java
│   │
│   ├── config/
│   │   └── RedisConfig.java
│   │
│   ├── controller/
│   │   ├── PostController.java
│   │   └── CommentController.java
│   │
│   ├── entity/
│   │   ├── User.java
│   │   ├── Bot.java
│   │   ├── Post.java
│   │   └── Comment.java
│   │
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── PostRepository.java
│   │   └── CommentRepository.java
│   │
│   └── service/
│       ├── PostService.java
│       ├── CommentService.java
│       └── NotificationScheduler.java
│
├── src/main/resources/
│   └── application.properties
│
├── docker-compose.yml
├── postman_collection.json
└── README.md
```

---

## Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven

### Step 1 — Clone the Repository

```bash
git clone https://github.com/p-singhal-0011/Guardrail-api-engine.git
cd Guardrail-api-engine
```

### Step 2 — Start PostgreSQL and Redis

```bash
docker-compose up -d
```

This spins up:
- PostgreSQL on port `5432`
- Redis on port `6379`

### Step 3 — Run the Application

```bash
./mvnw spring-boot:run
```

The server starts at `http://localhost:8080`

---

### application.properties

```properties
spring.application.name=Guardrail-api-engine

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/GuardrailApi
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Kolkata

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### docker-compose.yml

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: GuardrailApi
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  redis:
    image: redis:7
    ports:
      - "6379:6379"
```

---

## API Endpoints

### Posts

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/posts` | Create a new post |
| POST | `/api/posts/{postId}/like` | Like a post |
| POST | `/api/posts/{postId}/comments` | Add a comment to a post |

---

### Sample Request — Create Post

```json
POST /api/posts
{
  "authorId": 1,
  "authorType": "HUMAN",
  "content": "This is my first post!"
}
```

### Sample Request — Add Comment

```json
POST /api/posts/1/comments
{
  "authorId": 2,
  "authorType": "BOT",
  "content": "Great post!",
  "depthLevel": 1
}
```

### Sample Response — Guardrail Rejected

```json
HTTP 429 Too Many Requests
{
  "error": "Bot comment limit reached for this post."
}
```

---

## Redis Key Design

| Key | Type | Purpose | TTL |
|---|---|---|---|
| `post:{id}:bot_count` | String (Counter) | Tracks bot comments per post | None |
| `post:{id}:virality_score` | String (Counter) | Tracks virality points | None |
| `cooldown:bot_{id}:human_{id}` | String | Prevents repeated bot interactions | 10 minutes |
| `notif:cooldown:{userId}` | String | Notification cooldown per user | 15 minutes |
| `user:{id}:pending_notifs` | List | Queued notifications for batching | None |

---

## Thread Safety & Concurrency

### The Problem

When 200 bots try to comment on the same post simultaneously, a naive check-then-write approach causes race conditions — resulting in 101+ comments instead of exactly 100.

### The Solution

Redis `INCR` is an **atomic operation**. No two threads can execute it at the same time on the same key.

```java
// Atomically increment the bot comment counter
Long newCount = redisTemplate.opsForValue().increment("post:" + postId + ":bot_count");

// If over the cap, roll back the increment and reject
if (newCount > 100) {
    redisTemplate.opsForValue().decrement("post:" + postId + ":bot_count");
    throw new ResponseStatusException(
        HttpStatus.TOO_MANY_REQUESTS,
        "Bot comment limit of 100 reached for this post."
    );
}

// Safe to proceed — write to PostgreSQL
```

### Why This Guarantees Exactly 100

- `INCR` is a single atomic command in Redis — it cannot be interrupted
- The 101st request always gets rejected before touching the database
- No `HashMap`, no `static` variables, no in-memory state — fully stateless

---

## Quick Test Guide

Follow these steps to verify all guardrails are working:

1. **Use predefined author IDs** (e.g., 1 as HUMAN, 2 as BOT) in the request payload
2. **Create a Post** via `POST /api/posts`
3. **Like the Post** via `POST /api/posts/{postId}/like`
4. **Add BOT comments** using Postman Collection Runner (200 iterations, 0ms delay)
5. Observe the results:
   - First 100 comments → saved successfully in PostgreSQL
   - Comments 101 to 200 → API returns `429 Too Many Requests`
   - Redis `bot_count` = exactly 100
   - Database contains exactly 100 bot comments

### Redis Verification Commands

```bash
# Check bot comment count for post 1
GET post:1:bot_count

# Check virality score for post 1
GET post:1:virality_score

# Check cooldown TTL between bot 2 and user 1
TTL cooldown:bot_2:human_1

# View pending notifications for user 1
LRANGE user:1:pending_notifs 0 -1
```

---

## Future Improvements

- ML-based bot detection using behavioral scoring
- WebSocket support for real-time notifications
- Post ranking API based on virality score
- Admin dashboard for monitoring guardrail metrics
- Distributed Redis cluster for production scaling

---

## Author

**Priyansh Singhal**
Backend Engineer
[GitHub](https://github.com/p-singhal-0011) • [LinkedIn](https://www.linkedin.com/in/priyanshsinghal1)

---

> Built with Spring Boot, Redis, and PostgreSQL.
