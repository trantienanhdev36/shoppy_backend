# 🤖 AGENTS.md — Hướng dẫn cho AI Assistant

> **⚠️ ĐỌC FILE NÀY TRƯỚC KHI CHỈNH SỬA BẤT KỲ ĐIỀU GÌ TRONG DỰ ÁN**
>
> File này mô tả kiến trúc, tiến độ, quy ước code và lịch sử thay đổi của dự án Shoppy Backend.
> Mọi AI assistant phải đọc và hiểu nội dung này trước khi đề xuất hoặc thực hiện bất kỳ thay đổi nào.

---

## 📌 Thông tin dự án

| Thuộc tính       | Giá trị                                              |
|------------------|------------------------------------------------------|
| **Tên dự án**    | Shoppy Backend API                                   |
| **Mô tả**        | Hệ thống backend thương mại điện tử (Shopee clone)  |
| **Ngôn ngữ**     | Java 17                                              |
| **Framework**    | Spring Boot 4.1.1                                    |
| **Database**     | PostgreSQL 16                                        |
| **ORM**          | Spring Data JPA / Hibernate (`ddl-auto=update`)      |
| **Bảo mật**      | Spring Security + JWT (jjwt 0.12.6)                 |
| **Cache**        | Redis (Spring Data Redis)                            |
| **Cloud**        | Cloudinary (lưu trữ ảnh CDN)                        |
| **Email**        | Brevo SMTP (Spring Mail)                             |
| **Build tool**   | Maven                                                |
| **Container**    | Docker + Docker Compose                              |
| **Port mặc định**| 8080                                                 |

---

## 🏗️ Kiến trúc & Cấu trúc dự án

Dự án tuân theo **Layered Architecture** chuẩn Spring Boot:

```
src/main/java/com/shoppy/backend/
├── ShoppyApplication.java          # Entry point
├── config/
│   ├── CloudinaryConfig.java       # Bean cấu hình Cloudinary
│   ├── JwtAuthenticationFilter.java# Filter xác thực JWT mỗi request
│   └── SecurityConfig.java         # Cấu hình Spring Security, CORS, endpoint public
├── common/
│   ├── dto/
│   │   └── ApiResponse.java        # Wrapper response chuẩn cho toàn bộ API
│   ├── enums/
│   │   ├── OrderStatus.java        # Trạng thái đơn hàng
│   │   ├── PaymentMethod.java      # Phương thức thanh toán
│   │   ├── ShipperTransactionType.java
│   │   ├── TrackingStatus.java     # Trạng thái vận chuyển
│   │   ├── TransactionStatus.java
│   │   ├── UserRole.java           # CUSTOMER, SELLER, SHIPPER, ADMIN
│   │   ├── UserStatus.java
│   │   └── WalletTransactionType.java
│   ├── exception/
│   │   ├── AppException.java       # Custom exception với ErrorCode
│   │   ├── ErrorCode.java          # Enum tập trung mã lỗi
│   │   └── GlobalExceptionHandler.java # @ControllerAdvice xử lý exception toàn cục
│   └── util/
│       ├── JwtUtils.java           # Tạo & xác thực JWT token
│       ├── OrderQueueWorker.java   # Worker xử lý hàng đợi đơn hàng
│       └── WalletQueueWorker.java  # Worker xử lý hàng đợi ví tiền
├── controller/
│   ├── AuthController.java         # /api/auth/** — đăng ký, đăng nhập, xác thực email
│   ├── OrderController.java        # /api/orders/** — tạo đơn, xem đơn, cập nhật trạng thái
│   ├── ProductController.java      # /api/products/** — CRUD sản phẩm
│   ├── ShopController.java         # /api/shops/** — đăng ký shop, quản lý shop
│   ├── TestUploadController.java   # /api/test/upload — test upload ảnh (DEV only)
│   ├── UserController.java         # /api/users/** — profile, thông tin tài khoản
│   └── WalletController.java       # /api/wallet/** — nạp tiền, lịch sử giao dịch
├── domain/
│   ├── request/
│   │   ├── auth/LoginRequest.java
│   │   ├── order/CreateOrderRequest.java
│   │   ├── product/CreateProductRequest.java
│   │   ├── product/ProductVariantRequest.java
│   │   ├── shop/RegisterShopRequest.java
│   │   └── user/RegisterRequest.java, DepositRequest.java
│   └── response/
│       ├── auth/AuthResponse.java
│       ├── order/OrderResponse.java
│       ├── product/ProductDetailResponseDTO.java, ProductResponseDTO.java
│       │             ProductImageDTO.java, ProductVariantDTO.java
│       ├── shop/ShopResponse.java
│       └── user/UserResponse.java
├── entity/
│   ├── BaseEntity.java             # id (UUID), createdAt, updatedAt — tất cả entity kế thừa
│   ├── User.java                   # Người dùng (role, wallet balance, status)
│   ├── Shop.java                   # Cửa hàng (thuộc về 1 User/SELLER)
│   ├── Product.java                # Sản phẩm (thuộc về 1 Shop)
│   ├── ProductImage.java           # Ảnh sản phẩm (URL Cloudinary)
│   ├── ProductVariant.java         # Biến thể sản phẩm (màu, size, giá, tồn kho)
│   ├── Order.java                  # Đơn hàng
│   ├── OrderItem.java              # Chi tiết đơn hàng (1 order → nhiều items)
│   ├── OrderTracking.java          # Lịch sử tracking vận chuyển
│   ├── ShipperCodTransaction.java  # Giao dịch COD của shipper
│   ├── ShippingCompany.java        # Công ty vận chuyển
│   ├── VerificationToken.java      # Token xác thực email
│   ├── WalletDeposit.java          # Lịch sử nạp tiền vào ví
│   └── WalletTransaction.java      # Giao dịch ví (mua hàng, hoàn tiền...)
├── repository/                     # Spring Data JPA Repositories (1 file/entity)
└── service/
    ├── AuthService.java            # Đăng ký, đăng nhập, verify email
    ├── CloudinaryService.java      # Upload/xóa ảnh lên Cloudinary
    ├── CustomUserDetailsService.java # Implement UserDetailsService cho Spring Security
    ├── EmailService.java           # Gửi email qua Brevo SMTP
    ├── OrderService.java           # Logic xử lý đơn hàng
    ├── ProductService.java         # Logic sản phẩm
    ├── ShopService.java            # Logic shop
    ├── UserService.java            # Logic user/profile
    └── WalletService.java          # Logic ví tiền
```

---

## 🔑 Quy ước & Convention bắt buộc

### 1. Entity
- **Tất cả entity** phải kế thừa `BaseEntity` (id UUID, createdAt, updatedAt tự động).
- Dùng **Lombok** (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).
- Quan hệ JPA: `@ManyToOne` dùng `FetchType.LAZY` theo mặc định.
- Không dùng `@JsonIgnore` trực tiếp trên entity — dùng DTO/Response class thay thế.

### 2. API Response
- **Mọi API đều phải wrap response trong `ApiResponse<T>`** — không trả naked object.
- HTTP status code phải phù hợp ngữ nghĩa (200, 201, 400, 401, 403, 404...).

### 3. Exception Handling
- Ném `AppException(ErrorCode.XYZ)` thay vì các exception thông thường.
- Thêm mã lỗi mới vào `ErrorCode.java` — **không hardcode message trong service**.

### 4. Security
- Các endpoint yêu cầu xác thực: lấy user hiện tại qua `SecurityContextHolder`.
- Endpoint public phải được khai báo tường minh trong `SecurityConfig`.

### 5. Database
- **Dùng `ddl-auto=update`** (Hibernate tự cập nhật schema) — không dùng Flyway migration trừ khi cần migration phức tạp.
- Migration SQL (nếu có) đặt ở `src/main/resources/db/migration/` với prefix `V{date}__{description}.sql`.

### 6. Cấu hình môi trường
- Secrets (DB password, JWT secret, Cloudinary key...) **chỉ qua biến môi trường** hoặc file `.env` (đã gitignore).
- Không commit `.env` lên Git.

### 7. Package & Naming
- Request DTO: `domain/request/{module}/{Action}Request.java`
- Response DTO: `domain/response/{module}/{Entity}Response.java` hoặc `{Entity}ResponseDTO.java`
- Enums: `common/enums/{Name}.java`

---

## ✅ Tiến độ & Tính năng đã hoàn thành

| Module            | Tính năng                                     | Trạng thái    |
|-------------------|-----------------------------------------------|---------------|
| **Auth**          | Đăng ký tài khoản                             | ✅ Hoàn thành |
|                   | Đăng nhập + JWT token                         | ✅ Hoàn thành |
|                   | Xác thực email (VerificationToken)            | ✅ Hoàn thành |
|                   | Quên mật khẩu & Đặt lại MK (OTP Redis + Email)| ✅ Hoàn thành |
| **User**          | Xem thông tin profile                         | ✅ Hoàn thành |
|                   | Cập nhật profile                              | ✅ Hoàn thành |
| **Shop**          | Đăng ký shop (SELLER)                         | ✅ Hoàn thành |
|                   | Xem thông tin shop                            | ✅ Hoàn thành |
|                   | Cập nhật thông tin shop (PUT /shops/me)       | ✅ Hoàn thành |
|                   | Public Shop Profile & Sản phẩm (GET /shops/{id})| ✅ Hoàn thành |
| **Product**       | Tạo sản phẩm + upload ảnh (Cloudinary)        | ✅ Hoàn thành |
|                   | Biến thể sản phẩm (variant: màu, size)        | ✅ Hoàn thành |
|                   | Xem danh sách & chi tiết sản phẩm             | ✅ Hoàn thành |
|                   | Phân trang danh sách sản phẩm (5/trang)       | ✅ Hoàn thành |
|                   | Seller xem sản phẩm của Shop (lọc tồn kho)    | ✅ Hoàn thành |
|                   | Cập nhật sản phẩm (PUT /products/{id})        | ✅ Hoàn thành |
|                   | API Atomic biến thể (sửa kho, thêm biến thể)  | ✅ Hoàn thành |
|                   | Xóa / Ẩn sản phẩm                             | ✅ Hoàn thành |
|                   | Xóa / Ẩn biến thể sản phẩm                    | ✅ Hoàn thành |
| **Order**         | Tạo đơn hàng                                  | ✅ Hoàn thành |
|                   | Xem đơn hàng của user                         | ✅ Hoàn thành |
|                   | Cập nhật trạng thái đơn hàng                  | ✅ Hoàn thành |
|                   | Seller xem đơn của Shop (GET /seller/orders)  | ✅ Hoàn thành |
|                   | Seller hủy đơn (Hoàn tiền ví & kho)           | ✅ Hoàn thành |
|                   | Customer hủy đơn (khi Shop chưa chuẩn bị)    | ✅ Hoàn thành |
|                   | WAITING_FOR_PAYMENT status                    | ✅ Hoàn thành (migration V20261022) |
| **Wallet**        | Nạp tiền vào ví                               | ✅ Hoàn thành |
|                   | Xem lịch sử giao dịch                         | ✅ Hoàn thành |
|                   | Thanh toán qua ví khi đặt hàng                | ✅ Hoàn thành |
| **Shipping**      | Tracking đơn hàng (OrderTracking)             | ✅ Hoàn thành |
|                   | Shipper COD transaction                       | ✅ Hoàn thành |
|                   | Shipper xem đơn sẵn sàng nhận (READY_TO_SHIP)| ✅ Hoàn thành |
|                   | Shipper nhận đơn → giao hàng                  | ✅ Hoàn thành |
|                   | Shipper xác nhận giao thành công              | ✅ Hoàn thành |
|                   | Shipper xem lịch sử COD + số dư               | ✅ Hoàn thành |
|                   | Shipper nộp tiền COD (REMITTANCE)             | ✅ Hoàn thành |
|                   | Đăng ký làm Shipper (nâng cấp từ CUSTOMER)    | ✅ Hoàn thành |
|                   | Danh sách công ty vận chuyển (Public API)     | ✅ Hoàn thành |
| **Admin**         | Quản lý người dùng (Khóa/Mở, Role, Lọc)       | ✅ Hoàn thành |
|                   | Quản lý Cửa hàng toàn sàn (Khóa/Mở Shop)      | ✅ Hoàn thành |
|                   | Quản lý Danh mục toàn sàn (Category CRUD)     | ✅ Hoàn thành |
|                   | Quản lý Đối tác Vận chuyển (Thêm, Deactive, CRUD) | ✅ Hoàn thành |
|                   | Dashboard thống kê                            | ❌ Chưa làm  |
| **Cart**          | Giỏ hàng                                      | ❌ Chưa làm  |
| **Review**        | Đánh giá sản phẩm                             | ❌ Chưa làm  |
| **Notification**  | Thông báo in-app (polling API)                | ✅ Hoàn thành |

