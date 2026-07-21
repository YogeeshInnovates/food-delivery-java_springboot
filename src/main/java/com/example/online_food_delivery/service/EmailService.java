package com.example.online_food_delivery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class EmailService {

    private final EmailSender emailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendOtpEmail(String to, String otp, String name) {
        emailSender.sendOtp(to, otp, name);
    }

    public void sendWelcomeEmail(String to, String name) {
        emailSender.sendWelcomeHtml(to, name, buildWelcomeHtml(name));
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
