package com.example.online_food_delivery.service;

import com.sendgrid.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Profile("prod")
public class SendGridEmailSender implements EmailSender {

    private final SendGrid sendGrid;
    private final String fromEmail;

    public SendGridEmailSender(@Value("${SENDGRID_API_KEY}") String apiKey,
                               @Value("${MAIL_USERNAME}") String mailUsername) {
        this.sendGrid = new SendGrid(apiKey);
        this.fromEmail = mailUsername;
    }

    @Override
    public void sendOtp(String to, String otp, String name) {
        String subject = "FoodRush - Email Verification";
        String plainText = "Hi " + name + ",\n\n"
                + "Your email verification code is: " + otp + "\n\n"
                + "This code is valid for 10 minutes.\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Thank you,\nFoodRush Team";
        send(to, subject, plainText, null);
    }

    @Override
    public void sendWelcomeHtml(String to, String name, String html) {
        send(to, "Welcome to FoodRush! \uD83C\uDF54\uD83D\uDE80", null, html);
    }

    private void send(String to, String subject, String plainText, String htmlContent) {
        String body = "{" +
                "\"personalizations\":[{\"to\":[{\"email\":\"" + to + "\"}]}]," +
                "\"from\":{\"email\":\"" + fromEmail + "\",\"name\":\"FoodRush Team\"}," +
                "\"subject\":\"" + escapeJson(subject) + "\"," +
                "\"content\":[" +
                (plainText != null ? "{\"type\":\"text/plain\",\"value\":\"" + escapeJson(plainText) + "\"}" : "") +
                (plainText != null && htmlContent != null ? "," : "") +
                (htmlContent != null ? "{\"type\":\"text/html\",\"value\":\"" + escapeJson(htmlContent) + "\"}" : "") +
                "]" +
                "}";

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(body);

        try {
            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 400) {
                throw new RuntimeException("SendGrid error " + response.getStatusCode()
                        + ": " + response.getBody());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to send email via SendGrid", e);
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
