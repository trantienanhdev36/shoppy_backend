<div align="center">

# 🛒 Shopee Backend API

*Hệ thống Backend thương mại điện tử được xây dựng dựa trên mô hình Spring Boot, phục vụ các tính năng cốt lõi của một sàn thương mại điện tử.*

[![Java](https://img.shields.io/badge/Java-17-%23ED8B00?style=flat&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-%236DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-%234169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-%232496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)

</div>

---

## 🚀 Giới thiệu dự án
Dự án **Shopee Backend** mô phỏng lại hệ thống backend của một sàn thương mại điện tử. Hệ thống hỗ trợ phân quyền người dùng (Customer, Seller, Shipper, Admin), quản lý giỏ hàng, đặt hàng, theo dõi hành trình vận chuyển và tích hợp lưu trữ ảnh qua Cloudinary.

---

## 🛠️ Công nghệ sử dụng
* **Language & Framework:** Java 17, Spring Boot 3
* **Security & Auth:** Spring Security, JWT (JSON Web Token)
* **Database & ORM:** PostgreSQL 16, Spring Data JPA, Flyway / Hibernate
* **Containerization:** Docker & Docker Compose
* **Cloud Services:** Cloudinary (Lưu trữ hình ảnh CDN)
* **Documentation:** OpenAPI / Swagger UI

---

## 📂 Cấu trúc thư mục dự án

Cấu trúc mã nguồn tuân theo kiến trúc phân tầng (Layered Architecture) chuẩn của Spring Boot:

<details>
<summary><b>🔍 Xem chi tiết cấu trúc thư mục (Click để mở rộng)</b></summary>

```text
shopee-backend/
├── .github/
│   └── workflows/              # CI/CD Pipeline
├── docker/
│   ├── postgres/
│   │   └── init.sql            # Script khởi tạo database
│   └── docker-compose.yml      # Cấu hình container PostgreSQL 16
├── src/
│   ├── main/
│   │   ├── java/com/shopee/backend/
│   │   │   ├── ShopeeBackendApplication.java
│   │   │   ├── config/         # Cấu hình Spring Beans (Security, JWT, Swagger, Cloudinary...)
│   │   │   ├── common/         # Tiện ích chung, Enums, Exception Handler, ApiResponse
│   │   │   ├── controller/     # Tầng tiếp nhận HTTP Request & trả về Response
│   │   │   ├── service/        # Tầng xử lý logic nghiệp vụ chính
│   │   │   ├── repository/     # Tầng thao tác cơ sở dữ liệu (Spring Data JPA)
│   │   │   ├── entity/         # Định nghĩa các bảng Database & Jackson Annotations
│   │   │   └── domain/         # Chứa các Request & Response DTO phân theo module
│   │   └── resources/
│   │       ├── application.yml # Cấu hình chung hệ thống
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/   # Flyway migration scripts (V1__init_schema.sql)
│   └── test/                   # Unit & Integration Tests
├── .gitignore
├── pom.xml
└── README.md
