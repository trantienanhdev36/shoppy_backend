-- =============================================================================
-- Migration: V20261029__add_status_to_categories.sql
-- Mô tả : Bổ sung cột status vào bảng categories với giá trị mặc định là 'ACTIVE'
-- Ngày  : 2026-10-29
-- =============================================================================

ALTER TABLE categories 
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