---

## 🚫 Những điều đã biết cần tránh (Lessons Learned)

> Mục này ghi lại các lỗi từng gặp để AI không lặp lại.

### [2026-10-22] — Migration SQL thay vì thay đổi Entity trực tiếp
- **Vấn đề**: Thêm giá trị `WAITING_FOR_PAYMENT` vào enum `OrderStatus` yêu cầu migration DB riêng.
- **Giải pháp**: Tạo file `V20261022__allow_waiting_for_payment_order_status.sql` thực hiện `ALTER TYPE`.
- **Bài học**: Khi thêm giá trị vào PostgreSQL ENUM type đang tồn tại, **luôn dùng migration SQL** — Hibernate `ddl-auto=update` không tự xử lý được.

### [2026-10-29] — Không gán lại Collection có orphanRemoval trên Managed JPA Entity
- **Vấn đề**: Gán lại danh sách (`order.setItems(items)`) trên entity `Order` đang được quản lý bởi Hibernate Session trong `@Transactional` gây ra `HibernateException: A collection with orphan deletion was no longer referenced by the owning entity instance`.
- **Giải pháp**: Không gọi setter gán danh sách mới lên Entity để tránh làm đứt liên kết PersistentBag của Hibernate. Thay vào đó, truyền trực tiếp danh sách `items` và `trackings` vào hàm mapper của DTO: `OrderDetailResponse.fromEntity(order, items, trackings)`.
- **Bài học**: Với các quan hệ `@OneToMany(..., orphanRemoval = true)`, tuyệt đối không dùng setter để gán instance Collection mới lên entity đang managed.

### [2026-10-29] — Thêm cột NOT NULL vào Entity khi bảng đã có dữ liệu
- **Vấn đề**: Khi thêm trường `@Column(nullable = false)` vào Entity (như trường `status` của `Category`), Hibernate sinh DDL `ALTER TABLE categories ADD COLUMN status VARCHAR(20) NOT NULL`. Nếu bảng đã có dữ liệu từ trước, PostgreSQL sẽ báo lỗi: `ERROR: column "status" of relation "categories" contains null values` và huỷ bỏ việc thêm cột.
- **Giải pháp**:
  1. Chỉ định rõ `columnDefinition` có kèm giá trị mặc định: `@Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")`.
  2. Tạo migration SQL tương ứng `ALTER TABLE categories ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';`.
- **Bài học**: Khi thêm cột mới có `nullable = false` vào bảng đã tồn tại dữ liệu, **bắt buộc phải có `columnDefinition = "... DEFAULT '...'"`** để Hibernate sinh câu lệnh alter table có giá trị default, tránh bị PostgreSQL từ chối do null values.

### [2026-10-29] — Lỗi `function lower(bytea) does not exist` khi dùng `LOWER(CONCAT('%', :search, '%'))` với PostgreSQL
- **Vấn đề**: Khi tham số `:search` là `null`, PostgreSQL JDBC driver không xác định được kiểu dữ liệu của parameter trong phép toán ghép chuỗi `CONCAT` hoặc `||`, dẫn đến việc driver bind tham số với kiểu nhị phân `bytea`. Khi hàm `LOWER()` được gọi lên kiểu này, PostgreSQL ném lỗi `ERROR: function lower(bytea) does not exist`.
- **Giải pháp**:
  1. Xử lý gắn wildcard `%` và chuyển chữ thường ở tầng Java Service: `String cleanSearch = (search != null && !search.trim().isEmpty()) ? "%" + search.trim().toLowerCase() + "%" : null;`.
  2. Trong JPQL chỉ cần viết đơn giản: `(:search IS NULL OR LOWER(e.field) LIKE :search)`. Vì `:search` đứng cạnh toán tử `LIKE`, JPA/Hibernate tự động xác định chính xác kiểu dữ liệu là `VARCHAR/String`, vừa tối ưu hiệu năng DB vừa loại bỏ hoàn toàn lỗi `lower(bytea)`.

---



## 📝 Change Log (Lịch sử thay đổi)

> **Quy tắc**: Mỗi lần AI chỉnh sửa dự án, PHẢI ghi lại entry mới vào đây theo format dưới.

### Format ghi log:
```
### [YYYY-MM-DD] — {Tiêu đề thay đổi}
- **Thay đổi**: Mô tả ngắn gọn những gì đã làm
- **File liên quan**: Danh sách file đã tạo/sửa/xóa
- **Lý do**: Tại sao thay đổi này cần thiết
- **Lưu ý**: Cảnh báo hoặc side effect cần biết (nếu có)
```

---

### [2026-10-26] — Khởi tạo AGENTS.md
- **Thay đổi**: Tạo file `AGENTS.md` để làm tài liệu ngữ cảnh dự án cho AI assistant
- **File liên quan**: `AGENTS.md` (tạo mới)
- **Lý do**: Chuẩn hóa việc AI hiểu dự án trước khi chỉnh sửa, lưu tiến độ và bài học kinh nghiệm
- **Lưu ý**: File này phải được cập nhật sau mỗi lần thay đổi đáng kể

---

### [2026-10-26] — Phân trang danh sách sản phẩm
- **Thay đổi**: Implement pagination cho endpoint `GET /products/getAll`
  - Tạo `PagedResponse<T>` — generic wrapper cho bất kỳ response nào cần phân trang
  - Cập nhật `ProductRepository` — thêm `findProductPriceRangesByIds()` chỉ query price range theo danh sách ID (tránh load toàn bộ khi phân trang)
  - Cập nhật `ProductService.getAllProducts(int page, int size)` — dùng Spring Data `Pageable`, sắp xếp mới nhất lên đầu
  - Cập nhật `ProductController.getAllProducts()` — nhận query params `?page=0&size=5`
- **File liên quan**:
  - `common/dto/PagedResponse.java` (tạo mới)
  - `repository/ProductRepository.java` (thêm method)
  - `service/ProductService.java` (thay đổi signature và logic)
  - `controller/ProductController.java` (thêm query params, đổi return type)
- **Lý do**: Tránh load toàn bộ sản phẩm một lần, tối ưu hiệu năng
- **Lưu ý**:
  - `page` là **0-indexed** (trang đầu tiên là `page=0`)
  - `size` mặc định là **5**, có thể override qua query param
  - `PagedResponse<T>` có thể tái sử dụng cho Order, User, v.v.

---

### [2026-10-26] — Module Thông báo In-app (Notification System)
- **Thay đổi**: Phát triển hệ thống thông báo In-app hoàn chỉnh cho Shoppy Backend
  - Tạo enum `NotificationType` (`ORDER_PLACED`, `ORDER_STATUS_CHANGED`, `NEW_ORDER_FOR_SELLER`, `WALLET_DEPOSIT`, `EMAIL_VERIFIED`)
  - Tạo entity `Notification` (kế thừa `BaseEntity`, quan hệ `@ManyToOne(LAZY)` với `User`, `EnumType.STRING`, cờ `isRead`, `referenceId`)
  - Tạo `NotificationRepository` (hỗ trợ phân trang, đếm chưa đọc, bulk update `markAllAsRead`)
  - Tạo DTO `NotificationResponse`
  - Thêm error codes vào `ErrorCode`: `NOTIFICATION_NOT_FOUND`, `NOTIFICATION_ACCESS_DENIED`
  - Tạo `NotificationService` xử lý gửi thông báo `@Async` không chặn luồng chính, cùng các hàm nghiệp vụ: xem danh sách có phân trang, đếm chưa đọc, đánh dấu đã đọc
  - Tạo `NotificationController` cung cấp các API:
    - `GET /notifications` (hỗ trợ `page`, `size`)
    - `GET /notifications/unread-count`
    - `PATCH /notifications/{id}/read`
    - `PATCH /notifications/read-all`
  - Cập nhật `SecurityConfig` khai báo endpoint `/notifications/**` yêu cầu authenticated
  - Tích hợp gửi thông báo tự động vào các luồng nghiệp vụ:
    - `OrderService`: Bắn thông báo khi đặt hàng thành công (cho Customer & Seller), và khi trạng thái đơn hàng thay đổi (xác nhận, đóng gói, giao hàng, hoàn tất, hủy đơn)
    - `WalletService`: Bắn thông báo khi nạp tiền ví thành công
    - `AuthService`: Bắn thông báo khi kích hoạt email thành công
