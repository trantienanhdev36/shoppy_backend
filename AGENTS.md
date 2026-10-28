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
| **User**          | Xem thông tin profile                         | ✅ Hoàn thành |
|                   | Cập nhật profile                              | 🔄 Đang làm  |
| **Shop**          | Đăng ký shop (SELLER)                         | ✅ Hoàn thành |
|                   | Xem thông tin shop                            | ✅ Hoàn thành |
| **Product**       | Tạo sản phẩm + upload ảnh (Cloudinary)        | ✅ Hoàn thành |
|                   | Biến thể sản phẩm (variant: màu, size)        | ✅ Hoàn thành |
|                   | Xem danh sách & chi tiết sản phẩm             | ✅ Hoàn thành |
|                   | Phân trang danh sách sản phẩm (5/trang)       | ✅ Hoàn thành |
|                   | Cập nhật / Xóa sản phẩm                       | ❌ Chưa làm  |
| **Order**         | Tạo đơn hàng                                  | ✅ Hoàn thành |
|                   | Xem đơn hàng của user                         | ✅ Hoàn thành |
|                   | Cập nhật trạng thái đơn hàng                  | ✅ Hoàn thành |
|                   | Hủy đơn hàng                                  | 🔄 Đang làm  |
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
| **Admin**         | Quản lý người dùng                            | ❌ Chưa làm  |
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

