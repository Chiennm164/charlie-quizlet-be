# Hướng dẫn phát triển — Charlie Quizlet (Backend)

Tài liệu "làm thế nào" kèm code mẫu. Quy định bắt buộc: [CQ_CODING_RULES.md](CQ_CODING_RULES.md). Chạy dự án, bảng API, biến môi trường: [README.md](README.md).

## 1. Bức tranh tổng thể

```
src/main/java/com/charlie/quizlet/
├── config/            # SecurityConfig, JwtConfig, OpenApiConfig, AppConfig (Clock), AppProperties
├── common/
│   ├── ApiPaths       # hằng số đường dẫn API + danh sách endpoint công khai
│   ├── GlobalExceptionHandler   # mọi lỗi -> problem detail có errorCode
│   └── error/         # ErrorCode (enum), BusinessException, ErrorCatalog (đọc + cache bảng error_codes)
├── auth/              # đăng ký, đăng nhập, JWT, refresh token; reset/ = quên / đặt lại mật khẩu
├── user/              # User entity, Role, UserStatus, UserResponse
├── admin/             # AdminAccountInitializer (tạo Admin lúc khởi động), duyệt tài khoản chờ duyệt
└── studyset/          # học phần (StudySet) + thẻ (Card)
src/main/resources/
├── application.yml                 # cấu hình (app.* -> AppProperties)
├── ValidationMessages_vi.properties  # câu lỗi validate tiếng Việt
└── db/migration/                   # Flyway V1, V2, V3...
```

**Một request đi qua những gì:**

```
Request
  → Spring Security   (CORS; endpoint không công khai cần Bearer JWT; thiếu/sai token -> COMMON_UNAUTHORIZED)
  → Locale            (header Accept-Language, mặc định vi)
  → Controller        (@Valid DTO; sai -> COMMON_VALIDATION_FAILED + errors theo field)
  → Service           (logic; lỗi nghiệp vụ -> throw new BusinessException(ErrorCode.X))
  → Repository / DB
Lỗi ở bất kỳ bước nào → GlobalExceptionHandler → ErrorCatalog tra bảng error_codes theo ngôn ngữ → JSON lỗi
```

## 2. Cấu hình

`application.yml`:

```yaml
app:
  frontend:
    url: ${FRONTEND_URL:http://localhost:4200}
    reset-password-path: /reset-password
  password-reset:
    token-ttl: ${PASSWORD_RESET_TOKEN_TTL:PT30M}
```

Đọc trong code qua `AppProperties` (inject như bean bình thường):

```java
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final AppProperties appProperties;
    ...
    token.setExpiresAt(now.plus(appProperties.passwordReset().tokenTtl()));
    notifier.sendResetLink(user, appProperties.frontend().resetPasswordLink(rawToken));
}
```

**Thêm cấu hình mới** (vd. số câu tối đa mỗi đề):

1. `application.yml`: `app.exam.max-questions: ${EXAM_MAX_QUESTIONS:100}`
2. `AppProperties`: thêm `Exam exam` vào record chính + `public record Exam(int maxQuestions) { ... kiểm tra hợp lệ ... }`
3. Test đang tự tạo `AppProperties` thì truyền thêm tham số (`null` cho nhóm không dùng).
4. Có biến môi trường → thêm vào README.

## 3. Thêm một API

Ví dụ `GET /api/study-sets` (cần đăng nhập) — package `studyset/`:

```java
// common/ApiPaths.java
public static final String STUDY_SETS = API + "/study-sets";

// studyset/dto/StudySetResponse.java
public record StudySetResponse(Long id, String title, int termCount, Instant createdAt) {
    public static StudySetResponse from(StudySet s) { ... }
}

// studyset/StudySetController.java
@RestController
@RequiredArgsConstructor
@Tag(name = "Study sets")
public class StudySetController {

    private final StudySetService studySetService;

    @GetMapping(ApiPaths.STUDY_SETS)
    @Operation(summary = "List study sets of the current user")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "401", description = "Missing or expired token", content = @Content)
    public List<StudySetResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return studySetService.listForUser(Long.valueOf(jwt.getSubject()));
    }
}
```

