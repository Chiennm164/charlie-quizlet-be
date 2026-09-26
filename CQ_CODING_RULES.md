# Coding Rules — Charlie Quizlet (Backend)

Quy định bắt buộc cho code trong repo này (Spring Boot 4.1, Java 21, PostgreSQL, Flyway, JUnit 5).
Hướng dẫn kèm code mẫu: [CQ_DEV_GUIDE.md](CQ_DEV_GUIDE.md). Quy tắc FE tương ứng: `charlie-quizlet/CQ_CODING_RULES.md`.

## 1. Cấu trúc package

- Chia theo **tính năng**, không chia theo tầng: `auth/`, `user/`, sau này `question/`, `exam/`, `flashcard/`... Mỗi package tự chứa controller, service, entity, repository, `dto/` của nó.
- `config/` — cấu hình Spring (Security, JWT, OpenAPI, `AppProperties`, `Clock`).
- `common/` — thứ dùng chung mọi tính năng: `GlobalExceptionHandler`, `ApiPaths`, `common/error/` (mã lỗi). Không để logic nghiệp vụ của 1 tính năng trong `common/`.
- Tính năng con tách package con khi đủ lớn (vd. `auth/reset/` cho quên / đặt lại mật khẩu).

## 2. Controller & API

- Controller chỉ nhận request, gọi service, trả response — **không** chứa logic nghiệp vụ, không gọi repository.
- Đường dẫn khai báo trong `common/ApiPaths` và dùng ở cả `@*Mapping` lẫn `SecurityConfig` — không gõ chuỗi `"/api/..."` rải rác.
- Endpoint công khai thêm vào `ApiPaths.PUBLIC_GET` / `PUBLIC_POST`; mặc định mọi endpoint khác **bắt buộc** Bearer token.
- Request / response là **DTO dạng `record`** trong `dto/` (hoặc `XxxResponse` cạnh entity). **Không** trả entity JPA ra ngoài.
- Request body luôn `@Valid`; ràng buộc khai báo trên DTO (`@NotBlank`, `@Email`, `@Size`...). Giới hạn độ dài phải khớp FE (`APP_SETTINGS.validation`) và cột DB.
- Mỗi endpoint có tài liệu OpenAPI: `@Operation(summary = ...)`, các `@ApiResponse` lỗi có thể xảy ra; endpoint công khai thêm `@SecurityRequirements` rỗng.
- Status code: tạo mới `201`, thành công không có body `204`, còn lại `200`.

## 3. Lỗi

- Lỗi nghiệp vụ ném `new BusinessException(ErrorCode.X)`. **Không** ném `ResponseStatusException`, không tự dựng `ResponseEntity` lỗi trong controller.
- Mọi mã lỗi có trong **cả** enum `ErrorCode` **và** bảng `error_codes` (migration). Tên mã `NHÓM_MÔ_TẢ` (`AUTH_INVALID_CREDENTIALS`, `EXAM_ALREADY_SUBMITTED`...).
- Nội dung thông báo cho người dùng nằm trong DB (`message_vi/en`, `description_vi/en`) — không hardcode câu thông báo trong Java. Enum chỉ giữ câu tiếng Anh dự phòng.
- Lỗi không lường trước để `GlobalExceptionHandler` trả `COMMON_INTERNAL_ERROR`; chi tiết chỉ ghi log, **không** trả stack trace / câu lỗi kỹ thuật ra client.
- Không lộ thông tin nhạy cảm qua lỗi: đăng nhập sai email hay sai mật khẩu đều `AUTH_INVALID_CREDENTIALS`; "quên mật khẩu" luôn `204`.
- Ràng buộc validate mới cần câu tiếng Việt thì thêm key vào `ValidationMessages_vi.properties`.

## 4. Service & dữ liệu

- Logic nghiệp vụ nằm ở service; method ghi dữ liệu `@Transactional`, chỉ đọc `@Transactional(readOnly = true)`.
- Inject qua constructor (`@RequiredArgsConstructor` + field `final`). Không `@Autowired` field, không nhiều constructor chỉ để test.
- Thời gian lấy từ bean `Clock` (`clock.instant()`), **không** gọi `Instant.now()` / `LocalDateTime.now()` trực tiếp — để test cố định được thời gian.
- Lưu thời gian kiểu `Instant` ↔ cột `TIMESTAMPTZ`.
- Chuẩn hoá dữ liệu đầu vào ở service (vd. email `trim().toLowerCase(Locale.ROOT)`).
- Enum lưu DB dạng chuỗi (`@Enumerated(EnumType.STRING)`) + `CHECK` constraint trong migration.

