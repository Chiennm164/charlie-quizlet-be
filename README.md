# charlie-quizlet-be

Backend cho [charlie-quizlet](../charlie-quizlet) — Spring Boot 4.1 + Java 21 + PostgreSQL.

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [CQ_DEV_GUIDE.md](CQ_DEV_GUIDE.md) | Hướng dẫn kèm code mẫu: cấu hình, thêm API, lỗi & mã lỗi, migration, thời gian, test |
| [CQ_CODING_RULES.md](CQ_CODING_RULES.md) | Quy định bắt buộc khi viết code |
| README (file này) | Chạy dự án, biến môi trường, bảng API, model lỗi |

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
- Swagger UI (tài liệu + gọi thử API): http://localhost:8080/swagger-ui.html
- OpenAPI JSON (sinh type cho FE): http://localhost:8080/v3/api-docs

Cấu hình qua biến môi trường (mặc định trong `application.yml`, đọc trong code qua `config/AppProperties` và `auth/JwtProperties`):
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`,
`JWT_SECRET` (>= 32 byte, **bắt buộc đặt riêng khi deploy**), `JWT_EXPIRATION` (thời hạn access token, ISO-8601, mặc định `PT15M`),
`JWT_REFRESH_EXPIRATION` (thời hạn refresh token, mặc định `P30D`, tính lại từ đầu mỗi lần làm mới),
`SWAGGER_ENABLED` (mặc định `true`; đặt `false` khi deploy production),
`FRONTEND_URL` (mặc định `http://localhost:4200`, dùng để tạo link đặt lại mật khẩu),
`PASSWORD_RESET_TOKEN_TTL` (mặc định `PT30M`),
`ERROR_CODES_CACHE_TTL` (mặc định `PT5M` — thời gian cache bảng `error_codes`),
`ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_FULL_NAME` (tài khoản Admin tạo lúc khởi động nếu email chưa có; mặc định dev
`admin@charlie-quizlet.local` / `admin12345` — **bắt buộc đổi mật khẩu khi deploy**, hoặc để `ADMIN_EMAIL` rỗng để không tạo).

