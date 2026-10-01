package com.shoppy.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
public class ShoppyApplication {

    public static void main(String[] args) {
        // Ép JVM dùng đúng ID múi giờ IANA chuẩn cho Việt Nam
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        SpringApplication.run(ShoppyApplication.class, args);
    }
}