- **File liên quan**:
  - `common/enums/NotificationType.java` (tạo mới)
  - `entity/Notification.java` (tạo mới)
  - `repository/NotificationRepository.java` (tạo mới)
  - `domain/response/notification/NotificationResponse.java` (tạo mới)
  - `service/NotificationService.java` (tạo mới)
  - `controller/NotificationController.java` (tạo mới)
  - `common/exception/ErrorCode.java` (sửa)
  - `config/SecurityConfig.java` (sửa)
  - `service/OrderService.java` (sửa)
  - `service/WalletService.java` (sửa)
  - `service/AuthService.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Cung cấp tính năng thông báo in-app theo cơ chế polling cho người dùng
- **Lưu ý**:
  - Gửi thông báo sử dụng `@Async` để đảm bảo không ảnh hưởng hiệu năng và không gây rollback giao dịch nghiệp vụ cốt lõi nếu có lỗi gửi thông báo
  - Kiểu enum lưu `EnumType.STRING` dạng VARCHAR nên Hibernate `ddl-auto=update` tự động tạo bảng mà không gặp vấn đề ENUM type của PostgreSQL

---

### [2026-10-26] — Nghiệp vụ Shipper & Quản lý COD
- **Thay đổi**: Phát triển toàn diện hệ thống nghiệp vụ dành riêng cho Shipper:
  - Bổ sung các ErrorCode vào `ErrorCode`: `SHIPPER_NOT_BELONG_TO_COMPANY`, `ORDER_NOT_AVAILABLE_FOR_SHIPPER`, `ORDER_NOT_ASSIGNED_TO_YOU`, `ORDER_NOT_IN_SHIPPING_STATE`, `INVALID_REMITTANCE_AMOUNT`, `EXCEEDS_COD_BALANCE`
  - Bổ sung query trong `OrderRepository`: `findByShippingCompanyIdAndStatusOrderByCreatedAtAsc`, `findByIdAndShippingCompanyId`, `findByShipperIdOrderByCreatedAtDesc`, `findByShipperIdAndStatusOrderByCreatedAtDesc`
  - Bổ sung aggregate SUM query trong `ShipperCodTransactionRepository`: `sumAmountByShipperIdAndType`, `sumTotalAmountByShipperId`
  - Tạo DTO: `CodRemittanceRequest`, `CodBalanceResponse`
  - Tạo `ShipperService` xử lý 7 luồng nghiệp vụ:
    1. `getAvailableOrders`: Lấy danh sách đơn READY_TO_SHIP theo shipping company của shipper
    2. `getOrderDetail`: Xem chi tiết đơn hàng (chỉ cho phép xem đơn thuộc hãng mình)
    3. `assignShipper`: Nhận đơn chuyển sang trạng thái SHIPPING kèm tạo bản ghi OrderTracking
    4. `confirmDelivered`: Xác nhận giao thành công chuyển sang DELIVERED, tự động tạo giao dịch COD COLLECTION nếu đơn là COD, gửi notification
    5. `getMyDeliveries`: Xem các đơn shipper đã/đang nhận (hỗ trợ lọc theo OrderStatus)
    6. `getMyCodTransactions` & `getMyCodBalance`: Xem lịch sử COD và số dư tiền mặt COD đang giữ (tổng thu - tổng nộp)
    7. `submitCodRemittance`: Nộp tiền COD về hãng (tạo giao dịch REMITTANCE, kiểm tra không được nộp quá số dư đang giữ)
  - Tạo `ShipperController` cung cấp các API `/shipper/**`
  - Cấu hình phân quyền trong `SecurityConfig` cho role `SHIPPER`
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa)
  - `repository/OrderRepository.java` (sửa)
  - `repository/ShipperCodTransactionRepository.java` (sửa)
  - `domain/request/shipper/CodRemittanceRequest.java` (tạo mới)
  - `domain/response/shipper/CodBalanceResponse.java` (tạo mới)
  - `service/ShipperService.java` (tạo mới)
  - `controller/ShipperController.java` (tạo mới)
  - `config/SecurityConfig.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Hoàn thiện toàn bộ các luồng nghiệp vụ logistics phía Shipper và kiểm soát đối soát dòng tiền thu hộ COD.
- **Lưu ý**:
  - Shipper chỉ được nhận đơn thuộc công ty vận chuyển (`ShippingCompany`) mà shipper đó trực thuộc.
  - Số dư COD giữ trong tay được tính bằng `SUM(COLLECTION) - SUM(REMITTANCE)`. Khi nộp tiền thì số nộp không được vượt quá số dư COD hiện có.

---

### [2026-10-26] — Tái cấu trúc Đăng ký User & Nghiệp vụ Đăng ký Shipper
- **Thay đổi**:
  - Tách bạch vai trò đăng ký: xóa bỏ trường `role` và `shippingCompanyId` trong form đăng ký tài khoản `POST /users/register`. Mọi user mới đăng ký mặc định 100% là `CUSTOMER`.
  - Triển khai cơ chế Đăng ký Shipper tương đồng với Đăng ký Shop: Người dùng (`CUSTOMER`) đã đăng nhập có thể gửi yêu cầu đăng ký nâng cấp thành Shipper qua `POST /users/shippers/register`.
  - Kiểm tra nghiêm ngặt: không cho phép đăng ký nếu đã là `SHIPPER`, chỉ cho phép `CUSTOMER` đăng ký (chặn `SELLER` / `ADMIN`), hãng vận chuyển phải tồn tại và đang `ACTIVE`.
  - Bổ sung các ErrorCode: `ALREADY_A_SHIPPER`, `ONLY_CUSTOMER_CAN_REGISTER_SHIPPER`, `SHIPPING_COMPANY_NOT_ACTIVE`.
  - Bổ sung controller `ShippingCompanyController` cung cấp API public `GET /shipping-companies` và `GET /shipping-companies/{id}` để client lấy danh sách hãng vận chuyển khả dụng.
  - Cập nhật `SecurityConfig` cho phép `GET /shipping-companies/**` truy cập tự do.
- **File liên quan**:
  - `domain/request/user/RegisterRequest.java` (sửa: bỏ role, shippingCompanyId)
  - `service/UserService.java` (sửa: cố định role CUSTOMER)
  - `domain/request/shipper/RegisterShipperRequest.java` (tạo mới)
  - `domain/response/shipper/ShipperResponse.java` (tạo mới)
  - `domain/response/shipping/ShippingCompanyResponse.java` (tạo mới)
  - `repository/ShippingCompanyRepository.java` (sửa: thêm method `findByStatusOrderByNameAsc`)
  - `service/ShipperService.java` (sửa: thêm method `registerShipper`)
  - `controller/UserController.java` (sửa: thêm endpoint `POST /users/shippers/register`)
  - `controller/ShippingCompanyController.java` (tạo mới: cung cấp `GET /shipping-companies`)
  - `common/exception/ErrorCode.java` (sửa: thêm mã lỗi 4017, 4018, 4019)
  - `config/SecurityConfig.java` (sửa: permitAll cho `/shipping-companies/**`)
  - `AGENTS.md` (sửa: cập nhật tiến độ và log)
- **Lý do**: Tăng cường bảo mật tránh client tự gán role tùy ý, chuẩn hóa quy trình nâng cấp vai trò theo chuẩn e-commerce.
- **Lưu ý**: Endpoint `POST /users/shippers/register` yêu cầu JWT token của tài khoản `CUSTOMER`. Sau khi đăng ký thành công, JWT token cũ vẫn dùng được bình thường vì Security filter nạp UserDetails trực tiếp từ DB.

---

### [2026-10-26] — Chuẩn hóa toàn bộ Response DTO sang phương thức fromEntity()
- **Thay đổi**:
  - Bổ sung phương thức tĩnh `fromEntity()` cho toàn bộ nhóm Product DTOs:
    + `ProductImageDTO.fromEntity(ProductImage)`
    + `ProductVariantDTO.fromEntity(ProductVariant)`
    + `ProductDetailResponseDTO.fromEntity(Product)`
    + `ProductResponseDTO.fromEntity(Product)` & `fromEntity(Product, minPrice, maxPrice)`
  - Refactor `ProductService`: Xóa bỏ các mapper thủ công (`mapToProductResponse`, `setPriceRange`), chuyển sang gọi trực tiếp `fromEntity()`.
  - Cập nhật `UserResponse`: Bổ sung đầy đủ các trường thông tin hồ sơ (`phone`, `balance`, `avatarUrl`, `shippingCompanyId`, `createdAt`, `updatedAt`), thêm `@JsonInclude(NON_NULL)` và phương thức `UserResponse.fromEntity(User)`.
  - Chuẩn hóa `UserController`: Chuyển 2 endpoint trước đây trả Entity trần (`POST /users/register` và `GET /users/profile`) sang trả về `ApiResponse<UserResponse>` sử dụng `UserResponse.fromEntity(...)`.
  - Cập nhật `AuthService.login()`: Sử dụng `UserResponse.fromEntity(user)` thay vì build thủ công.
- **File liên quan**:
  - `domain/response/product/ProductImageDTO.java` (sửa)
  - `domain/response/product/ProductVariantDTO.java` (sửa)
  - `domain/response/product/ProductDetailResponseDTO.java` (sửa)
  - `domain/response/product/ProductResponseDTO.java` (sửa)
  - `domain/response/user/UserResponse.java` (sửa)
  - `service/ProductService.java` (sửa)
  - `service/AuthService.java` (sửa)
  - `controller/UserController.java` (sửa)
  - `domain/request/shipper/CodRemittanceRequest.java` (tái tạo)
  - `AGENTS.md` (sửa)
- **Lý do**: Triệt để tuân thủ quy ước kiến trúc Best Practice: không bao giờ trả Entity trần ra API, tái sử dụng logic ánh xạ qua `fromEntity()`, tránh rủi ro LazyInitializationException và lộ thông tin nhạy cảm.

---

### [2026-10-28] — Thêm bảng Categories & liên kết với Products
- **Thay đổi**:
  - Tạo entity `Category` kế thừa `BaseEntity`, hỗ trợ cây phân cấp (self-referencing `parent_id`), trường `slug` thân thiện URL, `imageUrl` (Cloudinary), `displayOrder`.
  - Tạo `CategoryRepository` với các query: `findBySlug`, `findAllRootCategories`, `findByParentId`, `findAllActive`.
  - Cập nhật entity `Product`: thêm quan hệ `@ManyToOne(FetchType.LAZY)` với `Category` (cột `category_id`, **nullable** để không phá vỡ dữ liệu cũ).
  - Tạo migration SQL `V20261028__add_categories_table.sql` bao gồm:
    - `CREATE TABLE categories` (với cột `parent_id` tự tham chiếu, hỗ trợ cây phân cấp)
    - `ALTER TABLE products ADD COLUMN category_id` (nullable, FK → categories)
    - `INSERT` 10 danh mục gốc: Điện tử & Công nghệ, Thời trang, Nhà cửa & Đời sống, Sức khỏe & Làm đẹp, Thể thao & Du lịch, Thực phẩm & Đồ uống, Sách & Văn phòng phẩm, Mẹ & Bé, Ô tô & Xe máy, Khác
    - `INSERT` 14 danh mục con (sub-categories) cho các nhóm chính
    - `UPDATE products SET category_id = (id của 'khac') WHERE category_id IS NULL` — gán toàn bộ sản phẩm cũ về danh mục "Khác"
    - `CREATE INDEX` cho `category_id`, `slug`, `parent_id`, `display_order`
- **File liên quan**:
  - `entity/Category.java` (tạo mới)
  - `repository/CategoryRepository.java` (tạo mới)
  - `entity/Product.java` (sửa: thêm trường `category`)
  - `src/main/resources/db/migration/V20261028__add_categories_table.sql` (tạo mới)
  - `AGENTS.md` (sửa)
- **Lý do**: Cung cấp cơ sở hạ tầng danh mục sản phẩm để hỗ trợ lọc, tìm kiếm và phân loại sản phẩm theo danh mục.
- **Lưu ý**:
  - Cột `category_id` trong `products` là **nullable** — sản phẩm mới nên được yêu cầu truyền category khi tạo (validation ở service layer, không phải DB constraint).
  - Migration `UPDATE` sẽ gán tất cả sản phẩm cũ (chưa có category) vào danh mục **"Khác"** (slug=`khac`) — an toàn cho dữ liệu production.
  - Slug phải là duy nhất — khi thêm danh mục mới cần generate slug từ name.
  - `ON CONFLICT (slug) DO NOTHING` trong INSERT đảm bảo idempotent — chạy migration nhiều lần không gây lỗi.
  - Cấu trúc cây (`parent_id`) hỗ trợ đệ quy không giới hạn cấp — nên giới hạn UI ở 2-3 cấp để UX tốt.

---

### [2026-10-28] — Phân trang GET /orders/my & Fix clamp trang cuối
- **Thay đổi**:
  - Tạo `PaginationUtils.fetchWithClamp()` — utility tái sử dụng logic "last page clamp": nếu `page` yêu cầu vượt quá tổng số trang và dữ liệu tồn tại → tự động redirect về trang cuối thay vì trả về rỗng.
  - Thêm overload `Page<Order> findByCustomerId(Long customerId, Pageable pageable)` vào `OrderRepository`.
  - Sửa `OrderService.getMyOrders()`: thay đổi signature thành `getMyOrders(email, page, size)` trả `PagedResponse<OrderSummaryResponse>`, sử dụng `PaginationUtils.fetchWithClamp`.
  - Sửa `OrderController.GET /orders/my`: nhận query params `?page=0&size=10` (mặc định 10/trang), wrap response trong `ApiResponse<PagedResponse<...>>`.
  - Fix `ProductService.getAllProducts()`: bổ sung clamp logic tương tự — nếu page vượt tổng trang → truy vấn lại trang cuối. Giữ nguyên logic query price range tối ưu.
- **File liên quan**:
  - `common/util/PaginationUtils.java` (tạo mới)
  - `repository/OrderRepository.java` (sửa: thêm `Page<Order> findByCustomerId(Long, Pageable)`)
  - `service/OrderService.java` (sửa: đổi signature `getMyOrders`)
  - `controller/OrderController.java` (sửa: endpoint `GET /orders/my` thêm page/size params)
  - `service/ProductService.java` (sửa: thêm clamp logic vào `getAllProducts`)
  - `AGENTS.md` (sửa)
- **Lý do**: Customer có thể tích lũy rất nhiều đơn hàng theo thời gian — cần phân trang để tránh load toàn bộ. Clamp về trang cuối cải thiện UX khi client gửi số trang sai.
- **Lưu ý**:
  - `PaginationUtils.fetchWithClamp()` có thể tái sử dụng cho mọi list endpoint khác (Shipper orders, COD transactions...).
  - `GET /orders/my` giờ trả `ApiResponse<PagedResponse<OrderSummaryResponse>>` thay vì `List<>` — **breaking change** với client cũ nếu có.
  - `size` mặc định là **10** cho orders (khác với products là 2/5).
  - ProductService giữ nguyên logic price range riêng (không dùng `fetchWithClamp`) vì cần custom mapping 2-query.

---

### [2026-10-29] — Khắc phục lỗ hổng bảo mật & cô lập dữ liệu đơn hàng Seller
- **Thay đổi**:
  - Bổ sung ErrorCode: `SHOP_NOT_FOUND` (4020) và `ORDER_NOT_BELONG_TO_SHOP` (4021) vào `ErrorCode.java`.
  - Cập nhật `OrderRepository`: Thêm các query phân trang theo Shop `findByShopIdAndStatus(shopId, status, pageable)`, `findByShopId(shopId, pageable)`, và phân trang toàn sàn `findByStatus(status, pageable)`.
  - Nâng cấp `OrderService`:
    + `getManageOrders`: Cô lập dữ liệu đơn hàng theo Shop. Seller chỉ xem đơn thuộc Shop của mình; Admin xem toàn bộ hệ thống; hỗ trợ lọc `status` tùy chọn và phân trang với `PaginationUtils.fetchWithClamp`.
    + `markReadyToShip`: Kiểm tra nghiêm ngặt quyền sở hữu — bắt buộc đơn hàng phải thuộc Shop của Seller đang đăng nhập (Admin có toàn quyền thao tác).
    + `getOrderDetail`: Cho phép Customer xem đơn mình, Seller xem đơn Shop mình, Admin xem toàn quyền. Ném `ORDER_NOT_BELONG_TO_SHOP` nếu không có quyền.
  - Cập nhật `OrderController`:
    + `GET /orders/manage`: Nhận `Authentication`, `status` (optional), `page`, `size`; wrap kết quả trong `ApiResponse<PagedResponse<...>>`.
    + `PUT /orders/{id}/ready-to-ship`: Nhận `Authentication`, wrap kết quả trong `ApiResponse<OrderDetailResponse>`.
    + `GET /orders/{id}`: Wrap kết quả trong `ApiResponse<OrderDetailResponse>`.
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa: thêm 4020, 4021)
  - `repository/OrderRepository.java` (sửa: thêm các query theo shopId có phân trang)
  - `service/OrderService.java` (sửa: markReadyToShip, getOrderDetail, thêm getManageOrders)
  - `controller/OrderController.java` (sửa: getManageOrders, markReadyToShip, getOrderDetail)
  - `AGENTS.md` (sửa: cập nhật changelog)
- **Lý do**: Ngăn chặn rò rỉ dữ liệu giữa các Shop và ngăn chặn Seller can thiệp/đóng gói trái phép đơn hàng của Shop khác.
- **Lưu ý**:
  - `GET /orders/manage` giờ trả `ApiResponse<PagedResponse<...>>` có phân trang, mặc định 10/trang.
---

### [2026-10-29] — Bổ sung categoryId vào CreateProductRequest & liên kết Danh mục khi tạo sản phẩm
- **Thay đổi**:
  - Bổ sung `CATEGORY_NOT_FOUND` (4022) và `PRODUCT_NOT_FOUND` (4023) vào `ErrorCode.java`.
  - Cập nhật `CategoryRepository`: Thêm query `findByIdAndIsDeletedFalse(Long id)`.
  - Cập nhật `CreateProductRequest`: Bổ sung trường `categoryId` kèm validation `@NotNull(message = "Danh mục sản phẩm không được để trống")`.
  - Cập nhật `ProductService.createProduct`:
    + Inject `CategoryRepository`.
    + Kiểm tra danh mục hợp lệ, đang hoạt động (`findByIdAndIsDeletedFalse`), ném `AppException(ErrorCode.CATEGORY_NOT_FOUND)` nếu không tìm thấy.
    + Gán danh mục cho sản phẩm: `product.setCategory(category)`.
    + Thêm `@Transactional(readOnly = true)` cho `getAllProducts()` và `getProductById()`.
  - Cập nhật DTOs:
    + `ProductResponseDTO`: Bổ sung trường `categoryId` và `categoryName`, tự động ánh xạ trong `fromEntity()`.
    + `ProductDetailResponseDTO`: Bổ sung trường `categoryId` và `categoryName`, tự động ánh xạ trong `fromEntity()`.
- **File liên quan**:
  - `domain/request/product/CreateProductRequest.java` (sửa: thêm categoryId)
  - `domain/response/product/ProductResponseDTO.java` (sửa: thêm categoryId, categoryName)
  - `domain/response/product/ProductDetailResponseDTO.java` (sửa: thêm categoryId, categoryName)
  - `repository/CategoryRepository.java` (sửa: thêm findByIdAndIsDeletedFalse)
  - `service/ProductService.java` (sửa: inject CategoryRepository, validate category khi tạo)
  - `common/exception/ErrorCode.java` (sửa: thêm mã lỗi 4022, 4023)
  - `AGENTS.md` (sửa: cập nhật changelog)
- **Lý do**: Hoàn thiện liên kết sản phẩm với danh mục ngành hàng theo thiết kế bảng Categories (đã tạo migration SQL V20261028), phục vụ tìm kiếm và duyệt sản phẩm theo chuyên mục.
- **Lưu ý**:
  - `POST /products/create` (dạng multipart/form-data) hiện bắt buộc phải gửi trường `categoryId`.

---

### [2026-10-29] — Tính năng Quản lý Sản phẩm Seller: GET /seller/products
- **Thay đổi**:
  - Tạo enum `StockStatus` (`ALL`, `IN_STOCK`, `OUT_OF_STOCK`) trong `common/enums`.
  - Cập nhật `ProductRepository`: Thêm các query phân trang theo Shop, lọc theo trạng thái tồn kho và từ khóa tên:
    + `findByShopId`, `findByShopIdAndKeyword`
    + `findInStockByShopId`, `findInStockByShopIdAndKeyword` (tổng tồn kho các biến thể > 0)
    + `findOutOfStockByShopId`, `findOutOfStockByShopIdAndKeyword` (tổng tồn kho <= 0)
  - Cập nhật DTOs:
    + `ProductResponseDTO` & `ProductDetailResponseDTO`: Thêm trường `totalStock` (tổng tồn kho các biến thể) tự động tính toán trong `fromEntity()`.
  - Cập nhật `ProductService`:
    + Thêm hàm `getSellerProducts(email, stockStatus, keyword, page, size)` có phân trang, kiểm tra quyền Shop và áp dụng last page clamp.
    + Tách hàm `mapToPagedProductResponse()` dùng chung tối ưu truy vấn price ranges theo batch ID.
  - Tạo `SellerProductController` cung cấp API:
    + `GET /seller/products?stockStatus=IN_STOCK&keyword=...&page=0&size=10`
  - Cập nhật `SecurityConfig`: Cấu hình `.requestMatchers("/seller/**").hasRole("SELLER")`.
- **File liên quan**:
  - `common/enums/StockStatus.java` (tạo mới)
  - `repository/ProductRepository.java` (sửa: thêm 6 queries lọc kho)
  - `domain/response/product/ProductResponseDTO.java` (sửa: thêm totalStock)
  - `domain/response/product/ProductDetailResponseDTO.java` (sửa: thêm totalStock)
  - `service/ProductService.java` (sửa: thêm getSellerProducts, mapToPagedProductResponse)
  - `controller/SellerProductController.java` (tạo mới)
  - `config/SecurityConfig.java` (sửa: phân quyền /seller/**)
  - `AGENTS.md` (sửa: cập nhật bảng tiến độ và changelog)
- **Lý do**: Cung cấp công cụ quản lý kho hàng và danh mục sản phẩm độc quyền cho Seller theo từng Shop.
- **Lưu ý**:
  - Endpoint `GET /seller/products` yêu cầu token của tài khoản có role `SELLER` và đã có Shop.

---

### [2026-10-29] — Tính năng Cập nhật Sản phẩm: PUT /products/{id}
- **Thay đổi**:
  - Bổ sung `PRODUCT_NOT_BELONG_TO_SHOP` (4024) vào `ErrorCode.java`.
  - Khởi tạo danh sách `variants` và `images` bằng `new ArrayList<>()` trong entity `Product` để chống NullPointerException.
  - Tạo DTOs:
    + `UpdateProductVariantRequest`: Hỗ trợ cập nhật biến thể cũ (`id != null`) hoặc thêm biến thể mới (`id == null`), cập nhật giá, tồn kho, SKU, ảnh riêng.
    + `UpdateProductRequest`: Hỗ trợ cập nhật tên, mô tả, danh mục (`categoryId`), giá/kho sản phẩm đơn, xóa ảnh cũ (`deleteImageIds`), danh sách biến thể cập nhật.
  - Cập nhật `ProductService`:
    + Thêm hàm `updateProduct(email, productId, request, images, variantImages)`:
      * Kiểm tra người dùng và quyền sở hữu sản phẩm (phải thuộc Shop của Seller hoặc Admin).
      * Kiểm tra & cập nhật danh mục Category mới.
      * Cập nhật thông tin cơ bản: tên, mô tả.
      * Xóa ảnh cũ theo `deleteImageIds` (orphan removal tự động xóa DB).
      * Upload và thêm ảnh mới vào Cloudinary thư mục `shoppy_products`.
      * Cập nhật biến thể: Đồng bộ theo danh sách gửi lên (cập nhật biến thể có sẵn, thêm biến thể mới, xóa biến thể thừa, cập nhật ảnh riêng từng biến thể).
  - Cập nhật `ProductController`:
    + Hỗ trợ 2 phương thức tiêu thụ cho `PUT /products/{id}`:
      * `consumes = MediaType.MULTIPART_FORM_DATA_VALUE`: Cho phép cập nhật kèm upload thêm ảnh mới.
      * `consumes = MediaType.APPLICATION_JSON_VALUE`: Cho phép cập nhật nhanh thông tin text, giá, kho không kèm upload file.
  - Cập nhật `SecurityConfig`: Thêm rule `.requestMatchers(HttpMethod.PUT, "/products/**").hasRole("SELLER")`.
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa: thêm 4024)
  - `entity/Product.java` (sửa: khởi tạo ArrayList cho variants, images)
  - `domain/request/product/UpdateProductVariantRequest.java` (tạo mới)
  - `domain/request/product/UpdateProductRequest.java` (tạo mới)
  - `service/ProductService.java` (sửa: thêm updateProduct)
  - `controller/ProductController.java` (sửa: thêm 2 endpoint PUT /products/{id})
  - `config/SecurityConfig.java` (sửa: phân quyền PUT /products/**)
  - `AGENTS.md` (sửa: cập nhật bảng tiến độ và changelog)
- **Lý do**: Hoàn thiện nghiệp vụ cốt lõi cho Seller quản lý và chỉnh sửa sản phẩm đã đăng bán.
- **Lưu ý**:
  - Chỉ chủ sở hữu của Shop hoặc ADMIN mới có quyền cập nhật sản phẩm.

---

### [2026-10-29] — Bổ sung API thao tác nhanh (Atomic APIs) cho biến thể sản phẩm
- **Thay đổi**:
  - Bổ sung `PRODUCT_VARIANT_NOT_FOUND` (4025) vào `ErrorCode.java`.
  - Cập nhật `ProductVariantDTO`: Thêm trường `imageUrl` trực tiếp để client dễ dàng lấy ảnh thumbnail biến thể.
  - Tạo DTOs:
    + `UpdateVariantStockRequest`: Chứa `stockQuantity` (@NotNull, @Min(0)) dùng để cập nhật nhanh số lượng kho.
    + `AddProductVariantRequest`: Chứa `variantName`, `price`, `stockQuantity`, `sku`, `imageUrl` dùng để thêm nhanh biến thể mới.
  - Cập nhật `ProductService`:
    + Inject `ProductVariantRepository`.
    + `updateVariantStock(email, variantId, request)`: Kiểm tra quyền sở hữu sản phẩm của biến thể, cập nhật `stockQuantity`, lưu và trả về `ProductVariantDTO`.
    + `addVariant(email, productId, request, image)`: Kiểm tra quyền sở hữu Shop, upload ảnh biến thể lên Cloudinary (nếu có file), tự động bật `hasVariants = true`, lưu biến thể mới và trả về `ProductVariantDTO`.
  - Cập nhật `ProductController`:
    + `PATCH /products/variants/{variantId}/stock`: Endpoint cập nhật nhanh số lượng tồn kho.
    + `POST /products/{id}/variants`: Hỗ trợ cả 2 consumes: `application/json` và `multipart/form-data` (kèm upload file ảnh biến thể), trả về HTTP 201 CREATED.
  - Cập nhật `SecurityConfig`: Thêm `.requestMatchers(HttpMethod.PATCH, "/products/**").hasRole("SELLER")`.
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa: thêm 4025)
  - `domain/response/product/ProductVariantDTO.java` (sửa: thêm imageUrl)
  - `domain/request/product/UpdateVariantStockRequest.java` (tạo mới)
  - `domain/request/product/AddProductVariantRequest.java` (tạo mới)
  - `service/ProductService.java` (sửa: thêm 2 hàm nghiệp vụ)
  - `controller/ProductController.java` (sửa: thêm 3 mapping endpoint)
  - `config/SecurityConfig.java` (sửa: phân quyền PATCH /products/**)
  - `AGENTS.md` (sửa: cập nhật bảng tiến độ và changelog)
- **Lý do**: Tối ưu trải nghiệm Seller Dashboard, cho phép sửa nhanh tồn kho trong 1 click và thêm biến thể mà không cần gửi lại toàn bộ thông tin sản phẩm.
- **Lưu ý**: Cả 2 endpoint đều kiểm tra quyền sở hữu Shop của Seller hoặc Admin.

---

### [2026-10-29] — Tính năng Tạm ẩn (Deactivate) và Xóa mềm (Soft Delete) sản phẩm
- **Thay đổi**:
  - Bổ sung trường `isActive` (mặc định `true`) vào entity `Product` kèm `@PrePersist` khởi tạo.
  - Tạo DTO `ProductStatusRequest` để cập nhật trạng thái hoạt động linh hoạt.
  - Cập nhật `ProductResponseDTO` & `ProductDetailResponseDTO`: Thêm trường `isActive` và mapping từ entity.
  - Cập nhật `ProductRepository`:
    + Thêm method `findAllActive(Pageable)` (lọc `isDeleted = false AND isActive = true`).
  - Cập nhật `ProductService`:
    + `getAllProducts`: Chuyển sang gọi `productRepository.findAllActive(pageable)` giúp khách mua hàng chỉ thấy sản phẩm active và chưa bị xóa.
    + `getProductById`: Bổ sung kiểm tra `isDeleted = true` ném `PRODUCT_NOT_FOUND`.
    + `deactivateProduct(email, productId)`: Đặt `isActive = false` để tạm ẩn sản phẩm khỏi sàn.
    + `activateProduct(email, productId)`: Đặt `isActive = true` để mở bán lại.
    + `updateProductActiveStatus(email, productId, active)`: Cập nhật trạng thái linh hoạt.
    + `softDeleteProduct(email, productId)`: Đánh dấu `isDeleted = true` cho cả sản phẩm và tất cả các biến thể của nó.
  - Cập nhật `ProductController`:
    + `PATCH /products/{id}/deactivate`: Tạm ngưng kinh doanh / ẩn sản phẩm.
    + `PATCH /products/{id}/activate`: Kích hoạt lại sản phẩm.
    + `PATCH /products/{id}/status`: Cập nhật trạng thái linh hoạt với body `{ "active": boolean }`.
    + `DELETE /products/{id}`: Xóa mềm sản phẩm.
  - Cập nhật `SecurityConfig`: Thêm `.requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("SELLER")`.
- **File liên quan**:
  - `entity/Product.java` (sửa: thêm isActive)
  - `domain/request/product/ProductStatusRequest.java` (tạo mới)
  - `domain/response/product/ProductResponseDTO.java` (sửa: thêm isActive)
  - `domain/response/product/ProductDetailResponseDTO.java` (sửa: thêm isActive)
  - `repository/ProductRepository.java` (sửa: thêm findAllActive)
  - `service/ProductService.java` (sửa: thêm deactivate, activate, updateStatus, softDelete)
  - `controller/ProductController.java` (sửa: thêm 4 mapping endpoint)
  - `config/SecurityConfig.java` (sửa: phân quyền DELETE /products/**)
  - `AGENTS.md` (sửa: cập nhật bảng tiến độ và changelog)
- **Lý do**: Hoàn thiện vòng đời sản phẩm (Product Lifecycle) cho Seller: tạm ẩn khi hết hàng/nghỉ bán và xóa mềm an toàn không làm hỏng lịch sử đơn hàng.
- **Lưu ý**: Chỉ chủ sở hữu Shop của sản phẩm hoặc ADMIN mới có quyền tạm ẩn hoặc xóa sản phẩm.

---

### [2026-10-29] — Quản lý Đơn hàng Seller: API GET /seller/orders & Nghiệp vụ Seller Hủy đơn (Hoàn tiền + kho)
- **Thay đổi**:
  - Tạo DTO `CancelOrderRequest`: Chứa trường `reason` để Seller ghi nhận lý do hủy đơn.
  - Cập nhật `WalletService`:
    + Thêm phương thức `refundOrder(customerId, orderId, amount, reason)`: Cộng tiền vào `user.balance`, tạo giao dịch `WalletTransaction` (CREDIT, COMPLETED) và bắn thông báo `WALLET_DEPOSIT` cho khách hàng.
  - Cập nhật `OrderService`:
    + Thêm phương thức `cancelOrderBySeller(orderId, sellerEmail, reason)`:
      * Kiểm tra quyền sở hữu Shop (chỉ chủ Shop hoặc ADMIN).
      * Kiểm tra trạng thái đơn: Chặn hủy nếu đơn đã `SHIPPING`, `DELIVERED`, hoặc `CANCELLED`.
      * Hoàn lại số lượng tồn kho (`restoreStock`) cho tất cả biến thể trong đơn qua `findByIdWithLock`.
      * Tự động hoàn tiền vào ví khách hàng (bao gồm tiền hàng + phí ship) nếu đơn thanh toán bằng `WALLET`.
      * Cập nhật trạng thái `CANCELLED`, ghi lịch sử `OrderTracking` và gửi thông báo in-app `ORDER_STATUS_CHANGED` cho khách hàng.
  - Tạo controller `SellerOrderController`:
    + `GET /seller/orders`: Lấy danh sách đơn hàng thuộc Shop với phân trang và bộ lọc `OrderStatus` (`PROCESSING`, `READY_TO_SHIP`, `SHIPPING`, `DELIVERED`, `CANCELLED`).
    + `PUT /seller/orders/{id}/cancel`: Seller hủy đơn hết hàng đột xuất.
  - Cập nhật `OrderController`:
    + Bổ sung endpoint `PUT /orders/{id}/cancel` đồng bộ với `SellerOrderController`.
  - Cập nhật `SecurityConfig`:
    + Bổ sung rule phân quyền `.requestMatchers(HttpMethod.PUT, "/orders/*/cancel").hasRole("SELLER")`.
