package com.example.online_food_delivery.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String mailUsername;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String otp, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("FoodRush Team <" + mailUsername + ">");
        message.setTo(to);
        message.setSubject("FoodRush - Email Verification");
        message.setText("Hi " + name + ",\n\n"
                + "Your email verification code is: " + otp + "\n\n"
                + "This code is valid for 10 minutes.\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Thank you,\nFoodRush Team");
        mailSender.send(message);
    }

    public void sendWelcomeEmail(String to, String name) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom("FoodRush Team <" + mailUsername + ">");
            helper.setTo(to);
            helper.setSubject("Welcome to FoodRush! \uD83C\uDF54\uD83D\uDE80");
            helper.setText(buildWelcomeHtml(name), true);
            mailSender.send(mime);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    private String buildWelcomeHtml(String name) {
        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;\">"
                + "<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 20px; border-radius: 10px; box-shadow: 0 4px 8px rgba(0,0,0,0.1);\">"
                + "<h2 style=\"color: #ff5722; text-align: center;\">Welcome to FoodRush! \uD83C\uDF54</h2>"
                + "<p style=\"font-size: 16px; color: #333333;\">Hi " + name + ",</p>"
                + "<p style=\"font-size: 16px; color: #333333;\">Thank you for signing up! We're thrilled to have you on board.</p>"
                + "<p style=\"font-size: 16px; color: #333333;\">Get ready to explore the best restaurants and enjoy lightning-fast food delivery right to your doorstep.</p>"
                + "<div style=\"text-align: center; margin: 30px 0;\">"
                + "<a href=\"" + frontendUrl + "\" style=\"background-color: #ff5722; color: #ffffff; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-size: 18px; font-weight: bold;\">Order Now</a>"
                + "</div>"
                + "<p style=\"font-size: 14px; color: #777777; text-align: center;\">If you have any questions, feel free to reply to this email.</p>"
                + "<p style=\"font-size: 14px; color: #777777; text-align: center;\">Happy Eating,<br>The FoodRush Team</p>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
}
