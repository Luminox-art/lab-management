# Lab Management System

Hệ thống quản lý phòng thực hành và thiết bị CNTT, dùng Spring Boot, Thymeleaf, Flyway và MySQL 9.4.

- Đặc tả nghiệp vụ: `docs/SRS.docx`
- API: `docs/API-SPEC.md`
- Kiến trúc và ERD: `docs/ARCHITECTURE.md`
- Wireframe: `docs/WIREFRAMES.md`
- Ma trận truy vết: `docs/TRACEABILITY.md`
- Test case: `docs/TEST-CASES.md`
- Kế hoạch triển khai chi tiết: `docs/IMPLEMENTATION-PLAN.md`
- Cài đặt và vận hành: `docs/LOCAL-SETUP.md`
- Flyway: `src/main/resources/db/migration`

## Kiểm tra baseline Sprint 0

Yêu cầu JDK 21+ và Maven 3.9+. Chạy formatter, static analysis, test và đóng gói JAR bằng một lệnh:

```powershell
mvn -B verify
```

Để chạy với MySQL local, thiết lập `LAB_DB_PASSWORD` (và ghi đè `LAB_DB_URL`, `LAB_DB_USER` khi cần), sau đó chạy:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Readiness endpoint: `GET /actuator/health/readiness`.