- **File liên quan**:
  - `domain/request/order/CancelOrderRequest.java` (tạo mới)
  - `service/WalletService.java` (sửa: thêm refundOrder)
  - `service/OrderService.java` (sửa: thêm cancelOrderBySeller)
  - `controller/SellerOrderController.java` (tạo mới)
  - `controller/OrderController.java` (sửa: thêm endpoint cancel)
  - `config/SecurityConfig.java` (sửa: phân quyền PUT cancel)
  - `AGENTS.md` (sửa: cập nhật bảng tiến độ và changelog)
- **Lý do**: Hoàn thiện tính năng xem và xử lý đơn hàng của Shop dành cho Seller, bảo đảm dòng tiền ví và số lượng tồn kho luôn chuẩn xác khi xảy ra tình huống hết hàng đột xuất.
- **Lưu ý**: Đơn hàng khi đã chuyển sang `SHIPPING` (shipper đã lấy hàng đi giao) không thể hủy qua API này.

---

### [2026-10-29] — Cập nhật thông tin Shop (PUT /shops/me) & Public Shop Profile (GET /shops/{id})
- **Thay đổi**:
  - Bổ sung `SHOP_NAME_EXISTED` (4026) và `SHOP_NOT_ACTIVE` (4027) vào `ErrorCode.java`.
  - Cập nhật `ShopRepository`:
    + Thêm `existsByNameAndIsDeletedFalseAndIdNot` (kiểm tra trùng tên khi update).
    + Thêm `findByIdAndIsDeletedFalse`.
  - Cập nhật `ProductRepository`:
    + Thêm `findActiveByShopId` (lấy sản phẩm `isDeleted = false AND isActive = true` của Shop cho khách xem).
  - Cập nhật `ProductService`:
    + Thêm `getPublicProductsByShop(shopId, page, size)` có phân trang và clamp.
  - Tạo DTOs:
    + `UpdateShopRequest`: Tên shop (@NotBlank, max 100), mô tả, googleMapUrl.
    + `ShopPublicProfileResponse`: Thông tin công khai của Shop (bảo mật, không lộ `balance` hay `userId`) kèm danh sách sản phẩm phân trang `PagedResponse<ProductResponseDTO>`.
  - Cập nhật `ShopService`:
    + Chuẩn hóa bắt lỗi sang `AppException(ErrorCode)`.
    + Thêm `updateMyShop(email, request, logo)`: Cập nhật tên, mô tả, Google Map, upload logo mới lên Cloudinary thư mục `shoppy_shop_logos`.
    + Thêm `getPublicShopProfile(shopId, page, size)`: Kiểm tra Shop tồn tại & đang active, tải danh sách sản phẩm công khai.
  - Cập nhật `ShopController`:
    + `PUT /shops/me`: Hỗ trợ cả 2 định dạng `multipart/form-data` (upload logo mới) và `application/json` (chỉ cập nhật text).
    + `GET /shops/{id}`: Endpoint Public trả về thông tin Shop và sản phẩm trang đầu.
    + `GET /shops/{id}/products`: Endpoint Public hỗ trợ khách phân trang duyệt sản phẩm riêng của Shop.
  - Cập nhật `SecurityConfig`:
    + Cho phép truy cập tự do không cần token: `GET /shops/*` và `GET /shops/*/products`.
    + Bảo vệ `GET /shops/me` và `PUT /shops/me` yêu cầu role `SELLER`.
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa: thêm 4026, 4027)
  - `repository/ShopRepository.java` (sửa: thêm methods)
  - `repository/ProductRepository.java` (sửa: thêm findActiveByShopId)
  - `service/ProductService.java` (sửa: thêm getPublicProductsByShop)
  - `domain/request/shop/UpdateShopRequest.java` (tạo mới)
  - `domain/response/shop/ShopPublicProfileResponse.java` (tạo mới)
  - `service/ShopService.java` (sửa: thêm updateMyShop, getPublicShopProfile)
  - `controller/ShopController.java` (sửa: thêm PUT /shops/me, GET /shops/{id})
  - `config/SecurityConfig.java` (sửa: phân quyền endpoint Shop)
  - `AGENTS.md` (sửa: cập nhật bảng tiến độ và changelog)
