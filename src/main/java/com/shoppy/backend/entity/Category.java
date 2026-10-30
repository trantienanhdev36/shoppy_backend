package com.shoppy.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "categories")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Slug thân thiện với URL, ví dụ: "dien-tu", "thoi-trang"
     */
    @Column(nullable = false, unique = true)
    private String slug;

    /**
     * URL ảnh đại diện cho danh mục (lưu trên Cloudinary)
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Danh mục cha (hỗ trợ cây phân cấp không giới hạn cấp)
     * null = danh mục gốc (root)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    @Builder.Default
    private String status = "ACTIVE";
}
