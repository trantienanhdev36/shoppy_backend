shopee-backend/
├── .github/
│   └── workflows/                # CI/CD Pipeline
├── docker/
│   ├── postgres/
│   │   └── init.sql              # Script khởi tạo database nếu cần
│   └── docker-compose.yml        # PostgreSQL 16 container setup
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── shopee/
│   │   │           └── backend/
│   │   │               ├── ShopeeBackendApplication.java
│   │   │               │
│   │   │               ├── config/                         # Cấu hình Spring Beans
│   │   │               │   ├── SecurityConfig.java         # Spring Security & JWT Filter Chain
│   │   │               │   ├── JwtAuthenticationFilter.java# Filter xác thực JWT token
│   │   │               │   ├── CloudinaryConfig.java       # Kết nối Cloudinary CDN
│   │   │               │   ├── JpaAuditingConfig.java      # Tự động gán created_at, updated_at
│   │   │               │   └── SwaggerConfig.java          # OpenAPI / Swagger UI
│   │   │               │
│   │   │               ├── common/                         # Dùng chung toàn hệ thống
│   │   │               │   ├── annotation/                 # Custom Annotations (vd: @ValidEnum)
│   │   │               │   ├── dto/
│   │   │               │   │   ├── ApiResponse.java        # Standardized Response Wrapper <T>
│   │   │               │   │   └── PageResponse.java       # Response phân trang (wrapper cho List<Entity>)
│   │   │               │   ├── enums/                      # Toàn bộ Enums
│   │   │               │   │   ├── UserRole.java           # ROLE_CUSTOMER, ROLE_SELLER, ROLE_SHIPPER, ROLE_ADMIN
│   │   │               │   │   ├── UserStatus.java         # ACTIVE, INACTIVE, BLOCKED
│   │   │               │   │   ├── OrderStatus.java        # PENDING, PROCESSING, READY_TO_SHIP...
│   │   │               │   │   ├── TrackingStatus.java     # PICKED_UP, IN_TRANSIT, DELIVERED...
│   │   │               │   │   ├── TransactionStatus.java  # PENDING, COMPLETED, FAILED, CANCELLED
│   │   │               │   │   └── PaymentMethod.java      # WALLET
│   │   │               │   ├── exception/
│   │   │               │   │   ├── AppException.java       # Custom Exception
│   │   │               │   │   ├── ErrorCode.java          # Enum quản lý mã lỗi & message
│   │   │               │   │   └── GlobalExceptionHandler.java # @ControllerAdvice xử lý lỗi tập trung
│   │   │               │   └── util/
│   │   │                   ├── JwtUtils.java               # Xử lý Token JWT
│   │   │                   └── SecurityUtils.java          # Lấy current user ID từ SecurityContext
│   │   │               │
│   │   │               ├── controller/                     # TẦNG CONTROLLER (Trả về ApiResponse<Entity>)
│   │   │               │   ├── AuthController.java         # /api/v1/auth
│   │   │               │   ├── UserController.java         # Quản lý thông tin user
│   │   │               │   ├── UserAddressController.java  # Quản lý địa chỉ
│   │   │               │   ├── ShopController.java         # Quản lý gian hàng
│   │   │               │   ├── CategoryController.java     # Quản lý danh mục
│   │   │               │   ├── ProductController.java      # Quản lý sản phẩm & biến thể
│   │   │               │   ├── DiscountController.java     # Mã giảm giá
│   │   │               │   ├── ProductRatingController.java# Đánh giá sản phẩm
│   │   │               │   ├── CartController.java         # Giỏ hàng
│   │   │               │   ├── OrderController.java        # Đơn hàng
│   │   │               │   ├── OrderTrackingController.java# Theo dõi hành trình giao hàng
│   │   │               │   ├── ShippingCompanyController.java # Hãng vận chuyển
│   │   │               │   ├── WalletController.java       # Ví tiền, Nạp/Rút, Payout
│   │   │               │   ├── NotificationController.java # Thông báo
│   │   │               │   └── MediaController.java        # Upload ảnh
│   │   │               │
│   │   │               ├── service/                        # TẦNG SERVICE (Logic nghiệp vụ, trả về Entity)
│   │   │               │   ├── AuthService.java
│   │   │               │   ├── UserService.java
│   │   │               │   ├── UserAddressService.java
│   │   │               │   ├── ShopService.java
│   │   │               │   ├── CategoryService.java
│   │   │               │   ├── ProductService.java
│   │   │               │   ├── DiscountService.java
│   │   │               │   ├── ProductRatingService.java
│   │   │               │   ├── CartService.java
│   │   │               │   ├── OrderService.java
│   │   │               │   ├── OrderTrackingService.java
│   │   │               │   ├── ShippingCompanyService.java
│   │   │               │   ├── WalletService.java
│   │   │               │   ├── NotificationService.java
│   │   │               │   └── CloudinaryService.java
│   │   │               │
│   │   │               ├── repository/                     # TẦNG REPOSITORY (Thao tác CSDL)
│   │   │               │   ├── UserRepository.java
│   │   │               │   ├── UserAddressRepository.java
│   │   │               │   ├── VerificationTokenRepository.java
│   │   │               │   ├── PasswordResetTokenRepository.java
│   │   │               │   ├── ShopRepository.java
│   │   │               │   ├── CategoryRepository.java
│   │   │               │   ├── ProductRepository.java
│   │   │               │   ├── ProductVariantRepository.java
│   │   │               │   ├── DiscountRepository.java
│   │   │               │   ├── ProductRatingRepository.java
│   │   │               │   ├── CartRepository.java
│   │   │               │   ├── CartItemRepository.java
│   │   │               │   ├── OrderRepository.java
│   │   │               │   ├── OrderItemRepository.java
│   │   │               │   ├── OrderTrackingRepository.java
│   │   │               │   ├── ShippingCompanyRepository.java
│   │   │               │   ├── WalletDepositRepository.java
│   │   │               │   ├── WalletWithdrawalRepository.java
│   │   │               │   ├── PayoutRepository.java
│   │   │               │   └── NotificationRepository.java
│   │   │               │
│   │   │               ├── entity/                         # TẦNG ENTITY (Tích hợp Jackson Annotations)
│   │   │               │   ├── BaseEntity.java             # Cấu hình @JsonIgnore cho isDeleted, deletedAt
│   │   │               │   ├── User.java                   # Cấu hình @JsonProperty(WRITE_ONLY) cho password
│   │   │               │   ├── UserAddress.java
│   │   │               │   ├── VerificationToken.java
│   │   │               │   ├── PasswordResetToken.java
│   │   │               │   ├── Shop.java
│   │   │               │   ├── Category.java
│   │   │               │   ├── Product.java                # Bổ sung @JsonIgnoreProperties hoặc @JsonIgnore
│   │   │               │   ├── ProductVariant.java
│   │   │               │   ├── Discount.java
│   │   │               │   ├── ProductRating.java
│   │   │               │   ├── Cart.java
│   │   │               │   ├── CartItem.java
│   │   │               │   ├── Order.java
│   │   │               │   ├── OrderItem.java
│   │   │               │   ├── OrderTracking.java
│   │   │               │   ├── ShippingCompany.java
│   │   │               │   ├── WalletDeposit.java
│   │   │               │   ├── WalletWithdrawal.java
│   │   │               │   ├── Payout.java
│   │   │               │   └── Notification.java
│   │   │               │
│   │   │               └── dto/                            # TẦNG DTO (Chỉ chứa REQUEST)
│   │   │                   ├── auth/
│   │   │                   │   ├── RegisterRequest.java
│   │   │                   │   └── LoginRequest.java
│   │   │                   ├── user/
│   │   │                   │   ├── UserUpdateRequest.java
│   │   │                   │   └── AddressRequest.java
│   │   │                   ├── shop/
│   │   │                   │   ├── ShopCreateRequest.java
│   │   │                   │   └── ShopUpdateRequest.java
│   │   │                   ├── product/
│   │   │                   │   ├── ProductCreateRequest.java
│   │   │                   │   ├── VariantRequest.java
│   │   │                   │   ├── DiscountRequest.java
│   │   │                   │   └── RatingRequest.java
│   │   │                   ├── cart/
│   │   │                   │   ├── AddToCartRequest.java
│   │   │                   │   └── UpdateCartItemRequest.java
│   │   │                   ├── order/
│   │   │                   │   ├── CreateOrderRequest.java
│   │   │                   │   └── UpdateOrderStatusRequest.java
│   │   │                   ├── shipping/
│   │   │                   │   └── ShippingCompanyRequest.java
│   │   │                   └── wallet/
│   │   │                       ├── DepositRequest.java
│   │   │                       └── WithdrawRequest.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml             # Cấu hình DB, Cloudinary, JWT
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           └── migration/
│   │               └── V1__init_schema.sql
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── shopee/
│                   └── backend/
│                       ├── controller/
│                       │   └── AuthControllerTest.java
│                       └── service/
│                           ├── AuthServiceTest.java
│                           └── OrderServiceTest.java
│
├── .gitignore
├── pom.xml
└── README.md