## Auth API

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/auth/register` | — | `{email, password, fullName}` → 201 + token. Luôn tạo tài khoản `STUDENT` (gửi kèm `role` cũng bị bỏ qua) |
| POST | `/api/auth/login` | — | `{email, password}` → token |
| POST | `/api/auth/refresh` | — | `{refreshToken}` → cặp token mới. Refresh token dùng 1 lần (token cũ bị thu hồi) |
| POST | `/api/auth/logout` | — | `{refreshToken}` → 204. Thu hồi refresh token của phiên; token sai / đã thu hồi cũng 204 |
| POST | `/api/auth/forgot-password` | — | `{email}` → 204. Luôn 204 dù email có tồn tại hay không (chống dò email) |
| POST | `/api/auth/reset-password` | — | `{token, newPassword}` → 204. Token dùng 1 lần, hết hạn sau `PASSWORD_RESET_TOKEN_TTL`; đăng xuất mọi thiết bị |
| GET | `/api/auth/me` | Bearer | Thông tin user hiện tại |
| PATCH | `/api/auth/me` | Bearer | `{fullName}` → user sau khi sửa |
| POST | `/api/auth/change-password` | Bearer | `{currentPassword, newPassword}` → token mới. Đăng xuất mọi thiết bị khác; sai mật khẩu hiện tại → 400 `AUTH_CURRENT_PASSWORD_INCORRECT` |

"Token" trả về từ register / login / refresh / change-password: `{accessToken, tokenType, expiresIn, refreshToken, user}` (`expiresIn` tính bằng giây).
Các API khác gửi header `Authorization: Bearer <accessToken>`.

## Role

Chỉ 2 role: `STUDENT` (tự đăng ký, làm bài) và `ADMIN` (tạo lúc khởi động từ `ADMIN_EMAIL` / `ADMIN_PASSWORD`; soạn bộ đề,
quản lý chủ đề). Không có tài khoản chờ duyệt. Mọi thứ dưới `/api/admin/**` chỉ `ADMIN` (role khác → 403 `COMMON_FORBIDDEN`).

## Topic API (chủ đề)

Danh sách phẳng, tên không trùng (không phân biệt hoa thường). Mỗi bộ đề thuộc đúng 1 chủ đề.

| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/topics` | Mọi chủ đề A → Z, kèm `quizCount` (ai đăng nhập cũng xem được) |
| POST | `/api/admin/topics` | `{name}` → 201. Trùng tên → 409 `TOPIC_NAME_TAKEN` |
| PUT | `/api/admin/topics/{id}` | Đổi tên |
| DELETE | `/api/admin/topics/{id}` | 204. Chủ đề còn bộ đề → 409 `TOPIC_IN_USE` |

## Quiz API (bộ đề trắc nghiệm)

**Tạo / sửa / xoá chỉ `ADMIN`** (mọi Admin sửa được mọi đề). `PUBLISHED` = đã duyệt, học sinh thấy; `DRAFT` chỉ Admin thấy
(người khác 404 `QUIZ_NOT_FOUND`).

| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/quizzes/by-topic?limit=8` | **Home**: chủ đề có đề đã xuất bản (A → Z), mỗi chủ đề `{topic, totalQuizzes, quizzes}` — tối đa `limit` (1–20) đề mới nhất |
| GET | `/api/quizzes?topicId=&q=&sort=&page=&size=` | Đề đã xuất bản, lọc theo chủ đề. `sort`: `RECENT` \| `NEWEST` \| `TITLE` |
| GET | `/api/admin/quizzes?status=&topicId=&q=&sort=&page=&size=` | Mọi đề, cả nháp (ADMIN) |
| POST | `/api/quizzes` | `{topicId, title, description?, timeLimitMinutes?, status, questions: [{content, explanation?, options: [{content, correct}]}]}` → 201 |
| GET | `/api/quizzes/{id}` | Bộ đề (kèm `topic`). `questions` (kèm đáp án đúng) **chỉ có khi `canEdit`** (Admin); học sinh nhận `questions: null` |
| PUT | `/api/quizzes/{id}` | Gửi toàn bộ câu hỏi theo thứ tự mới; câu / đáp án có `id` được giữ, không có `id` là mới, cũ không gửi bị xoá |
| DELETE | `/api/quizzes/{id}` | 204, xoá cả câu hỏi |

- `topicId` không tồn tại → 404 `TOPIC_NOT_FOUND`.
- `status`: `DRAFT` (lưu được cả khi chưa có câu) \| `PUBLISHED` (cần ≥ 1 câu, không thì 400 `QUIZ_EMPTY`). `publishedAt` giữ lần xuất bản đầu, về nháp thì xoá.
- `timeLimitMinutes`: 1–300, bỏ trống = không giới hạn. Tối đa 200 câu; mỗi câu 2–6 đáp án.
- Mỗi câu **đúng 1** đáp án `correct` (400 `QUIZ_CORRECT_OPTION_REQUIRED`); đáp án trùng trong 1 câu → 400 `QUIZ_DUPLICATE_OPTION`.
- `id` câu / đáp án không thuộc đề (hoặc gửi 2 lần) → 400 `QUIZ_ITEM_NOT_FOUND`.

**Refresh token:**

- Access token (JWT) ngắn hạn (`JWT_EXPIRATION`) vì không thu hồi được trước hạn. Hết hạn thì gọi `/api/auth/refresh` lấy cặp mới.
- Refresh token là chuỗi ngẫu nhiên, DB chỉ lưu hash SHA-256 (bảng `refresh_tokens`). Mỗi lần làm mới, token cũ bị thu hồi và
  thay bằng token mới cùng `family_id` (1 family = 1 phiên đăng nhập).
- Token đã thu hồi mà bị gửi lại quá 10 giây sau khi bị thay → nghi bị lộ, thu hồi cả phiên (người dùng phải đăng nhập lại).
  Gửi lại trong 10 giây (2 tab làm mới cùng lúc) chỉ bị từ chối.
- Đặt lại mật khẩu thu hồi mọi phiên của user. Đổi mật khẩu (đang đăng nhập) cũng vậy, nhưng cấp phiên mới cho thiết bị đang đổi.

> Chưa có SMTP: link đặt lại mật khẩu được **ghi ra log** (`Password reset link for ...`).
> Khi có mail server, thay `LoggingPasswordResetNotifier` bằng một implementation gửi mail của `PasswordResetNotifier`.

## Lỗi API

Mọi lỗi (nghiệp vụ, validate, lỗi Spring MVC, 401/403 của Spring Security, lỗi không lường trước) đều trả
[Problem Details (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457) kèm 3 trường cho FE:

```json
{
  "status": 401,
  "title": "Unauthorized",
  "detail": "Email hoặc mật khẩu không đúng",
  "errorCode": "AUTH_INVALID_CREDENTIALS",
  "errorMessage": "Email hoặc mật khẩu không đúng",
  "errorDescription": "Kiểm tra lại thông tin đăng nhập hoặc dùng chức năng \"Quên mật khẩu\".",
  "errors": { "email": "..." }
}
```

- `errorMessage` / `errorDescription` lấy từ bảng **`error_codes`** theo header `Accept-Language` (`vi` mặc định, `en`).
- `errors` chỉ có khi `errorCode = COMMON_VALIDATION_FAILED`.
- Lỗi không lường trước → `COMMON_INTERNAL_ERROR` (chi tiết chỉ ghi log, không trả về client).

**Bảng `error_codes`** (`V3__create_error_codes.sql`): `code`, `http_status`, `message_vi`, `message_en`,
`description_vi`, `description_en`, `note` (ghi chú nội bộ, không trả về FE). Sửa câu chữ trực tiếp trong DB,
có hiệu lực sau `ERROR_CODES_CACHE_TTL` — không cần build lại.

**Ném lỗi trong code:**

```java
throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_REGISTERED);
```

**Thêm mã lỗi mới:** (1) migration mới thêm dòng vào `error_codes`; (2) thêm hằng số cùng tên vào enum
`ErrorCode` (kèm status + câu tiếng Anh dự phòng khi DB thiếu dòng); (3) nếu FE cần rẽ nhánh theo mã này,
thêm vào `ERROR_CODES` ở `src/app/core/config/error-codes.ts` của repo FE.

| Nhóm | Mã |
|---|---|
| Chung | `COMMON_BAD_REQUEST`, `COMMON_VALIDATION_FAILED`, `COMMON_UNAUTHORIZED`, `COMMON_FORBIDDEN`, `COMMON_NOT_FOUND`, `COMMON_CONFLICT`, `COMMON_INTERNAL_ERROR` |
| Auth | `AUTH_INVALID_CREDENTIALS`, `AUTH_ACCOUNT_LOCKED`, `AUTH_USER_NOT_FOUND`, `AUTH_EMAIL_ALREADY_REGISTERED`, `AUTH_RESET_TOKEN_INVALID`, `AUTH_REFRESH_TOKEN_INVALID`, `AUTH_CURRENT_PASSWORD_INCORRECT` |
| Chủ đề | `TOPIC_NOT_FOUND`, `TOPIC_NAME_TAKEN`, `TOPIC_IN_USE` |
| Bộ đề | `QUIZ_NOT_FOUND`, `QUIZ_CORRECT_OPTION_REQUIRED`, `QUIZ_DUPLICATE_OPTION`, `QUIZ_EMPTY`, `QUIZ_ITEM_NOT_FOUND` |

## Cấu trúc

```
src/main/java/com/charlie/quizlet/
├── config/        # Security, CORS, JWT, OpenAPI, AppProperties (cấu hình app.*), Clock
├── common/        # GlobalExceptionHandler, ApiPaths (đường dẫn API + endpoint công khai)
│   └── error/     # ErrorCode, BusinessException, ErrorCatalog (đọc + cache bảng error_codes)
├── auth/          # đăng ký, đăng nhập, JWT, refresh token (làm mới / đăng xuất)
│   └── reset/     # quên / đặt lại mật khẩu
├── user/
├── admin/         # tạo Admin lúc khởi động
├── topic/         # chủ đề của bộ đề (ADMIN quản lý)
├── quiz/          # bộ đề trắc nghiệm: câu hỏi + đáp án (ADMIN soạn)
└── <feature>/     # question, exam, flashcard...
src/main/resources/
├── application.yml
├── ValidationMessages_vi.properties   # câu lỗi validate tiếng Việt
└── db/migration/                      # Flyway: V1 users, V2 password_reset_tokens, V3 error_codes, V4 refresh_tokens, V5–V6 mã lỗi mới, V7 study_sets + cards (đã xoá ở V9), V8 quizzes + questions + question_options, V10 bỏ role TEACHER + topics
```
