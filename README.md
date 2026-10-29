# OmniCart — Inventory & Billing Management System

A college Industry Practice project for a small retail shop: manage products and stock, create bills, and review sales.
**Stack:** React (Vite) · Spring Boot 3 · Spring Security + JWT · JPA/Hibernate · MySQL.

> **Honest status:** all code in this ZIP was written in an environment with no internet, Maven, or MySQL, so it has **not been compiled or run yet**.
> Expect to fix small typos on the first `mvn spring-boot:run` / `npm run dev`. See "Known limitations" at the bottom.

## Features
| Role | What they can do |
|---|---|
| Public | Landing page (`/`), register (always creates **Staff**), login, one-click Demo Login |
| Admin | Dashboard (totals, revenue, low stock, recent sales), product & category CRUD, adjust stock, billing, all sales |
| Staff | Staff dashboard (today's bills), search inventory (read-only), billing, **own** sales only |

Main workflow: Admin adds products → Staff creates a bill → stock drops automatically → sales history and dashboard update.

## Setup (local)
1. **JDK 17+**, **Maven 3.9+**, **Node.js 18+**, **MySQL 8** running locally.
2. Database: the app creates schema `omnicart` automatically (`createDatabaseIfNotExist=true`). If your MySQL user cannot create databases run: `CREATE DATABASE omnicart;`
3. Configure the backend with environment variables (nothing is hardcoded; see `backend/src/main/resources/application.properties.example`):

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/omnicart?...` | JDBC URL |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | *(empty)* | MySQL password — **set this** |
| `JWT_SECRET` | demo value | 32+ characters; change outside demos |
| `CORS_ORIGINS` | `http://localhost:5173` | Allowed frontend origin(s) |
| `SEED_DEMO_DATA` | `true` | Create demo users/products on startup |

   Example (Windows PowerShell): `$env:DB_PASSWORD="yourpass"` · Linux/macOS: `export DB_PASSWORD=yourpass`
4. Start backend: `cd backend && mvn spring-boot:run` → http://localhost:8080 (tables and demo data are created on first start; re-running never duplicates them).
5. Start frontend: `cd frontend && npm install && npm run dev` → http://localhost:5173 (set `VITE_API_URL` in `frontend/.env` if the backend is elsewhere).
6. Tests: `cd backend && mvn test` (unit tests for billing logic; no database needed).

## Demo credentials
| Role | Email | Password |
|---|---|---|
| Admin | `admin@omnicart.local` | `Admin@123` |
| Staff | `staff@omnicart.local` | `Staff@123` |

The login page has **Demo Admin Login** and **Demo Staff Login** buttons. They call the real `POST /api/auth/login`, receive a JWT and load the user.

## Architecture
`React → Axios (JWT header) → Controller → Service → Repository → MySQL`

```
backend/src/main/java/com/omnicart/
  config/ (SecurityConfig, DataSeeder)   security/ (JwtService, JwtAuthFilter)
  controller/  service/  repository/  entity/  dto/  exception/  util/
frontend/src/  components/ layouts/ pages/ services/ context/ hooks/ utils/
```

**Entities:** `Category 1—* Product`, `User 1—* Sale`, `Sale 1—* SaleItem`, `SaleItem *—1 Product`. `SaleItem` stores `productNameSnapshot` and `unitPrice` so old invoices never change. Money is `BigDecimal`.

## API overview
| Area | Endpoints | Access |
|---|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` | public / logged in |
| Categories | `GET /api/categories` · `POST`, `PUT /{id}`, `DELETE /{id}` | any user / **Admin** |
| Products | `GET /api/products?search=&categoryId=&page=&size=`, `GET /{id}` · `POST`, `PUT`, `DELETE` | any user / **Admin** |
| Inventory | `GET /api/inventory`, `GET /api/inventory/low-stock` · `PATCH /api/inventory/{id}/stock` | any user / **Admin** for PATCH |
| Sales | `POST /api/sales`, `GET /api/sales?invoice=&from=&to=`, `GET /api/sales/{id}` | logged in (staff see only their own) |
| Dashboard | `GET /api/dashboard/summary`, `/low-stock` (Admin) · `/staff-summary`, `/recent-sales` | see code |

Errors are JSON: `{ timestamp, status, message, fieldErrors? }`.

## Concepts (for the viva)
- **MVC / layers:** controllers handle HTTP, services hold business rules, repositories talk to the DB.
- **Dependency injection:** Spring creates beans and injects them through constructors (`@RequiredArgsConstructor`).
- **JPA/Hibernate:** entities map to tables; `ddl-auto=update` generates the schema for the demo.
- **JWT:** login returns a signed token (`JwtService`); `JwtAuthFilter` validates it on each request; `SecurityConfig` maps URLs to roles. Frontend stores the token in `localStorage` (acceptable for a local demo; production would prefer httpOnly cookies).
- **Passwords:** BCrypt hashes only; DTOs never expose `passwordHash`.
- **DTOs + Bean Validation:** `Dtos.java` request/response records with `@NotBlank`, `@Email`, `@Positive`, etc.
- **Exception handling:** `GlobalExceptionHandler` (`@RestControllerAdvice`) maps errors to clean responses.
- **Transactions:** `BillingService.createSale` is `@Transactional`. Stock is deducted with one atomic SQL update (`... where stock >= qty`), so overselling is prevented and any failure rolls back the whole bill. Prices come from the DB; the browser sends only product ids and quantities. Duplicate cart lines are merged.

## 4–6 minute demo script
1. Landing page → explain features. 2. Login → **Demo Admin Login**. 3. Dashboard: totals and low-stock list.
4. Products → add/edit a product. 5. Log out → **Demo Staff Login**. 6. Billing → search, add items, **Generate bill** → invoice → Print.
7. Inventory: stock decreased. 8. Sales History → open the invoice. 9. Explain layers + JWT.

## Known limitations
- Not yet compiled or run (see top). Discount/tax, refunds, PDF export, charts and user-management screens were intentionally left out.
- Only two unit tests (billing). No frontend tests.
- Public registration confirm-password is checked in the browser only; the API validates the password rules.
