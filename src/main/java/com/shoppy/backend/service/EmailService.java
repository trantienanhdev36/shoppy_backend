package com.shoppy.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async // Gửi mail bất đồng bộ để tránh làm chậm Response đăng ký
    public void sendVerificationEmail(String toEmail, String fullName, String activationLink) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản Shoppy của bạn");

            String htmlContent = "<h3>Xin chào " + fullName + ",</h3>"
                    + "<p>Cảm ơn bạn đã đăng ký tài khoản tại Shoppy.</p>"
                    + "<p>Vui lòng nhấp vào đường link bên dưới để kích hoạt tài khoản của bạn (đường link có hiệu lực trong 24h):</p>"
                    + "<p><a href=\"" + activationLink + "\">KÍCH HOẠT TÀI KHOẢN NGAY</a></p>"
                    + "<br><p>Trân trọng,<br>Shoppy Team</p>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email xác thực: " + e.getMessage());
        }
    }
}