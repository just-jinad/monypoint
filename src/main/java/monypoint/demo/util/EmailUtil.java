package monypoint.demo.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Component
public class EmailUtil {
    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String fromEmail;

    public EmailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String verificationLink = "http://localhost:6062/verify-email?email=" + toEmail + "&token=" + token;
        String htmlContent = "<p>Please verify your email by clicking <a href=\"" + verificationLink + "\">here</a>.</p>";

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("Monypoint Email Verification");
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}