package com.shoppy.backend.controller;

import com.shoppy.backend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestUploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload-cloudinary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> testUploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File không được để trống!");
        }

        // Upload vào thư mục 'shoppy_test' trên Cloudinary
        String imageUrl = cloudinaryService.uploadImage(file, "shoppy_test");

        return ResponseEntity.ok(Map.of(
                "message", "Upload ảnh thành công!",
                "imageUrl", imageUrl
        ));
    }
}