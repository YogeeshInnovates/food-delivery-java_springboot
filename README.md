# 🍔 Online Food Delivery System — Spring Boot

A fully-featured, production-ready **REST API** for an online food delivery platform built with **Java 17 + Spring Boot**. Supports multi-role access (Customer, Owner, Admin), JWT-based authentication, Redis caching, and Docker-based deployment.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Database | PostgreSQL 15 |
| Caching | Redis |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| ORM | Spring Data JPA / Hibernate |
| Documentation | Swagger / OpenAPI (springdoc 3.0.2) |
| Build Tool | Maven 3.9.5 |
| Containerization | Docker + Docker Compose |
| Boilerplate | Lombok |

---

## ✨ Features

- ✅ **JWT Authentication** — Secure login with 24-hour token expiry
- ✅ **Role-Based Access Control (RBAC)** — `CUSTOMER`, `OWNER`, `ADMIN` roles with `@PreAuthorize`
- ✅ **Redis Caching** — Menu data cached for fast repeated reads
- ✅ **Soft Delete** — Menu items soft-deleted to preserve order history
- ✅ **Pagination** — All list endpoints support `Pageable`
- ✅ **Global Exception Handling** — Clean JSON error responses via `@ControllerAdvice`
- ✅ **Swagger UI** — Auto-generated interactive API documentation
- ✅ **Docker Compose** — One-command full stack startup (App + PostgreSQL + Redis)
- ✅ **Transactional Order Placement** — `@Transactional` ensures data consistency during checkout

---

## 🏗️ Architecture Overview

```
src/
├── config/security/
│   ├── SecurityConfig.java         # HTTP security & route protection
│   ├── JwtFilter.java              # Bearer token validation on every request
│   ├── JwtUtil.java                # Token generation, validation, claims
│   └── CustomUserDetailService.java # Loads user from DB for Spring Security
├── controller/
│   ├── AuthController.java         # /api/auth — Register & Login
│   ├── AdminController.java        # /api/admin — Admin-only operations
│   ├── RestaurantController.java   # /api/restaurants
│   ├── MenuController.java         # /api/restaurants/{id}/menu
│   ├── CartController.java         # /api/cart
│   └── OrderController.java        # /api/orders
├── entity/                         # JPA Entities (User, Restaurant, Order, etc.)
├── repository/                     # Spring Data JPA Repositories
├── service/                        # Business Logic Layer
├── dto/                            # Request/Response DTOs with @Valid
└── exception/                      # GlobalExceptionHandler + custom exceptions
```

---

## 📡 API Endpoints

### 🔐 Auth — `/api/auth`
| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | `/register` | Register as Customer | Public |
| POST | `/register_owner` | Register as Owner | Public |
| POST | `/login` | Login & get JWT token | Public |

### 👑 Admin — `/api/admin`
| Method | Endpoint | Description |
|---|---|---|
| GET | `/users` | Get all users (paginated) |
| PATCH | `/users/{id}/block` | Block/unblock a user |
| GET | `/restaurants` | View all restaurants |
| PATCH | `/restaurants/{id}/approve` | Approve a restaurant |
| GET | `/orders` | View all orders (paginated) |
| GET | `/dashboard` | Platform statistics |

### 🍽️ Restaurants — `/api/restaurants`
| Method | Endpoint | Description | Role |
|---|---|---|---|
| GET | `/` | List all restaurants (paginated) | Public |
| GET | `/filter?city=&cuisine=` | Filter restaurants | Public |
| GET | `/{id}` | Get restaurant details | Public |
| POST | `/` | Add new restaurant | OWNER |
| PUT | `/{id}` | Update restaurant | OWNER |
| PATCH | `/{id}/status` | Toggle open/closed status | OWNER |

### 🍕 Menu — `/api`
| Method | Endpoint | Description | Role |
|---|---|---|---|
| GET | `/restaurants/{id}/menu` | Get menu (paginated, filterable by category) | Public |
| POST | `/restaurants/{id}/menu` | Add menu item | OWNER |
| PUT | `/menu/{itemId}` | Update menu item | OWNER |
| PATCH | `/menu/{itemId}/availability` | Toggle item availability | OWNER |
| DELETE | `/menu/{itemId}` | Soft delete menu item | OWNER |

### 🛒 Cart — `/api/cart`
| Method | Endpoint | Description |
|---|---|---|
| GET | `/` | View cart items |
| POST | `/items` | Add item to cart |
| PUT | `/items/{itemId}?quantity=` | Update item quantity |
| DELETE | `/items/{itemId}` | Remove single item |
| DELETE | `/` | Clear entire cart |

### 📦 Orders — `/api/orders`
| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | `/` | Place an order | CUSTOMER |
| GET | `/` | My orders (paginated) | CUSTOMER |
| GET | `/{id}` | Order details | CUSTOMER/OWNER/ADMIN |
| PATCH | `/{id}/status` | Update order status | OWNER/ADMIN |
| POST | `/{id}/cancel` | Cancel an order | CUSTOMER |
| GET | `/summary` | Lifetime spend summary | CUSTOMER |

---

## ⚙️ Getting Started

### Prerequisites
- Docker & Docker Compose installed
- Java 17 (for local dev without Docker)

### Run with Docker Compose (Recommended)

```bash
# Clone the repo
git clone https://github.com/YogeeshInnovates/food-delivery-java_springboot.git
cd food-delivery-java_springboot

# Start everything (App + PostgreSQL + Redis)
docker-compose up --build
```

The app will be available at: `http://localhost:8080`

### Run Locally (Without Docker)

```bash
# Make sure PostgreSQL and Redis are running locally
# Update src/main/resources/application.properties with your DB credentials

./mvnw spring-boot:run
```

---

## 🔑 Environment Variables

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/food_delivery_db` |
| `SPRING_DATASOURCE_USERNAME` | DB username | — |
| `SPRING_DATASOURCE_PASSWORD` | DB password | — |
| `SPRING_REDIS_HOST` | Redis host | `localhost` |
| `JWT_SECRET` | Secret key for JWT signing | — |
| `JWT_EXPIRATION` | Token expiry in ms | `86400000` (24hrs) |

---

## 📖 API Documentation (Swagger)

Once the app is running, visit:

```
http://localhost:8080/swagger-ui.html
```

---

## 🗄️ Database Schema (Key Entities)

- **User** — id, name, email, password, role (`CUSTOMER` / `OWNER` / `ADMIN`), isActive
- **Restaurant** — id, name, city, cuisine, status, owner
- **MenuItems** — id, name, price, category, availability, restaurant
- **CartItem** — id, user, menuItem, quantity
- **Order** — id, customer, restaurant, items, status (`PLACED` → `DELIVERED`), totalAmount
- **OrderItem** — id, order, menuItem, quantity, price

---

## 🐳 Docker Services

```yaml
Services:
  app      → Spring Boot App (port 8080)
  db       → PostgreSQL 15-alpine (port 5432)
  redis    → Redis alpine (port 6379)

Network: food-delivery-network
```

---

## 📌 What's Missing / Future Improvements

- [ ] Payment gateway integration
- [ ] Email/SMS notifications on order updates
- [ ] Unit & Integration Tests
- [ ] CI/CD Pipeline (GitHub Actions)
- [ ] Rate limiting on Auth endpoints

---

## 👨‍💻 Author

**Yogeesh** — [GitHub](https://github.com/YogeeshInnovates)