## 5. Database & migration

- Schema chỉ thay đổi qua **Flyway** (`src/main/resources/db/migration/V<số>__<mô_tả>.sql`); `ddl-auto: validate`.
- **Không sửa migration đã chạy** trên môi trường nào — thay đổi tiếp theo luôn là file `V<n+1>__...` mới.
- Tên bảng / cột `snake_case`, bảng số nhiều (`users`, `password_reset_tokens`); khoá ngoại có index; cột bắt buộc `NOT NULL`.
- Entity khớp đúng migration (tên cột, độ dài, nullable); `@CreationTimestamp` / `@UpdateTimestamp` cho `created_at` / `updated_at`.

## 6. Cấu hình

- Mọi cấu hình của app đặt trong `application.yml` dưới `app.*` và đọc qua **`AppProperties`** (JWT: `JwtProperties`). **Không** dùng `@Value("${...}")` rải rác, không hardcode URL / thời hạn / giới hạn trong code.
- Giá trị khác nhau theo môi trường dùng biến môi trường có mặc định cho dev: `${FRONTEND_URL:http://localhost:4200}`. Secret (`JWT_SECRET`, mật khẩu DB) **bắt buộc** đặt riêng khi deploy.
- Thêm cấu hình mới: thêm vào `application.yml` + field trong `AppProperties` (có kiểm tra bắt buộc trong compact constructor) + ghi vào bảng biến môi trường của README nếu có biến môi trường.
- Hằng số nội bộ không đổi theo môi trường (độ dài token, tên claim JWT...) là `private static final` trong class dùng nó.

## 7. Bảo mật

- Mật khẩu hash bằng `PasswordEncoder` (BCrypt); giới hạn 72 byte. Không log mật khẩu, token, secret.
- Token một lần (đặt lại mật khẩu...) chỉ lưu **hash** (SHA-256) trong DB, có thời hạn, dùng xong đánh dấu `used_at`.
- 401 / 403 do Security chặn cũng trả model lỗi chung (đã cấu hình trong `SecurityConfig`).
- CORS chỉ mở cho `app.cors.allowed-origins`.

## 8. Code style

- 4 space, UTF-8, LF (xem `.editorconfig`); `pom.xml` giữ tab.
- Import theo nhóm: `java.*` → `org.*` → `com.charlie.*` → thư viện khác (`io.*`, `jakarta.*`, `lombok.*`).
- Tên: class `PascalCase`, method / biến `camelCase`, hằng số `UPPER_SNAKE_CASE`, DTO `XxxRequest` / `XxxResponse`.
- Comment / Javadoc viết tiếng Việt, giải thích **vì sao** (quy tắc nghiệp vụ, ràng buộc), không nhắc lại code làm gì. Tên định danh, câu log, thông báo trong enum dự phòng bằng tiếng Anh. Code cũ còn comment tiếng Anh — chuyển dần khi sửa tới file đó, không cần sửa hàng loạt.
- Lombok chỉ dùng `@Getter/@Setter/@NoArgsConstructor` cho entity, `@RequiredArgsConstructor` cho bean, `@Slf4j`. Không dùng `@Data` cho entity.

## 9. Test

- Chạy `./mvnw test`. Test đặt cùng package với code: `src/test/java/.../XxxTest.java`.
- Service: unit test thuần (mock repository, `Clock.fixed(...)`), không cần Spring context.
- Controller: `@WebMvcTest` + `@Import` các config cần (`AppConfig`, `SecurityConfig`, `JwtConfig`, `GlobalExceptionHandler`, `ErrorCatalog`...), service `@MockitoBean`. Kiểm tra status + `errorCode`, không so câu thông báo tiếng Việt (có thể đổi trong DB).
- Mỗi mã lỗi mới / nhánh bảo mật (quyền, token hết hạn...) có ít nhất 1 test.
- `CharlieQuizletBeApplicationTests` chạy cả context + migration trên DB local — cần PostgreSQL đang chạy.

## 10. Git

- Commit ngắn gọn, nêu **vì sao**; mỗi commit 1 mục tiêu. Không commit `target/`, `.idea/`, secret.
- Thay đổi API (path, request / response, mã lỗi) phải cập nhật cùng lúc: OpenAPI annotation, README (bảng API / mã lỗi), và báo FE (`API_ENDPOINTS`, model, `ERROR_CODES`).
