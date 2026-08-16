# Lab Management System

Ứng dụng quản lý phòng thực hành và thiết bị, xây dựng bằng Spring Boot, Thymeleaf, MySQL và Flyway.

## Công nghệ

- Java 21
- Maven 3.9+
- MySQL 9.x
- Spring Boot
- Thymeleaf
- Flyway

## Chạy dự án trên máy local

### 1. Chuẩn bị

Cài đặt và khởi động:

- JDK 21
- Maven 3.9+
- MySQL 9.x

Kiểm tra:

```powershell
java -version
mvn -version
mysql --version
```

### 2. Tạo database

Đăng nhập MySQL:

```powershell
mysql -u root -p
```

Trong MySQL, chạy file `create.sql` một lần:

```sql
SOURCE /lab-management-system/create.sql;
```

Hoặc tự chạy câu lệnh sau:

```sql
CREATE DATABASE IF NOT EXISTS lab_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

`create.sql` chỉ tạo database. Khi ứng dụng khởi động, Flyway sẽ tự chạy các file trong `src/main/resources/db/migration` để tạo bảng và dữ liệu mẫu. Không cần chạy thủ công các file `V1`, `V2`, ...

### 3. Khai báo tài khoản MySQL

Sửa tài khoản và mật khẩu trong application.yml.

Trong PowerShell, thay `root` và `mat-khau-mysql` bằng tài khoản MySQL trên máy:

```powershell
$env:LAB_DB_URL='jdbc:mysql://localhost:3306/lab_management?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true'
$env:LAB_DB_USER='root'
$env:LAB_DB_PASSWORD='mat-khau-mysql'
```

Không **nên ghi mật khẩu thật trực tiếp vào `application.yml`** hoặc commit mật khẩu lên GitHub.

### 4. Chạy ứng dụng

```powershell
mvn spring-boot:run
```

Sau khi thấy thông báo `Started LabManagementApplication`, mở:

```text
http://localhost:8080
```

Để dừng ứng dụng, nhấn `Ctrl + C`.

## Kiểm tra và đóng gói

```powershell
mvn -B verify
```

File JAR được tạo trong thư mục `target`.
