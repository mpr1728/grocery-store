## Grocery Store Microservices Project - README

This project implements a modern online grocery store using:

- ReactJS for UI
- Java Spring Boot microservices (Product, User, Order)
- Spring Boot BFF API Gateway
- MySQL for persistence
- Docker and GitHub Actions for CI/CD

---

## Project Structure

```
root/
├── grocery-ui/                # React UI with admin and customer views
├── java-microservices/
│   ├── product-service/       # Microservice for managing groceries
│   ├── user-service/          # Microservice for login/registration
│   ├── order-service/         # Microservice for purchases and points
│   └── bff/                   # BFF gateway for the frontend
├── docker-compose.local.yml  # For local dev
├── .github/workflows/        # GitHub Actions pipelines
├── postman-collection/       # Postman collection for testing APIs
└── README.md
```

---

## 🧪 Local Development Setup

### Prerequisites:

- Docker & Docker Compose
- Java 17
- Node.js 18

### Run Locally (Monorepo):

```bash
# Start all microservices and frontend
cd grocery-store
docker-compose -f docker-compose.local.yml up --build
```

### Access Services:

- Frontend: [http://localhost](http://localhost)
- BFF: [http://localhost:8081](http://localhost:8081)
- Product: [http://localhost:8082](http://localhost:8082)
- User: [http://localhost:8083](http://localhost:8083)
- Order: [http://localhost:8084](http://localhost:8084)

### Run Each Java Service Individually:

```bash
cd java-microservices/product-service
./mvnw spring-boot:run

cd java-microservices/user-service
./mvnw spring-boot:run

cd java-microservices/order-service
./mvnw spring-boot:run

cd java-microservices/bff
./mvnw spring-boot:run
```

### Run Frontend Locally:

```bash
cd grocery-ui
npm install
npm run dev
```

---

## 🗄️ Database Schema (MySQL)

### product-service

```sql
CREATE TABLE products (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100),
  category VARCHAR(50),
  price DECIMAL(10, 2),
  stock INT
);
```

### user-service

```sql
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20),
  membership_plan VARCHAR(50)
);
```

### order-service

```sql
CREATE TABLE orders (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  product_id INT,
  quantity INT,
  total_price DECIMAL(10, 2),
  order_date DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🔄 Database Migrations (Flyway)

Each microservice contains a Flyway configuration that automatically applies SQL migrations at startup:

- Place migration files under `src/main/resources/db/migration`
- Naming format: `V1__initial.sql`, `V2__add_indexes.sql`, etc.

Example for product-service:

```sql
-- V1__initial.sql
CREATE TABLE products (...);
```

---

## 🌱 Seed Data for Local Dev

Each service includes a `data.sql` file under `src/main/resources` to load sample records when running locally.

Example for user-service:

```sql
INSERT INTO users (email, password_hash, role, membership_plan) VALUES
('test@example.com', '$2a$10$exampleHashedPwd', 'CUSTOMER', 'BASIC');
```

---

## 🚀 Deployment via GitHub Actions

- CI/CD pipeline builds frontend, backend, and BFF
- Pushes Docker images to Amazon ECR
- SSH deploys to EC2 with `docker-compose.yml`

Secrets Required:

- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
- `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`
- `ECR_REGISTRY`

---

## 🔒 Security

- Passwords hashed using BCrypt
- JWT-based authentication
- Email validation and password strength indicators in UI

---

## 🧪 End-to-End Test (First-Time Setup)

1. Start the full system using Docker:

```bash
cd grocery-store
docker-compose -f docker-compose.local.yml up --build
```

2. Open the app:

```bash
http://localhost
```

3. In the UI:

- Register a new user
- Log in and browse groceries
- Add items to cart and place an order

4. Admin view:

- Go to `/admin`
- Add new groceries by category

5. API test (optional):

- Import the Postman collection: `postman-collection/GroceryStoreAPIs.postman_collection.json`
- Run requests against: `http://localhost:{port}`

---

## 🖼️ Screenshots and Diagrams

### App Architecture Diagram



### UI - Customer Portal



### UI - Admin Portal



---

## 📬 Support or Contribution

Open a GitHub issue or pull request to collaborate!

---

## 📮 Postman API Collection

A sample Postman collection is available under:

```
/postman-collection/GroceryStoreAPIs.postman_collection.json
```

Includes test cases for:

- Register/Login (user-service)
- Product CRUD (product-service)
- Order placement and points (order-service)
- JWT token handling and BFF routing

