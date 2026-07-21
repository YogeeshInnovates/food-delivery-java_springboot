package com.example.online_food_delivery.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${spring.mail.username}") String mailUsername) {
        this.mailSender = mailSender;
        this.fromAddress = "FoodRush Team <" + mailUsername + ">";
    }

    @Override
    public void sendOtp(String to, String otp, String name) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("FoodRush - Email Verification");
        msg.setText("Hi " + name + ",\n\n"
                + "Your email verification code is: " + otp + "\n\n"
                + "This code is valid for 10 minutes.\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Thank you,\nFoodRush Team");
        mailSender.send(msg);
    }

    @Override
    public void sendWelcomeHtml(String to, String name, String html) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Welcome to FoodRush! \uD83C\uDF54\uD83D\uDE80");
            helper.setText(html, true);
            mailSender.send(mime);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }
}
