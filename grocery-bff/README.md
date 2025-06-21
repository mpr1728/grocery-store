# Grocery Store BFF Server

## Overview
This is the Backend-for-Frontend (BFF) Spring Boot server that forwards UI requests to individual microservices.

## Endpoints
- `/api/users/register` → forwards to User microservice on port 8082
- `/api/users/login` → forwards to User microservice
- `/api/products` (GET/POST) → forwards to Product microservice on port 8083

## How to Run

```bash
cd grocery-bff
./mvnw spring-boot:run
```

Make sure:
- Java 17+ is installed
- Product microservice is running on port 8083
- User microservice is running on port 8082
