# Deploy on Render (Docker + PostgreSQL)

## 1. PostgreSQL database

1. Render Dashboard → **New** → **PostgreSQL** (Free).
2. Wait until status is **Available**.
3. Open the database → **Connections** → copy **Internal Database URL**  
   Example: `postgresql://user:pass@dpg-xxxx-a/rps_battle`

Convert to JDBC for Spring (same host/user/pass/db):

`jdbc:postgresql://dpg-xxxx-a:5432/rps_battle`

(Use hostname from the URL; port is usually `5432`.)

## 2. Web service (Docker)

- **Language:** Docker  
- **Root Directory:** *(empty)*  
- **Dockerfile Path:** `Dockerfile`

## 3. Link Postgres to the web service (easiest)

1. Web Service → **Environment** → **Add environment variable** → **Add from database** → pick your Postgres.
2. Render adds `DATABASE_URL` automatically (`postgres://...`). The app converts that to JDBC — **do not** paste `postgres://` into `SPRING_DATASOURCE_URL`.

Still add these manually:

| Key | Value |
|-----|--------|
| `TORN_API_MY_KEY` | Torn API key for house account (deposits + admin panel) |
| `ADMIN_SECRET_KEY` | optional legacy admin password |

**Optional** (only if not using linked `DATABASE_URL`):

| Key | Value |
|-----|--------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/DB?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | from Postgres |
| `SPRING_DATASOURCE_PASSWORD` | from Postgres |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.postgresql.Driver` |
| `SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` |

**Wrong (causes crash):**

- `SPRING_DATASOURCE_URL=postgres://user:pass@host/db` (not JDBC)
- `SPRING_DATASOURCE_URL=jdbc:postgresql://user:pass@host/db` **without** `SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver` (MySQL driver error)

**Correct manual JDBC (credentials separate):**

```
SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-xxxx-a:5432/dbname?sslmode=require
SPRING_DATASOURCE_USERNAME=your_user
SPRING_DATASOURCE_PASSWORD=your_password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
```

If you linked `DATABASE_URL` from Render, **delete** `SPRING_DATASOURCE_URL` entirely.

## 4. Push and deploy

```bash
git add .
git commit -m "Render: Postgres env config"
git push origin main
```

## 5. If it still exits with status 1

Open **Logs** and scroll to the **red** lines at the bottom. Common messages:

- `Communications link failure` / `Connection refused` → database env vars wrong or DB not linked  
- `password authentication failed` → wrong `SPRING_DATASOURCE_PASSWORD`  
- `Failed to configure a DataSource` → missing env vars  

Paste those lines when asking for help.