- Endpoint công khai: thêm path vào `ApiPaths.PUBLIC_GET` / `PUBLIC_POST` và thêm `@SecurityRequirements` (rỗng) vào method.
- Phân quyền theo role: claim `role` đã map thành `ROLE_*` → dùng `.requestMatchers(...).hasRole("TEACHER")` trong `SecurityConfig` (vd. `ApiPaths.ADMIN_ALL` → `hasRole("ADMIN")`). Muốn dùng `@PreAuthorize("hasRole('ADMIN')")` trên method thì bật `@EnableMethodSecurity` (hiện chưa bật).
- Xong API: kiểm tra trên Swagger UI (`/swagger-ui.html`), cập nhật bảng API trong README, báo FE thêm `API_ENDPOINTS` + model.

## 4. Lỗi

**Ném lỗi:**

```java
User user = userRepository.findByEmail(email)
        .filter(u -> passwordEncoder.matches(raw, u.getPasswordHash()))
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
```

**Thêm mã lỗi mới** (vd. `EXAM_ALREADY_SUBMITTED`):

1. Migration mới `V<n>__add_exam_error_codes.sql`:
   ```sql
   INSERT INTO error_codes (code, http_status, message_vi, message_en, description_vi, description_en, note) VALUES
   ('EXAM_ALREADY_SUBMITTED', 409, 'Bạn đã nộp bài này rồi', 'You have already submitted this exam',
    'Mỗi đề chỉ được nộp một lần.', 'Each exam can only be submitted once.', NULL);
   ```
2. Enum `ErrorCode`: `EXAM_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "You have already submitted this exam"),`
3. Ném `new BusinessException(ErrorCode.EXAM_ALREADY_SUBMITTED)` trong service + test.
4. FE cần xử lý riêng mã này thì thêm vào `ERROR_CODES` bên FE; không thì FE tự hiện dialog lỗi chung với câu BE trả về.

**Sửa câu thông báo:** sửa trực tiếp bảng `error_codes` (hoặc migration mới cho các môi trường khác) — có hiệu lực sau `ERROR_CODES_CACHE_TTL` (mặc định 5 phút), không cần build lại.

**Câu lỗi validate** (trường `errors`): tiếng Việt trong `ValidationMessages_vi.properties`, tiếng Anh dùng bản có sẵn của Hibernate Validator. Ràng buộc mới chưa có câu tiếng Việt thì thêm key `jakarta.validation.constraints.<Tên>.message`.

## 5. Migration

```
src/main/resources/db/migration/
  V1__create_users.sql
  V2__create_password_reset_tokens.sql
  V3__create_error_codes.sql
  V4__create_refresh_tokens.sql
  V5__add_admin_error_codes.sql
  V6__add_change_password_error_code.sql
  V7__create_study_sets.sql
  V8__...                      ← thay đổi tiếp theo luôn là file mới
```

- Chạy tự động khi khởi động app (và khi chạy `CharlieQuizletBeApplicationTests`).
- Entity phải khớp schema — `ddl-auto: validate` sẽ báo lỗi khi khởi động nếu lệch.
- Không sửa file migration đã chạy; muốn đổi thì viết migration mới (`ALTER TABLE ...`).

## 6. Thời gian

```java
private final Clock clock;          // bean trong AppConfig

Instant now = clock.instant();      // không dùng Instant.now()
```

Test: `Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)`.

## 7. Test

```bash
./mvnw test                          # toàn bộ (cần PostgreSQL cho test context)
./mvnw test -Dtest=AuthServiceTest   # 1 class
```

| Loại | Cách viết | Ví dụ |
|---|---|---|
| Service | Unit test thuần, `mock(...)` repository, `Clock.fixed` | `AuthServiceTest`, `PasswordResetServiceTest` |
| Controller | `@WebMvcTest` + `@Import({ AppConfig, SecurityConfig, JwtConfig, JwtService, GlobalExceptionHandler, ErrorCatalog })`, service `@MockitoBean`, `@MockitoBean ErrorCodeRepository` | `AuthControllerTest` |
| Cấu hình / tài liệu API | `@SpringBootTest` + MockMvc | `OpenApiDocsTest` |

Kiểm tra lỗi bằng `jsonPath("$.errorCode")`, không so câu thông báo (nội dung nằm ở DB, có thể đổi).

## 8. Chạy & kiểm tra nhanh

```bash
export JAVA_HOME=~/.sdkman/candidates/java/current PATH=$JAVA_HOME/bin:$PATH
./mvnw spring-boot:run
curl localhost:8080/api/ping
```

- Swagger UI: http://localhost:8080/swagger-ui.html (nút *Authorize* để dán token).
- Devtools tự khởi động lại khi `target/classes` thay đổi (IDE build, hoặc `./mvnw compile`).
- Link đặt lại mật khẩu (chưa có SMTP) được ghi ra log: `Password reset link for ...`.
