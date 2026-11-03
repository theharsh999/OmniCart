# OmniCart — Modern Retail Inventory, Point of Sale (POS) & Analytics System

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5.3-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-26%20Passing-brightgreen?style=for-the-badge)](https://github.com/theharsh999/OmniCart)

OmniCart is a full-stack, enterprise-grade retail inventory, high-speed point of sale (POS), financial analytics, and intelligent assistant platform engineered for modern counter operations in retail outlets, convenience stores, and supermarkets.

---

## 🌟 Key Highlights & Capabilities

- ⚡ **High-Speed Point of Sale (POS):** Fast SKU/barcode search, debounced product lookups, live cart calculations, atomic stock deduction, and immediate printable invoice generation (`INV-YYYYMMDD-XXXXX`).
- 💳 **Flexible Multi-Mode Payment:** Seamless support for Cash (with auto-calculated customer change), simulated Dynamic UPI QR generation, and Card terminal simulation.
- 📊 **Visual Sales Analytics Dashboard:** Zero-dependency, responsive SVG cubic-bezier revenue trend charts, KPI metrics (Gross Revenue, Order Count, Average Order Value, Items Sold), previous-period growth comparisons, top-selling products by quantity and revenue, payment method breakdown, category revenue shares, and cashier performance league tables.
- 🤖 **OmniCart AI Assistant / Rule-Based Chatbot:** Embedded floating assistant available across the dashboard that parses natural-language prompts to query real-time stock levels, check out-of-stock items, retrieve today's revenue, list bestsellers, and navigate the application.
- 🛡️ **Atomic Concurrency & Anti-Overselling:** Server-authoritative pricing (client sends only product IDs and quantities) paired with atomic SQL conditional decrements (`WHERE stock_quantity >= qty`) to prevent race conditions across parallel counters.
- 📜 **Historical Invoice Immutability:** Line items snapshot product names, SKUs, and unit prices at transaction time, guaranteeing past invoices remain legally accurate even if catalog prices change.
- 🔐 **Role-Based Access Control (RBAC):** Strict segregation between **Store Owner (`ADMIN`)** and **Counter Cashier (`STAFF`)** enforced through stateless Spring Security JWT authentication and React route guards.
- 👥 **Cashier Account Management:** Owner portal to create, view, search, and monitor cashier staff members and track their individual sales performance.
- 📦 **Automated Seed Data:** Built-in seeders populate the database with realistic products across multiple categories, default roles, and 30 days of historical sales data for immediate demo evaluation.

---

## 👥 User Roles & Permissions

| Role | Access Scope | Key Capabilities |
|---|---|---|
| **Store Owner (`ADMIN`)** | Full System Access | • Full Product & Category CRUD<br>• Inventory Stock Adjustments & Low-Stock Alerts<br>• Real-time Executive Dashboard & Financial KPIs<br>• Advanced Sales Analytics & Trend Visualizations<br>• Cashier Staff Management & Performance Tracking<br>• Sale Auditing & Exclusive Invoice Voiding with Auto-Restock<br>• AI Assistant & POS Billing |
| **Counter Cashier (`STAFF`)** | POS & Personal Sales | • Fast Point of Sale (POS) Counter Billing<br>• Multi-Mode Payment Processing (Cash / UPI / Card)<br>• Printable Customer Receipt / Invoice Generation<br>• Read-Only Catalog & Inventory Lookup<br>• Shift Summary Dashboard (Today's Sales & Orders)<br>• Personal Sales History Review<br>• AI Assistant for Stock Inquiries |

---

## 🛠️ Technology Stack

### Backend
- **Language & Runtime:** Java 17+ / Java 21
- **Framework:** Spring Boot 3.3.x (Spring Web, Spring Data JPA, Spring Security, Spring Validation)
- **Security:** Stateless JWT (`io.jsonwebtoken:jjwt`), BCrypt password hashing
- **Persistence:** Hibernate 6 / JPA with MySQL 8 Driver
- **Testing:** JUnit 5, Mockito, AssertJ

### Frontend
- **Framework:** React 18 (Single Page Application)
- **Tooling:** Vite 5 (ultra-fast build and HMR)
- **Routing:** React Router v6
- **HTTP Client:** Axios (configured with JWT interceptors)
- **Icons:** Lucide React
- **Styling:** Modular modern CSS with design tokens, responsive CSS grids, and zero external UI bloat

---

## 📁 Repository Structure

```
OmniCart/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/omnicart/
│   │   │   │   ├── assistant/     # AI natural language intent detector & store chatbot
│   │   │   │   ├── config/        # SecurityConfig, DataSeeder, DemoSalesSeeder, WebConfig
│   │   │   │   ├── controller/    # REST API endpoints (Auth, Products, Sales, Analytics, etc.)
│   │   │   │   ├── dto/           # Java records for request/response payloads & validation
│   │   │   │   ├── entity/        # JPA Entities (User, Product, Category, Sale, SaleItem)
│   │   │   │   ├── exception/     # GlobalExceptionHandler & custom exceptions
│   │   │   │   ├── repository/    # Spring Data JPA repositories with custom JPQL queries
│   │   │   │   ├── security/      # JwtService, JwtAuthFilter, UserDetailsServiceImpl
│   │   │   │   ├── service/       # Business logic (Billing, Analytics, Inventory, Auth)
│   │   │   │   └── util/          # Barcode, SKU, and Invoice generator utilities
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── application.properties.example
│   │   └── test/                  # 26 automated unit tests for billing, analytics & assistant
│   └── pom.xml                    # Maven project configuration & dependencies
├── frontend/
│   ├── src/
│   │   ├── components/            # Chatbot, RevenueTrendChart, ProtectedRoute, Modals
│   │   ├── context/               # AuthContext (JWT management & session state)
│   │   ├── layouts/               # DashboardLayout with dynamic role-based navigation
│   │   ├── pages/                 # AdminDashboard, StaffDashboard, POS Billing, Analytics, etc.
│   │   ├── services/              # Axios service layer (api, auth, products, sales, analytics, assistant)
│   │   ├── App.jsx                # Main route definitions & guards
│   │   ├── main.jsx               # Application entry point
│   │   └── index.css              # Global design system, utility classes & responsive styles
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── OmniCart_Complete_Project_Documentation.md  # Detailed 1100+ line technical architecture & viva report
└── README.md                      # Project documentation
```

---

## 🚀 Getting Started

### Prerequisites
Make sure the following tools are installed on your machine:
- **Java Development Kit (JDK):** Version 17 or higher
- **Apache Maven:** Version 3.8+
- **Node.js:** Version 18+ and `npm`
- **MySQL Server:** Version 8.0+

---

### 1. Database Setup

Create the database schema in MySQL (or let Spring Boot create it automatically on boot):

```sql
CREATE DATABASE IF NOT EXISTS omnicart CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

### 2. Backend Setup & Run

Navigate to the `backend` directory:

```bash
cd backend
```

Configure your MySQL credentials in `src/main/resources/application.properties` or provide them via environment variables:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/omnicart?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password
```

Run the backend application:

```bash
mvn clean spring-boot:run
```

The Spring Boot server will start at `http://localhost:8080`.
> **Note:** On first startup, the database tables, default categories, demo products, default credentials, and 30 days of realistic sales data are automatically initialized.

---

### 3. Frontend Setup & Run

In a new terminal window, navigate to the `frontend` directory:

```bash
cd frontend
```

Install the dependencies:

```bash
npm install
```

Start the Vite development server:

```bash
npm run dev
```

Open your browser and navigate to `http://localhost:5173`.

---

## 🔑 Demo Credentials

OmniCart includes pre-seeded accounts for instant evaluation, plus convenient **One-Click Demo Login** buttons on the login page:

| Role | Email | Password | Pre-seeded Permissions |
|---|---|---|---|
| **Store Owner** | `admin@omnicart.local` | `Admin@123` | Full Admin & Management Access |
| **Counter Cashier** | `staff@omnicart.local` | `Staff@123` | POS Billing & Shift Operations |

---

## 📡 REST API Reference Summary

### Authentication (`/api/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register new staff cashier account |
| `POST` | `/api/auth/login` | Public | Authenticate user & return signed JWT |
| `GET` | `/api/auth/me` | Authenticated | Retrieve current authenticated user profile |

### Point of Sale & Sales Management (`/api/sales`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/sales` | Authenticated | Atomic checkout & invoice generation |
| `GET` | `/api/sales` | Authenticated | List sales (filtered by date/invoice; staff scoped to self) |
| `GET` | `/api/sales/{id}` | Authenticated | Get detailed sale receipt with line items |
| `DELETE`| `/api/sales/{id}` | Owner (`ADMIN`) | Void sale and atomically restock inventory |

### Analytics & Reporting (`/api/analytics`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/analytics/dashboard` | Owner (`ADMIN`) | Aggregated financial metrics, daily trend, category/payment breakdown |
| `GET` | `/api/analytics/summary` | Owner (`ADMIN`) | Revenue, order count, AOV, item volume with growth percentages |
| `GET` | `/api/analytics/revenue-trend` | Owner (`ADMIN`) | Day-by-day revenue, order volume, and unit sales |
| `GET` | `/api/analytics/top-products` | Owner (`ADMIN`) | Top products ranked by quantity sold and gross revenue |
| `GET` | `/api/analytics/payment-breakdown` | Owner (`ADMIN`) | Revenue and transaction distribution across payment methods |
| `GET` | `/api/analytics/cashier-performance` | Owner (`ADMIN`) | Performance metrics for each cashier (bills, revenue, AOV) |

### AI Store Assistant (`/api/assistant`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/assistant/chat` | Authenticated | Natural language query interface for stock, revenue, and store help |

### Products & Inventory (`/api/products`, `/api/categories`, `/api/inventory`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/products` | Authenticated | Paginated and filtered product catalog search |
| `POST` | `/api/products` | Owner (`ADMIN`) | Create new product with SKU and pricing |
| `PUT` | `/api/products/{id}` | Owner (`ADMIN`) | Update product details |
| `DELETE`| `/api/products/{id}` | Owner (`ADMIN`) | Remove product from catalog |
| `GET` | `/api/categories` | Authenticated | List all product categories |
| `POST` | `/api/categories` | Owner (`ADMIN`) | Create new category |
| `GET` | `/api/inventory` | Authenticated | View current stock levels |
| `GET` | `/api/inventory/low-stock` | Authenticated | List items below reorder threshold |
| `PATCH`| `/api/inventory/{id}/stock` | Owner (`ADMIN`) | Adjust stock count with audit reason |

### Cashier Management (`/api/cashiers`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/cashiers` | Owner (`ADMIN`) | List all registered cashier staff members |
| `POST` | `/api/cashiers` | Owner (`ADMIN`) | Create new cashier account with employee ID |
| `GET` | `/api/cashiers/{id}` | Owner (`ADMIN`) | Retrieve cashier details |

---

## 🧪 Testing & Verification

The backend includes a comprehensive suite of unit tests covering the critical business logic:

```bash
cd backend
mvn test
```

### Test Coverage Highlights
- **`BillingServiceTest`**: Validates atomic checkout, stock deductions, empty cart rejections, insufficient stock rollbacks, and invoice numbering.
- **`AnalyticsServiceTest`**: Validates revenue aggregations, period-over-period percentage growth calculations, category breakdowns, and cashier KPI metrics.
- **`AssistantServiceTest` & `IntentDetectorTest`**: Validates natural language intent classification (stock inquiry, out-of-stock items, revenue metrics, bestsellers) and database-backed dynamic responses.

**Frontend Verification:**
```bash
cd frontend
npm run build
```
Builds production-ready assets cleanly with 0 errors via Vite.

---

## 💡 Key Architectural Concepts

1. **Atomic Concurrency:** Stock decrement is executed using an atomic database query (`UPDATE products SET stock_quantity = stock_quantity - :qty WHERE id = :id AND stock_quantity >= :qty`). If any item in a multi-item cart fails the condition, the entire transaction rolls back via `@Transactional`.
2. **Server-Authoritative Pricing:** The frontend cart sends only `{ productId, quantity }`. All prices are fetched directly from the database to prevent client-side price tampering.
3. **Data Integrity & Historical Accuracy:** `SaleItem` stores a snapshot of the product name and price at checkout time (`productNameSnapshot`, `unitPrice`). Future catalog updates never distort historical sales or financial reports.
4. **Stateless Security:** JWT authentication with expiration eliminates server session state, supporting horizontal scaling.
5. **Zero External Chart Bloat:** The revenue trend chart is implemented with pure SVG `<path d="M...">` cubic bezier mathematics, ensuring instant loading, zero rendering lag, and no heavy third-party bundle weight.

---

## 📄 License & Academic Attribution

This project is developed as an Industry Practice (IP) Capstone Project.
Feel free to clone, star, and utilize this project for educational and demonstration purposes.