- **Lý do**: Cho phép Seller hoàn thiện hồ sơ cửa hàng chuyên nghiệp và cung cấp trang profile công khai cho người mua hàng duyệt sản phẩm theo từng Shop.
- **Lưu ý**: Khách chưa đăng nhập vẫn xem được trang `GET /shops/{id}`. Thông tin nhạy cảm (số dư ví Shop, userId chủ shop) đã được che giấu trong response public.

---

### [2026-10-29] — Bổ sung shopId & shopName vào ProductResponseDTO và ProductDetailResponseDTO
- **Thay đổi**:
  - Bổ sung 2 trường `shopId` và `shopName` vào `ProductResponseDTO` và `ProductDetailResponseDTO`.
  - Tự động map `product.getShop().getId()` và `product.getShop().getName()` trong phương thức tĩnh `fromEntity()`.
- **File liên quan**:
  - `domain/response/product/ProductResponseDTO.java` (sửa)
  - `domain/response/product/ProductDetailResponseDTO.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Giúp client (web/mobile) và người test API biết sản phẩm thuộc Shop nào khi gọi `GET /products/getAll`, `GET /products/{id}` hoặc `GET /seller/products`, từ đó dễ dàng lấy `shopId` để xem hoặc điều hướng đến trang Shop `/shops/{id}` và `/shops/{id}/products`.


---

### [2026-10-29] — Tối ưu ProductResponseDTO: Loại bỏ mảng variants, bổ sung thumbnailUrl
- **Thay đổi**:
  - Loại bỏ hoàn toàn mảng `variants` (`List<ProductVariantDTO>`) khỏi `ProductResponseDTO` (áp dụng cho toàn bộ các API danh sách sản phẩm như `GET /shops/{id}/products`, `GET /shops/{id}`, `GET /products/getAll`, `GET /seller/products`).
  - Bổ sung trường `thumbnailUrl` (ảnh đại diện sản phẩm) trực tiếp lấy từ ảnh đầu tiên của sản phẩm (`ProductImage`) hoặc ảnh biến thể đầu tiên.
  - Vẫn giữ nguyên các trường tóm tắt cần thiết: `totalStock` (tổng kho), `minPrice`, `maxPrice` (khoảng giá), `hasVariants`, `isActive`, `shopId`, `shopName`, `categoryId`, `categoryName`.
  - Giữ nguyên mảng `variants` đầy đủ cho `ProductDetailResponseDTO` (API xem chi tiết `GET /products/{id}`).
- **File liên quan**:
  - `domain/response/product/ProductResponseDTO.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Tối ưu hóa kích thước payload và hiệu năng truy vấn cho danh sách sản phẩm; tách bạch rõ ràng giữa tầng danh sách (Card Grid) và tầng chi tiết sản phẩm (Detail Page).


---

### [2026-10-29] — Bổ sung API GET /products/{id}/variants lấy riêng danh sách biến thể
- **Thay đổi**:
  - Thêm phương thức `findByProductIdAndIsDeletedFalseOrderByIdAsc` vào `ProductVariantRepository`.
  - Thêm hàm `getProductVariants(productId)` vào `ProductService`.
  - Tạo endpoint `GET /products/{id}/variants` trong `ProductController` trả về `ApiResponse<List<ProductVariantDTO>>`.
- **File liên quan**:
  - `repository/ProductVariantRepository.java` (sửa)
  - `service/ProductService.java` (sửa)
  - `controller/ProductController.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Cho phép Seller (và Client) lấy nhanh danh sách các biến thể của 1 sản phẩm kèm ID biến thể (`variantId`), số lượng kho, giá bán, SKU để phục vụ cho việc quản lý tồn kho (`PATCH /products/variants/{variantId}/stock`) mà không cần tải toàn bộ dữ liệu nặng của sản phẩm.


---

### [2026-10-29] — Thêm chức năng Tạm ẩn / Kích hoạt và Xóa mềm biến thể sản phẩm
- **Thay đổi**:
  - Bổ sung trường `isActive` (boolean, default true) vào entity `ProductVariant` kèm `@PrePersist` khởi tạo.
  - Cập nhật `ProductVariantDTO`: Thêm trường `isActive` và mapping từ entity.
  - Bổ sung `CANNOT_DELETE_LAST_VARIANT (4028)` vào `ErrorCode.java`.
  - Cập nhật `ProductVariantRepository`: Thêm method `countByProductIdAndIsDeletedFalse(Long productId)`.
  - Cập nhật `ProductRepository`: Thêm điều kiện `variant.isDeleted = false` và `v.isDeleted = false` vào các truy vấn khoảng giá và tồn kho để loại trừ biến thể đã bị xóa mềm.
  - Cập nhật `ProductService`:
    + Thêm `deactivateVariant(email, variantId)`: Tạm ngưng kinh doanh biến thể (`isActive = false`).
    + Thêm `activateVariant(email, variantId)`: Mở bán lại biến thể (`isActive = true`).
    + Thêm `updateVariantActiveStatus(email, variantId, active)`: Cập nhật trạng thái linh hoạt.
    + Thêm `softDeleteVariant(email, variantId)`: Xóa mềm biến thể (`isDeleted = true`), kiểm tra ràng buộc không cho phép xóa biến thể duy nhất còn lại của sản phẩm (ném `CANNOT_DELETE_LAST_VARIANT`).
  - Cập nhật `ProductController`:
    + `PATCH /products/variants/{variantId}/deactivate`
    + `PATCH /products/variants/{variantId}/activate`
    + `PATCH /products/variants/{variantId}/status` (Body `{ "active": boolean }`)
    + `DELETE /products/variants/{variantId}`
- **File liên quan**:
  - `entity/ProductVariant.java` (sửa)
  - `domain/response/product/ProductVariantDTO.java` (sửa)
  - `common/exception/ErrorCode.java` (sửa)
  - `repository/ProductVariantRepository.java` (sửa)
  - `repository/ProductRepository.java` (sửa)
  - `service/ProductService.java` (sửa)
  - `controller/ProductController.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Hoàn thiện toàn diện vòng đời của biến thể sản phẩm (Variant Lifecycle) cho Seller: tạm ngưng bán từng phân loại màu/size và xóa mềm an toàn các biến thể không còn kinh doanh.


