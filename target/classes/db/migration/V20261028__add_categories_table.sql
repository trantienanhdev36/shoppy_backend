-- =============================================================================
-- Migration: V20261028__add_categories_table.sql
-- Mô tả : Tạo bảng categories và liên kết với bảng products.
--          Cũng INSERT một số danh mục mặc định và UPDATE sản phẩm cũ
--          (chưa có category) vào danh mục "Khác" để tránh giá trị NULL.
-- Ngày  : 2026-10-28
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Tạo bảng categories
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories
(
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL UNIQUE,
    description   TEXT,
    slug          VARCHAR(255) NOT NULL UNIQUE,
    image_url     VARCHAR(1000),
    parent_id     BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    display_order INTEGER      NOT NULL DEFAULT 0,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE categories IS 'Danh mục sản phẩm, hỗ trợ cây phân cấp không giới hạn cấp';

-- -----------------------------------------------------------------------------
-- 2. Thêm cột category_id vào bảng products (nullable — không phá vỡ dữ liệu cũ)
-- -----------------------------------------------------------------------------
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES categories (id) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- 3. INSERT danh mục gốc mặc định (root categories)
-- -----------------------------------------------------------------------------
INSERT INTO categories (name, slug, description, display_order, is_deleted, created_at, updated_at)
VALUES ('Điện tử & Công nghệ', 'dien-tu-cong-nghe',
        'Điện thoại, máy tính, phụ kiện, thiết bị điện tử', 1, FALSE, NOW(), NOW()),
       ('Thời trang', 'thoi-trang',
        'Quần áo, giày dép, túi xách, phụ kiện thời trang', 2, FALSE, NOW(), NOW()),
       ('Nhà cửa & Đời sống', 'nha-cua-doi-song',
        'Đồ nội thất, trang trí nhà cửa, đồ gia dụng', 3, FALSE, NOW(), NOW()),
       ('Sức khỏe & Làm đẹp', 'suc-khoe-lam-dep',
        'Mỹ phẩm, chăm sóc sức khỏe, thực phẩm chức năng', 4, FALSE, NOW(), NOW()),
       ('Thể thao & Du lịch', 'the-thao-du-lich',
        'Dụng cụ thể thao, đồ dã ngoại, thiết bị du lịch', 5, FALSE, NOW(), NOW()),
       ('Thực phẩm & Đồ uống', 'thuc-pham-do-uong',
        'Thực phẩm tươi sống, đồ ăn vặt, đồ uống', 6, FALSE, NOW(), NOW()),
       ('Sách & Văn phòng phẩm', 'sach-van-phong-pham',
        'Sách, dụng cụ học tập, văn phòng phẩm', 7, FALSE, NOW(), NOW()),
       ('Mẹ & Bé', 'me-va-be',
        'Đồ chơi, quần áo trẻ em, sản phẩm cho mẹ và bé', 8, FALSE, NOW(), NOW()),
       ('Ô tô & Xe máy', 'o-to-xe-may',
        'Phụ kiện ô tô, xe máy, xe đạp', 9, FALSE, NOW(), NOW()),
       ('Khác', 'khac',
        'Các sản phẩm không thuộc danh mục trên', 99, FALSE, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 4. INSERT danh mục con (sub-categories) cho một số danh mục chính
-- -----------------------------------------------------------------------------
INSERT INTO categories (name, slug, description, parent_id, display_order, is_deleted, created_at, updated_at)
VALUES
    -- Con của "Điện tử & Công nghệ"
    ('Điện thoại & Máy tính bảng', 'dien-thoai-may-tinh-bang', NULL,
     (SELECT id FROM categories WHERE slug = 'dien-tu-cong-nghe'), 1, FALSE, NOW(), NOW()),
    ('Laptop & Máy tính', 'laptop-may-tinh', NULL,
     (SELECT id FROM categories WHERE slug = 'dien-tu-cong-nghe'), 2, FALSE, NOW(), NOW()),
    ('Phụ kiện điện tử', 'phu-kien-dien-tu', NULL,
     (SELECT id FROM categories WHERE slug = 'dien-tu-cong-nghe'), 3, FALSE, NOW(), NOW()),
    ('Âm thanh & Hình ảnh', 'am-thanh-hinh-anh', NULL,
     (SELECT id FROM categories WHERE slug = 'dien-tu-cong-nghe'), 4, FALSE, NOW(), NOW()),

    -- Con của "Thời trang"
    ('Quần áo nam', 'quan-ao-nam', NULL,
     (SELECT id FROM categories WHERE slug = 'thoi-trang'), 1, FALSE, NOW(), NOW()),
    ('Quần áo nữ', 'quan-ao-nu', NULL,
     (SELECT id FROM categories WHERE slug = 'thoi-trang'), 2, FALSE, NOW(), NOW()),
    ('Giày dép', 'giay-dep', NULL,
     (SELECT id FROM categories WHERE slug = 'thoi-trang'), 3, FALSE, NOW(), NOW()),
    ('Túi xách & Ví', 'tui-xach-vi', NULL,
     (SELECT id FROM categories WHERE slug = 'thoi-trang'), 4, FALSE, NOW(), NOW()),

    -- Con của "Nhà cửa & Đời sống"
    ('Đồ gia dụng', 'do-gia-dung', NULL,
     (SELECT id FROM categories WHERE slug = 'nha-cua-doi-song'), 1, FALSE, NOW(), NOW()),
    ('Nội thất', 'noi-that', NULL,
     (SELECT id FROM categories WHERE slug = 'nha-cua-doi-song'), 2, FALSE, NOW(), NOW()),
    ('Dụng cụ nhà bếp', 'dung-cu-nha-bep', NULL,
     (SELECT id FROM categories WHERE slug = 'nha-cua-doi-song'), 3, FALSE, NOW(), NOW()),

    -- Con của "Sức khỏe & Làm đẹp"
    ('Mỹ phẩm', 'my-pham', NULL,
     (SELECT id FROM categories WHERE slug = 'suc-khoe-lam-dep'), 1, FALSE, NOW(), NOW()),
    ('Chăm sóc cá nhân', 'cham-soc-ca-nhan', NULL,
     (SELECT id FROM categories WHERE slug = 'suc-khoe-lam-dep'), 2, FALSE, NOW(), NOW()),
    ('Thực phẩm chức năng', 'thuc-pham-chuc-nang', NULL,
     (SELECT id FROM categories WHERE slug = 'suc-khoe-lam-dep'), 3, FALSE, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 5. UPDATE sản phẩm cũ (category_id IS NULL) → gán vào danh mục "Khác"
--    Đây là câu lệnh UPDATE chính để xử lý dữ liệu lịch sử.
-- -----------------------------------------------------------------------------
UPDATE products
SET category_id = (SELECT id FROM categories WHERE slug = 'khac' LIMIT 1),
    updated_at  = NOW()
WHERE category_id IS NULL
  AND is_deleted = FALSE;

-- -----------------------------------------------------------------------------
-- 6. Tạo index để tối ưu truy vấn
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products (category_id);
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories (slug);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories (parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_display_order ON categories (display_order);

COMMIT;
