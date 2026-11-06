-- =============================================================================
-- Migration: V20261106__rename_shop_google_map_url_to_address.sql
-- Mô tả : Đổi tên cột google_map_url thành address và giữ nguyên dữ liệu hiện có.
-- Ngày  : 2026-11-06
-- =============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'shops'
          AND table_schema = current_schema()
          AND column_name = 'google_map_url'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'shops'
          AND table_schema = current_schema()
          AND column_name = 'address'
    ) THEN
        ALTER TABLE shops RENAME COLUMN google_map_url TO address;
    END IF;
END $$;
