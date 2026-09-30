# Military Asset Management System (MAMS)

A full-stack military logistics and custody management platform for tracking assets (weapons, ammunition, vehicles), managing inter-base transfers, personnel assignments, and expenditures.

## Project Structure

```text
Military_Asset_Management_System/
├── backend/                  # Java 17 + Spring Boot 3.3.4 (REST API & DB Migrations)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/mams/   # Controllers, Entities, Repositories, Services, Security, AOP
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-prod.yml
│       │       └── db/migration/  # Flyway Migrations (V1 Schema, V2 Seed Data)
│       └── test/
│
├── frontend/                 # React 19 + Vite + Tailwind CSS + Lucide Icons
│   ├── package.json
│   ├── vite.config.js        # Port 3000, proxies /api -> http://localhost:8080
│   ├── tailwind.config.js    # Tactical military dark theme
│   ├── index.html
│   └── src/
│       ├── api/              # Axios instance with JWT interceptor & 401 redirect
│       ├── context/          # AuthContext (JWT, user state, role checks)
│       ├── components/       # ProtectedRoute, Layout with tactical sidebar
│       └── pages/            # Login, Dashboard, Purchases, Transfers, Assignments, Users
│
└── .gitignore
```

---

## Quick Start

### 1. Run the Backend
Ensure MySQL is running on `localhost:3306`:
```powershell
cd backend
mvn spring-boot:run
```
- API Base URL: `http://localhost:8080`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui/index.html`

### 2. Run the Frontend
In a separate terminal:
```powershell
cd frontend
npm install
npm run dev
```
- Frontend UI: `http://localhost:3000`

---

## Default Credentials
- **Username**: `admin`
- **Password**: `Admin@123`
