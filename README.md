# charlie-quizlet-be

Backend cho [charlie-quizlet](../charlie-quizlet) — Spring Boot 4.1 + Java 21 + PostgreSQL.

## Yêu cầu

- Java 21 (cài qua SDKMAN: `sdk install java 21.0.12+1.1-tem`)
- PostgreSQL, với database/user:
  ```bash
  sudo -u postgres psql -c "CREATE USER quizlet WITH PASSWORD 'quizlet';"
  sudo -u postgres psql -c "CREATE DATABASE charlie_quizlet OWNER quizlet;"
  ```

## Chạy

```bash
./mvnw spring-boot:run
```

- API: http://localhost:8080/api/ping
- Health: http://localhost:8080/actuator/health

Cấu hình qua biến môi trường (mặc định trong `application.yml`):
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`,
`JWT_SECRET` (>= 32 byte, **bắt buộc đặt riêng khi deploy**), `JWT_EXPIRATION` (ISO-8601, mặc định `PT24H`).

## Auth API

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/auth/register` | — | `{email, password, fullName}` → 201 + token (role mặc định `STUDENT`) |
| POST | `/api/auth/login` | — | `{email, password}` → token |
| GET | `/api/auth/me` | Bearer | Thông tin user hiện tại |

Các API khác gửi header `Authorization: Bearer <accessToken>`. Lỗi trả về dạng
[Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`detail`, và `errors` cho lỗi validate).

## Cấu trúc

```
src/main/java/com/charlie/quizlet/
├── config/    # Security, CORS
├── common/    # tiện ích dùng chung
└── <feature>/ # auth, user, question, exam, flashcard...
src/main/resources/db/migration/   # Flyway migrations (V1__..., V2__...)
```