---

### [2026-10-29] — Bổ sung shopId vào OrderDetailResponse & OrderSummaryResponse
- **Thay đổi**:
  - Bổ sung trường `shopId` vào DTO `OrderDetailResponse` và `OrderSummaryResponse`.
  - Tự động map `order.getShopId()` trong `fromEntity()`.
- **File liên quan**:
  - `domain/response/order/OrderResponse.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Giúp client/developer dễ dàng nhận biết đơn hàng thuộc về Cửa hàng nào để thực hiện các thao tác đóng gói hoặc kiểm tra quyền sở hữu chính xác.

---

### [2026-10-29] — Sửa lỗi HibernateException orphanRemoval khi build OrderDetailResponse
- **Thay đổi**:
  - Bổ sung hàm overload `OrderDetailResponse.fromEntity(order, items, trackings)` để nhận trực tiếp danh sách items và trackings rời rạc từ repository.
  - Refactor `buildDetailResponse()` trong `OrderService` và `ShipperService`: Xóa bỏ việc gọi `order.setItems(items)` và `order.setTrackings(trackings)`, truyền trực tiếp danh sách vào hàm mapper DTO.
- **File liên quan**:
  - `domain/response/order/OrderResponse.java` (sửa)
  - `service/OrderService.java` (sửa)
  - `service/ShipperService.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Ngăn chặn lỗi Hibernate dereferenced collection with orphan removal khi hoàn tất transaction các API cập nhật trạng thái đơn hàng (`ready-to-ship`, `cancel`, `assign-shipper`, `delivered`).
- **Lưu ý**: Giữ nguyên tính toàn vẹn của Hibernate Session, không gây LazyInitializationException.

---

### [2026-10-29] — Bổ sung Handler cho HttpMessageNotReadable & Validation trong GlobalExceptionHandler
- **Thay đổi**:
  - Bổ sung xử lý `HttpMessageNotReadableException`, `MethodArgumentNotValidException`, và `IllegalArgumentException` trong `GlobalExceptionHandler`.
  - Tự động wrap thông báo lỗi thiếu Request Body hoặc sai định dạng JSON vào đối tượng chuẩn `ApiResponse<Void>`.
- **File liên quan**:
  - `common/exception/GlobalExceptionHandler.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Chuẩn hóa phản hồi API theo `ApiResponse` ngay cả khi client gửi thiếu body hoặc sai định dạng request.

---

### [2026-10-29] — Sửa lỗi NOT NULL cột status trong shipper_cod_transactions
- **Thay đổi**:
  - Bổ sung trường `status` (kiểu `TransactionStatus`, mặc định `COMPLETED`) vào entity `ShipperCodTransaction`.
  - Chuyển `order_id` thành nullable trong `ShipperCodTransaction` và schema database để hỗ trợ các giao dịch nộp tiền `REMITTANCE` không gắn với một order cụ thể.
  - Cập nhật DTO `ShipperCodTransactionResponse` và `ShipperService`.
- **File liên quan**:
  - `entity/ShipperCodTransaction.java` (sửa)
  - `service/ShipperService.java` (sửa)
  - `domain/response/order/OrderResponse.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Sửa lỗi `PSQLException: ERROR: null value in column "status" of relation "shipper_cod_transactions" violates not-null constraint` khi shipper xác nhận giao hàng đơn COD.

---

### [2026-10-29] — Bổ sung API xem Lịch sử Giao dịch Ví (GET /users/wallet/transactions)
- **Thay đổi**:
  - Tạo DTO `WalletTransactionResponse`: ánh xạ đầy đủ `id`, `orderId`, `amount`, `type` (`CREDIT`/`DEBIT`), `status`, `createdAt`.
  - Cập nhật `WalletTransactionRepository`: bổ sung query phân trang `findByUserIdAndIsDeletedFalse` và `findByUserIdAndTypeAndIsDeletedFalse`.
  - Cập nhật `WalletService`: triển khai hàm `getMyWalletTransactions` sử dụng `PaginationUtils.fetchWithClamp` sắp xếp mới nhất lên đầu (`createdAt DESC`).
  - Cập nhật `WalletController`: cung cấp endpoint `GET /users/wallet/transactions` hỗ trợ `page`, `size`, và bộ lọc tùy chọn `type`.
