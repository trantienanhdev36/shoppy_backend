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

    @Async
    public void sendOtpEmail(String toEmail, String fullName, String otp) {
        try {
            log.info("MÃ OTP ĐẶT LẠI MẬT KHẨU CHO [{}]: {}", toEmail, otp);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "Shoppy Team");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực đặt lại mật khẩu - Shoppy");

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 25px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;">
                    <h2 style="color: #ee4d2d; text-align: center; margin-top: 0; font-size: 26px;">SHOPPY</h2>
                    <p style="font-size: 15px; color: #333;">Xin chào <b>%s</b>,</p>
                    <p style="font-size: 14px; color: #555; line-height: 1.6;">Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Shoppy của mình. Dưới đây là mã xác thực OTP của bạn:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <span style="display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #ee4d2d; background-color: #fff0ed; padding: 12px 28px; border-radius: 8px; border: 1px dashed #ee4d2d;">%s</span>
                    </div>
                    <p style="color: #666; font-size: 13px; line-height: 1.5;">Mã xác thực này có hiệu lực trong vòng <b>5 phút</b>. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 25px 0;" />
                    <p style="color: #999; font-size: 12px; margin-bottom: 0;">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc liên hệ với bộ phận hỗ trợ của chúng tôi.</p>
                </div>
                """.formatted(fullName, otp);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Lỗi gửi email OTP: {}", e.getMessage());
        }
    }
}