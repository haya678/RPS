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

## 3. Environment variables (Web Service → Environment)

| Key | Value |
|-----|--------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/DATABASE` |
| `SPRING_DATASOURCE_USERNAME` | from Postgres dashboard |
| `SPRING_DATASOURCE_PASSWORD` | from Postgres dashboard |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.postgresql.Driver` |
| `SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` |
| `TORN_API_MY_KEY` | your Torn API key |
| `ADMIN_SECRET_KEY` | long random string |

**Link database:** Web Service → **Environment** → **Add from database** (select your Postgres) — then fix `SPRING_DATASOURCE_URL` to **jdbc:postgresql://...** if needed (Render’s raw URL is not JDBC).

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