- **File liên quan**:
  - `domain/response/wallet/WalletTransactionResponse.java` (tạo mới)
  - `repository/WalletTransactionRepository.java` (sửa)
  - `service/WalletService.java` (sửa)
  - `controller/WalletController.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Hoàn thiện nghiệp vụ ví tiền cho Customer xem sao kê lịch sử biến động số dư.
- **Lưu ý**: Tự động kế thừa bảo mật từ rule `/users/**` (yêu cầu authenticated), dùng chuẩn `PagedResponse<T>` và `ApiResponse<T>`.

---

### [2026-10-29] — Hoàn thiện chức năng Cập nhật Hồ sơ cá nhân (Profile) & Đổi mật khẩu
- **Thay đổi**:
  - Tạo DTO `UpdateProfileRequest` (họ tên, số điện thoại có validate Regex VN) và `ChangePasswordRequest` (mật khẩu cũ, mật khẩu mới).
  - Cập nhật `UserRepository`: Bổ sung `existsByPhoneAndIdNot` để kiểm tra trùng số điện thoại với tài khoản khác khi cập nhật.
  - Cập nhật `UserService`:
    - Triển khai `updateProfile(email, request, avatar)`: Cho phép cập nhật họ tên, số điện thoại, và upload avatar mới lên Cloudinary CDN (`shoppy_avatars`).
    - Triển khai `changePassword(email, request)`: Kiểm tra khớp mật khẩu cũ qua BCrypt `passwordEncoder.matches()` trước khi mã hóa lưu mật khẩu mới.
    - Thay thế `RuntimeException` bằng `AppException(ErrorCode.USER_NOT_FOUND)` trong `getUserByEmail()`.
  - Cập nhật `UserController`:
    - `PUT /users/profile` (`multipart/form-data`): Cập nhật thông tin kèm upload ảnh đại diện mới.
    - `PUT /users/profile` (`application/json`): Cập nhật thông tin text (họ tên, số điện thoại) không upload file.
    - `PUT /users/change-password` (`application/json`): Đổi mật khẩu cho người dùng hiện tại.
- **File liên quan**:
  - `domain/request/user/UpdateProfileRequest.java` (tạo mới)
  - `domain/request/user/ChangePasswordRequest.java` (tạo mới)
  - `repository/UserRepository.java` (sửa)
  - `service/UserService.java` (sửa)
  - `controller/UserController.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Hoàn thiện toàn bộ các tính năng quản lý hồ sơ cá nhân của người dùng (Customer, Seller, Shipper).
- **Lưu ý**: Endpoint được bảo vệ tự động bởi rule Spring Security `/users/**` (yêu cầu authenticated).

---

### [2026-10-29] — Luồng Quên Mật Khẩu & Đặt Lại Mật Khẩu qua OTP (Email + Redis)
- **Thay đổi**: Triển khai giải pháp Quên mật khẩu tối ưu và thân thiện bằng mã OTP 6 chữ số gửi qua Email:
  - Bổ sung 3 mã lỗi chuẩn vào `ErrorCode`:
    - `INVALID_OTP` (4029): "Mã OTP không chính xác" (BAD_REQUEST)
    - `EXPIRED_OTP` (4030): "Mã OTP đã hết hạn hoặc chưa được gửi" (BAD_REQUEST)
    - `OTP_COOLDOWN` (4031): "Vui lòng đợi 60 giây trước khi yêu cầu gửi lại mã OTP" (TOO_MANY_REQUESTS)
  - Tạo 2 Request DTOs:
    - `ForgotPasswordRequest`: kiểm tra `@NotBlank`, `@Email`.
    - `ResetPasswordRequest`: kiểm tra `@NotBlank`, `@Pattern(regexp = "^[0-9]{6}$")` cho mã OTP 6 số, `@Size(min = 6, max = 32)` cho mật khẩu mới.
  - Cập nhật `EmailService`:
    - Bổ sung `@Async public void sendOtpEmail(String toEmail, String fullName, String otp)` gửi mẫu email HTML có giao diện Shoppy với hộp số OTP nổi bật, thời hạn 5 phút.
  - Cập nhật `AuthService`:
    - Inject `StringRedisTemplate` và `EmailService`.
    - Dùng prefix Redis: `otp:reset_pwd:{email}` (TTL 5 phút) và `cooldown:reset_pwd:{email}` (TTL 60 giây).
    - Triển khai `forgotPassword(ForgotPasswordRequest request)`: kiểm tra tài khoản tồn tại và ACTIVE, kiểm tra cooldown chống spam 60s, sinh OTP an toàn bằng `SecureRandom`, lưu Redis và gửi email bất đồng bộ.
    - Triển khai `resetPassword(ResetPasswordRequest request)`: kiểm tra user, đối chiếu OTP trong Redis, mã hóa mật khẩu mới bằng BCrypt và lưu DB, xóa sạch OTP & cooldown trong Redis.
  - Cập nhật `AuthController`:
    - `POST /auth/forgot-password`: Nhận email, trả về `ApiResponse.success(...)`.
    - `POST /auth/reset-password`: Nhận email, otp và newPassword, trả về `ApiResponse.success(...)`.
  - Cập nhật `SecurityConfig`:
    - Bổ sung `/auth/forgot-password` và `/auth/reset-password` vào danh sách `permitAll()` công khai.
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa)
  - `domain/request/auth/ForgotPasswordRequest.java` (tạo mới)
  - `domain/request/auth/ResetPasswordRequest.java` (tạo mới)
  - `service/EmailService.java` (sửa)
  - `service/AuthService.java` (sửa)
  - `controller/AuthController.java` (sửa)
  - `config/SecurityConfig.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Cung cấp cơ chế khôi phục mật khẩu bảo mật cao, chống spam (rate limit 60s), trải nghiệm tiện lợi cho người dùng di động và web.
- **Lưu ý**: Cần đảm bảo Redis và Brevo SMTP đang chạy ổn định. Mã OTP được log ra console ở môi trường dev để tiện kiểm thử.

---

### [2026-10-29] — Tính năng Hủy Đơn Hàng cho Customer (khi Shop chưa chuẩn bị xong)
- **Thay đổi**: Triển khai nghiệp vụ cho phép khách hàng chủ động hủy đơn hàng:
  - Bổ sung 3 ErrorCode mới vào `ErrorCode`:
    - `ORDER_NOT_BELONG_TO_CUSTOMER` (4032): "Bạn không có quyền thao tác trên đơn hàng của người khác" (FORBIDDEN)
    - `ORDER_ALREADY_PREPARED` (4033): "Người bán đã chuẩn bị xong đơn hàng hoặc đang giao, không thể hủy" (BAD_REQUEST)
    - `ORDER_ALREADY_CANCELLED` (4034): "Đơn hàng này đã bị hủy trước đó" (BAD_REQUEST)
  - Cập nhật `OrderService`:
    - Triển khai `cancelOrderByCustomer(Long orderId, String customerEmail, String reason)`:
      1. Kiểm tra đơn hàng thuộc quyền sở hữu của Customer (`customerId == customer.getId()`).
      2. Kiểm tra trạng thái đơn: Chặn hủy nếu đã ở `READY_TO_SHIP` (người bán đã chuẩn bị xong), `SHIPPING`, `DELIVERED`, hoặc `CANCELLED`. Chỉ cho phép hủy khi ở `PENDING`, `WAITING_FOR_PAYMENT`, `PROCESSING`.
      3. Tự động hoàn kho (`restoreStock`) nếu đơn hàng đã được giữ/trừ kho (`PROCESSING`, `WAITING_FOR_PAYMENT`).
      4. Tự động hoàn tiền vào ví Shoppy (`walletService.refundOrder`) nếu đơn đã thanh toán bằng ví `WALLET`.
      5. Cập nhật `OrderStatus.CANCELLED` và lưu DB.
      6. Thêm bản ghi `OrderTracking` với `TrackingStatus.CANCELLED`.
      7. Bắn thông báo in-app cho Customer xác nhận hủy thành công, và bắn thông báo cho Seller (chủ shop) biết đơn đã bị khách hủy kèm lý do.
    - Bổ sung hàm điều phối `cancelOrder(Long orderId, String userEmail, String reason)` nhận diện linh hoạt: Customer đặt đơn -> chạy luồng khách hủy; Seller của Shop / Admin -> chạy luồng người bán hủy.
  - Cập nhật `OrderController`:
    - `PUT /orders/{id}/cancel`: Sử dụng hàm `cancelOrder` điều phối thông minh.
  - Cập nhật `SecurityConfig`:
    - Cấu hình `.requestMatchers(HttpMethod.PUT, "/orders/*/cancel").authenticated()` cho phép người dùng đã đăng nhập (cả Customer lẫn Seller) gọi endpoint.
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa)
  - `service/OrderService.java` (sửa)
  - `controller/OrderController.java` (sửa)
  - `config/SecurityConfig.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Đáp ứng trọn vẹn yêu cầu nghiệp vụ thương mại điện tử, bảo vệ quyền lợi người mua khi đổi ý trước khi hàng được đóng gói, đồng thời bảo đảm an toàn kho hàng và tiền ví.
- **Lưu ý**: Một khi đơn đã chuyển sang `READY_TO_SHIP` (Shop đã đóng gói xong), khách hàng sẽ không thể tự hủy qua API này.

---

### [2026-10-29] — Module Quản lý Người dùng Toàn sàn cho Admin (Admin User Management)
- **Thay đổi**: Triển khai trọn bộ tính năng quản trị người dùng dành riêng cho vai trò `ADMIN`:
  - Bổ sung 3 ErrorCode mới vào `ErrorCode`:
    - `CANNOT_BLOCK_SELF` (4035): "Bạn không thể tự khóa tài khoản của chính mình" (BAD_REQUEST)
    - `CANNOT_CHANGE_SELF_ROLE` (4036): "Bạn không thể tự thay đổi vai trò của chính mình" (BAD_REQUEST)
    - `CANNOT_BLOCK_ADMIN` (4037): "Không thể khóa tài khoản quản trị viên" (BAD_REQUEST)
  - Cập nhật `UserRepository`: Bổ sung JPQL query `findUsersByAdmin` hỗ trợ tìm kiếm linh hoạt (email, fullName, phone) kết hợp lọc theo `UserRole`, `UserStatus` và phân trang với `Pageable`.
  - Cập nhật `OrderRepository`: Thêm `countByCustomerId(Long customerId)` để thống kê số đơn hàng đã đặt của người dùng.
  - Tạo các DTOs:
    - `UpdateUserStatusRequest`: validate `@NotNull UserStatus`, `@Size(max = 255) reason`.
    - `UpdateUserRoleRequest`: validate `@NotNull UserRole`.
    - `AdminUserDetailResponse`: DTO chi tiết hiển thị thông tin user, số dư ví, thông tin Shop liên kết (nếu là Seller), hãng vận chuyển (nếu là Shipper) và số đơn hàng.
  - Tạo `AdminUserService`:
    - `getAllUsers`: Phân trang tự động với `PaginationUtils.fetchWithClamp`, hỗ trợ tìm kiếm từ khóa và bộ lọc trạng thái/vai trò.
    - `getUserDetail`: Xem chi tiết hồ sơ tài khoản và các thực thể liên kết.
    - `updateUserStatus`: Khóa (`BLOCKED`), mở khóa (`ACTIVE`), tạm ngưng (`INACTIVE`); tự động tạm dừng Shop nếu khóa tài khoản Seller; gửi Notification in-app cho người dùng; ngăn Admin tự khóa chính mình hoặc khóa Admin khác.
    - `updateUserRole`: Thay đổi vai trò giữa CUSTOMER, SELLER, SHIPPER, ADMIN; gửi Notification; ngăn Admin tự hạ quyền của chính mình.
  - Tạo `AdminUserController`: Cung cấp các endpoint RESTful chuẩn:
    - `GET /admin/users` (hỗ trợ params: `search`, `role`, `status`, `page`, `size`)
    - `GET /admin/users/{id}`
    - `PATCH /admin/users/{id}/status`
    - `PATCH /admin/users/{id}/role`
  - Quyền truy cập: Được bảo vệ tự động bởi cấu hình `.requestMatchers("/admin/**").hasRole("ADMIN")` trong `SecurityConfig`.
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa)
  - `repository/UserRepository.java` (sửa)
  - `repository/OrderRepository.java` (sửa)
  - `domain/request/admin/UpdateUserStatusRequest.java` (tạo mới)
  - `domain/request/admin/UpdateUserRoleRequest.java` (tạo mới)
  - `domain/response/admin/AdminUserDetailResponse.java` (tạo mới)
  - `service/AdminUserService.java` (tạo mới)
  - `controller/AdminUserController.java` (tạo mới)
  - `AGENTS.md` (sửa)
- **Lý do**: Cung cấp công cụ quản trị kiểm soát người dùng, xử lý vi phạm, bảo vệ an ninh và vận hành trơn tru toàn hệ thống Shoppy.
- **Lưu ý**: Chỉ tài khoản có `role = ADMIN` mới có thể gọi các API thuộc `/admin/**`.

---

### [2026-10-29] — Module Quản lý Danh mục Toàn sàn & Quản lý Cửa hàng cho Admin
- **Thay đổi**: Triển khai 2 module quản trị cốt lõi trên sàn Shoppy:
  1. **Quản lý Danh mục Sản phẩm (Category Management)**:
     - Bổ sung các ErrorCode vào `ErrorCode`:
       - `CATEGORY_NAME_EXISTED` (4039): "Tên danh mục đã tồn tại, vui lòng chọn tên khác"
       - `CATEGORY_SLUG_EXISTED` (4040): "Đường dẫn slug của danh mục đã tồn tại"
       - `CATEGORY_PARENT_INVALID` (4041): "Danh mục cha không hợp lệ (không thể chọn chính mình hoặc danh mục con làm cha)"
       - `CANNOT_DELETE_CATEGORY_HAS_PRODUCTS` (4042): "Không thể xóa danh mục đang có sản phẩm kinh doanh"
     - Cập nhật `CategoryRepository`: Thêm JPQL `findCategoriesByAdmin` (tìm kiếm + phân trang), kiểm tra trùng tên/slug khi update.
     - Tạo DTOs: `CategoryRequest`, `CategoryResponse` (hỗ trợ cây đa cấp `children`).
     - Tạo `CategoryService`:
       - `getAllCategoriesTree()`: Lấy cây danh mục phân cấp đa tầng (chạy gom nhóm Map O(N), tránh N+1 query).
       - `getAllCategoriesPaged(search, page, size)`: Phân trang danh mục phẳng cho Admin.
       - `getCategoryById(id)`: Xem chi tiết danh mục.
       - `createCategory(request, image)`: Tự động chuẩn hóa slug tiếng Việt, upload ảnh lên Cloudinary (`shoppy_categories`), kiểm tra trùng lặp.
       - `updateCategory(id, request, image)`: Chặn tạo chu trình cha-con khép kín (`isDescendant`), cập nhật ảnh và thông tin.
       - `deleteCategory(id)`: Soft delete, chặn xóa khi danh mục đang có sản phẩm kinh doanh, nâng cấp danh mục con lên cha.
     - Tạo `CategoryController`: Public API `GET /categories` (lấy cây danh mục) và `GET /categories/{id}`.
     - Tạo `AdminCategoryController`: Admin API `GET`, `POST` (JSON & multipart), `PUT` (JSON & multipart), `DELETE /admin/categories/{id}`.
     - Cập nhật `SecurityConfig`: Cho phép `GET /categories/**` truy cập tự do không cần token.
  2. **Quản lý Cửa hàng Toàn sàn (Admin Shop Management)**:
     - Bổ sung ErrorCode `INVALID_SHOP_STATUS` (4043): "Trạng thái Cửa hàng không hợp lệ (phải là ACTIVE, INACTIVE hoặc BLOCKED)".
     - Cập nhật `ShopRepository`: Thêm `findShopsByAdmin` (tìm kiếm tên/mô tả, lọc trạng thái, phân trang).
     - Cập nhật `ProductRepository`: Thêm `countByShopIdAndIsDeletedFalse(shopId)`, `existsByCategoryIdAndIsDeletedFalse(categoryId)`.
     - Cập nhật `OrderRepository`: Thêm `countByShopId(shopId)`.
     - Tạo DTOs: `UpdateShopStatusRequest`, `AdminShopDetailResponse`.
     - Tạo `AdminShopService`:
       - `getAllShops(search, status, page, size)`: Xem danh sách shop toàn sàn có phân trang, lọc trạng thái, tìm kiếm.
       - `getShopDetail(id)`: Xem chi tiết shop, chủ sở hữu, số lượng sản phẩm đang bán, tổng số đơn hàng đã nhận.
       - `updateShopStatus(id, request, adminEmail)`: Thay đổi trạng thái shop (`ACTIVE`, `INACTIVE`, `BLOCKED`), tự động bắn thông báo in-app cho chủ shop kèm lý do.
     - Tạo `AdminShopController`: `GET /admin/shops`, `GET /admin/shops/{id}`, `PATCH /admin/shops/{id}/status`.
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa)
  - `repository/CategoryRepository.java` (sửa)
  - `repository/ShopRepository.java` (sửa)
  - `repository/ProductRepository.java` (sửa)
  - `repository/OrderRepository.java` (sửa)
  - `domain/request/category/CategoryRequest.java` (tạo mới)
  - `domain/response/category/CategoryResponse.java` (tạo mới)
  - `domain/request/admin/UpdateShopStatusRequest.java` (tạo mới)
  - `domain/response/admin/AdminShopDetailResponse.java` (tạo mới)
  - `service/CategoryService.java` (tạo mới)
  - `service/AdminShopService.java` (tạo mới)
  - `controller/CategoryController.java` (tạo mới)
  - `controller/AdminCategoryController.java` (tạo mới)
  - `controller/AdminShopController.java` (tạo mới)
  - `config/SecurityConfig.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Chuẩn hóa danh mục ngành hàng cho sàn thương mại điện tử, đồng thời cung cấp công cụ giám sát và xử lý vi phạm đối với các Shop trên sàn.
- **Lưu ý**: Khách hàng và người bán có thể gọi `GET /categories` mà không cần token để xem danh mục khi mua sắm hoặc đăng sản phẩm.

---

### [2026-10-29] — Đồng bộ Logic Ẩn Sản Phẩm & Chặn Đặt Hàng khi Shop bị Khóa/Tạm Ngưng
- **Thay đổi**: Đảm bảo tính toàn vẹn nghiệp vụ sàn TMĐT khi một Cửa hàng bị Admin khóa (`BLOCKED`) hoặc tạm ngưng (`INACTIVE`):
  1. `ProductRepository`:
     - Cập nhật JPQL `findAllActive` và `findActiveByShopId`: Bổ sung điều kiện bắt buộc `AND p.shop.isDeleted = false AND p.shop.status = 'ACTIVE'`. Đảm bảo sản phẩm của Shop bị khóa sẽ tự động biến mất khỏi trang chủ và danh sách sản phẩm toàn sàn.
  2. `ProductService`:
     - Cập nhật `getProductById` và `getProductVariants`: Kiểm tra nếu `product.getShop()` bị xóa hoặc có trạng thái khác `ACTIVE` (`INACTIVE`, `BLOCKED`) ➔ lập tức ném lỗi `AppException(ErrorCode.SHOP_NOT_ACTIVE)`. Khách hàng bấm vào link sản phẩm cũ sẽ nhận thông báo cửa hàng đang tạm ngưng.
  3. `OrderService`:
     - Cập nhật `createOrder`: Kiểm tra trạng thái Shop sở hữu của từng sản phẩm trong đơn, nếu Shop không `ACTIVE` hoặc bị xóa ➔ lập tức chặn tạo đơn hàng và trả lỗi `AppException(ErrorCode.SHOP_NOT_ACTIVE)`. Đồng thời chặn đặt hàng các sản phẩm đã bị ẩn/xóa.
- **File liên quan**:
  - `repository/ProductRepository.java` (sửa)
  - `service/ProductService.java` (sửa)
  - `service/OrderService.java` (sửa)
  - `AGENTS.md` (sửa)
- **Lý do**: Tránh tình trạng khách hàng vẫn nhìn thấy hoặc đặt mua sản phẩm từ các shop vi phạm đã bị Admin cấm kinh doanh.
- **Lưu ý**: Khi Admin mở khóa lại Shop (`status = 'ACTIVE'`), toàn bộ sản phẩm đang active của shop sẽ tự động hiển thị lại bình thường trên sàn mà không cần chỉnh sửa từng sản phẩm.

---

### [2026-10-29] — Thêm Status cho Category & Đồng Bộ Logic Ẩn Sản Phẩm Khi Category Bị Deactive
- **Thay đổi**:
  1. `Category Entity`:
     - Bổ sung trường `status` (`VARCHAR(20)`, mặc định `"ACTIVE"`, hỗ trợ giá trị `"ACTIVE"` hoặc `"INACTIVE"`).
  2. `ErrorCode`:
     - Bổ sung `CATEGORY_NOT_ACTIVE` (4044: "Danh mục sản phẩm hiện đang tạm ngưng hoạt động").
     - Bổ sung `INVALID_CATEGORY_STATUS` (4045: "Trạng thái danh mục không hợp lệ (phải là ACTIVE hoặc INACTIVE)").
  3. `DTO & API Quản trị`:
     - Tạo `UpdateCategoryStatusRequest` với validation `@NotBlank`.
     - Cập nhật `CategoryRequest` và `CategoryResponse` mang trường `status`.
     - Bổ sung API `PATCH /admin/categories/{id}/status` cho phép Admin chuyển đổi nhanh trạng thái danh mục.
     - Cập nhật `GET /admin/categories` hỗ trợ query param `status` để lọc danh mục theo trạng thái.
  4. `CategoryRepository & Public API`:
     - `findAllActiveOrderByDisplayOrder`: Bổ sung điều kiện `c.status = 'ACTIVE'` để người mua/bán chỉ nhìn thấy các danh mục đang hoạt động.
  5. `Đồng bộ ẩn Sản phẩm (ProductRepository & ProductService)`:
     - `ProductRepository`: JPQL `findAllActive` và `findActiveByShopId` thêm điều kiện `AND (p.category IS NULL OR (p.category.isDeleted = false AND p.category.status = 'ACTIVE'))`. Mọi sản phẩm thuộc danh mục bị `INACTIVE` sẽ tự động ẩn khỏi trang chủ sàn và trang chi tiết cửa hàng.
     - `ProductService`: `getProductById` và `getProductVariants` kiểm tra nếu `product.getCategory()` bị xóa hoặc `INACTIVE` ➔ ném `AppException(ErrorCode.CATEGORY_NOT_ACTIVE)`.
  6. `Chặn đặt hàng (OrderService)`:
     - `createOrder`: Kiểm tra trạng thái danh mục của sản phẩm trong đơn, nếu danh mục bị deactive (`INACTIVE`) hoặc bị xóa ➔ lập tức chặn tạo đơn và trả lỗi `AppException(ErrorCode.CATEGORY_NOT_ACTIVE)`.
- **File liên quan**:
  - `entity/Category.java` (sửa: thêm status)
  - `common/exception/ErrorCode.java` (sửa: thêm 4044, 4045)
  - `domain/request/category/CategoryRequest.java` (sửa: thêm status)
  - `domain/request/category/UpdateCategoryStatusRequest.java` (tạo mới)
  - `domain/response/category/CategoryResponse.java` (sửa: thêm status)
  - `repository/CategoryRepository.java` (sửa: lọc status ACTIVE, lọc admin theo status)
  - `service/CategoryService.java` (sửa: xử lý status, thêm updateCategoryStatus)
  - `controller/AdminCategoryController.java` (sửa: thêm PATCH status, hỗ trợ lọc status)
  - `repository/ProductRepository.java` (sửa: bổ sung kiểm tra category.status = 'ACTIVE')
  - `service/ProductService.java` (sửa: kiểm tra category status khi xem chi tiết/biến thể)
  - `service/OrderService.java` (sửa: kiểm tra category status khi tạo đơn hàng)
  - `AGENTS.md` (sửa: cập nhật changelog)
- **Lý do**: Đáp ứng yêu cầu nghiệp vụ quản trị danh mục toàn sàn: khi một danh mục tạm ngưng/bảo trì, tất cả sản phẩm thuộc danh mục đó phải lập tức ẩn khỏi khách hàng và không thể đặt mua.
- **Lưu ý**: Khi Admin chuyển trạng thái danh mục trở lại `ACTIVE`, mọi sản phẩm đang hoạt động của danh mục đó sẽ tự động hiển thị lại trên toàn sàn mà không cần chỉnh sửa từng sản phẩm.

---

### [2026-10-29] — Cơ chế Bảo vệ Tài khoản & Vai trò Admin (Chặn Deactive ADMIN)
- **Thay đổi**:
  - Bổ sung & chuẩn hóa các mã lỗi trong `ErrorCode`:
    + `CANNOT_BLOCK_SELF` (4035): *"Bạn không thể tự khóa hoặc vô hiệu hóa tài khoản của chính mình"*
    + `CANNOT_CHANGE_SELF_ROLE` (4036): *"Bạn không thể tự thay đổi vai trò của chính mình"*
    + `CANNOT_BLOCK_ADMIN` (4037): *"Không thể khóa hoặc vô hiệu hóa tài khoản quản trị viên"*
    + `CANNOT_DEMOTE_ADMIN` (4038): *"Không thể tước hoặc hạ vai trò của tài khoản quản trị viên"*
  - Cập nhật `AdminUserService`:
    + `updateUserStatus`: Chặn không cho Admin tự deactive (chuyển sang `BLOCKED`, `INACTIVE`, `PENDING`) tài khoản của chính mình (`ErrorCode.CANNOT_BLOCK_SELF`). Đồng thời, chặn không cho bất kỳ ai deactive tài khoản có vai trò `ADMIN` (`ErrorCode.CANNOT_BLOCK_ADMIN`). Chỉ cho phép cập nhật nếu giữ trạng thái `ACTIVE`.
    + `updateUserRole`: Chặn không cho Admin tự thay đổi/hạ vai trò của chính mình (`ErrorCode.CANNOT_CHANGE_SELF_ROLE`). Đồng thời, chặn không cho phép hạ vai trò của bất kỳ tài khoản nào đang là `ADMIN` xuống các vai trò khác (`ErrorCode.CANNOT_DEMOTE_ADMIN`).
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa: cập nhật message 4035, 4037 và thêm 4038)
  - `service/AdminUserService.java` (sửa: kiểm tra status và role bảo vệ ADMIN)
  - `AGENTS.md` (sửa: cập nhật changelog)
- **Lý do**: Ngăn chặn tình trạng hệ thống bị mất quyền quản trị viên do vô tình hoặc cố ý tự deactive tài khoản admin, hoặc hạ quyền các tài khoản quản trị viên tối cao.
- **Lưu ý**: Admin vẫn có thể nâng quyền một user thông thường (`CUSTOMER`, `SELLER`, `SHIPPER`) lên `ADMIN`, nhưng một khi đã là `ADMIN` thì không thể bị hạ quyền hoặc vô hiệu hóa thông qua API quản trị.

---

### [2026-10-29] — Quản lý Đối tác Vận chuyển (Admin) & Chặn Shipper Nhận Hàng khi Hãng bị Deactive
- **Thay đổi**:
  1. `ErrorCode`:
     - Bổ sung:
       + `SHIPPING_COMPANY_CODE_EXISTED` (4046): *"Mã đơn vị vận chuyển đã tồn tại, vui lòng chọn mã khác"*
       + `SHIPPING_COMPANY_NAME_EXISTED` (4047): *"Tên đơn vị vận chuyển đã tồn tại, vui lòng chọn tên khác"*
       + `INVALID_SHIPPING_COMPANY_STATUS` (4048): *"Trạng thái đơn vị vận chuyển không hợp lệ (phải là ACTIVE hoặc INACTIVE)"*
  2. `Entity ShippingCompany`:
     - Chuyển sang `@SuperBuilder`, đặt `columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'"` cho cột `status` để bảo đảm tính tương thích kế thừa `BaseEntity` và an toàn DDL.
  3. `DTOs`:
     - Tạo `CreateShippingCompanyRequest`, `UpdateShippingCompanyRequest`, `UpdateShippingCompanyStatusRequest` trong `domain/request/shipping/`.
  4. `ShippingCompanyRepository`:
     - Bổ sung `findByStatusAndIsDeletedFalseOrderByNameAsc`, `findByIdAndIsDeletedFalse`.
     - Bổ sung các phương thức kiểm tra trùng `code` và `name` (`existsByCodeAndIsDeletedFalse`, `existsByNameAndIsDeletedFalse`...).
     - Bổ sung query JPQL phân trang và tìm kiếm cho Admin: `findShippingCompaniesByAdmin`.
  5. `ShippingCompanyService`:
     - Quản lý toàn bộ nghiệp vụ: Xem danh sách phân trang (Admin), xem chi tiết, thêm mới kèm upload logo Cloudinary (`shoppy_shipping_logos`), sửa thông tin, chuyển đổi trạng thái `status` (`ACTIVE` / `INACTIVE`), xóa mềm (`isDeleted = true`).
  6. `Controllers`:
     - Tạo `AdminShippingCompanyController` (`/admin/shipping-companies`): Cung cấp đầy đủ các endpoint CRUD dành cho Admin (hỗ trợ cả `multipart/form-data` lẫn `application/json`).
     - Tái cấu trúc `ShippingCompanyController` (Public): Gọi qua `ShippingCompanyService.getAllActiveShippingCompanies()` trả về danh sách đối tác đang hoạt động.
  7. `Đồng bộ Nghiệp vụ Shipper & Đơn hàng`:
     - `ShipperService.validateShipperHasCompany`: Bổ sung kiểm tra hãng vận chuyển của shipper phải đang tồn tại và có `status = 'ACTIVE'`. Nếu hãng bị deactive (`INACTIVE`), shipper **lập tức bị chặn nhận đơn hàng** (`assignShipper`) và không thể xem danh sách đơn sẵn sàng nhận (`getAvailableOrders`) với mã lỗi `ErrorCode.SHIPPING_COMPANY_NOT_ACTIVE`.
     - `OrderService.createOrder`: Bổ sung kiểm tra khi khách đặt hàng, hãng vận chuyển được chọn phải đang `ACTIVE`, nếu không sẽ ném `AppException(ErrorCode.SHIPPING_COMPANY_NOT_ACTIVE)`.
- **File liên quan**:
  - `common/exception/ErrorCode.java` (sửa: thêm 4046, 4047, 4048)
  - `entity/ShippingCompany.java` (sửa: SuperBuilder, columnDefinition status)
  - `domain/request/shipping/CreateShippingCompanyRequest.java` (tạo mới)
  - `domain/request/shipping/UpdateShippingCompanyRequest.java` (tạo mới)
  - `domain/request/shipping/UpdateShippingCompanyStatusRequest.java` (tạo mới)
  - `repository/ShippingCompanyRepository.java` (sửa)
  - `service/ShippingCompanyService.java` (tạo mới)
  - `controller/AdminShippingCompanyController.java` (tạo mới)
  - `controller/ShippingCompanyController.java` (sửa)
  - `service/ShipperService.java` (sửa: chặn nhận đơn khi hãng inactive)
  - `service/OrderService.java` (sửa: chặn tạo đơn khi hãng inactive)
  - `AGENTS.md` (sửa)
- **Lý do**: Hoàn thiện tính năng quản trị logistics toàn sàn: cho phép Admin quản lý các đơn vị vận chuyển đối tác, và lập tức ngừng quyền tiếp nhận đơn hàng của Shipper trực thuộc khi hãng vận chuyển đó bị tạm dừng dịch vụ.
- **Lưu ý**: Khi Admin mở kích hoạt lại hãng vận chuyển (`status = 'ACTIVE'`), các Shipper trực thuộc hãng sẽ lập tức có thể tiếp tục nhận đơn hàng bình thường mà không cần thao tác gì thêm.

---

## 🔧 Hướng dẫn môi trường phát triển

### Chạy local:
```bash
# 1. Khởi động PostgreSQL + Redis bằng Docker
docker-compose up -d

# 2. Cấu hình file .env (điền các giá trị thực)
# Xem mẫu trong .env.example

# 3. Chạy ứng dụng
./mvnw spring-boot:run
```

### Biến môi trường cần thiết (xem `.env`):
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION`
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- `REDIS_HOST`, `REDIS_PORT`

