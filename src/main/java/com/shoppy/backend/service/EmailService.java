package com.shoppy.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@shoppy.com}")
    private String fromEmail;

    //  Bổ sung fallback :http://localhost:8080
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    @Async
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        try {
            //  Nối baseUrl động từ .env / Render với Route của Controller
            String activationLink = baseUrl + "/auth/verify?token=" + token;

            log.info("LINK XÁC THỰC HOÀN CHỈNH: {}", activationLink);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "Shoppy Team");
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản Shoppy");

            String htmlContent = "<h3>Xin chào " + fullName + ",</h3>"
                    + "<p>Vui lòng nhấp vào đường link bên dưới để kích hoạt tài khoản:</p>"
                    + "<p><a href=\"" + activationLink + "\" data-brevo-click-tracking=\"false\">KÍCH HOẠT TÀI KHOẢN NGAY</a></p>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Lỗi gửi email: {}", e.getMessage());
        }
    }
}