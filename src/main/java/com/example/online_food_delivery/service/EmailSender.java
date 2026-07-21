package com.example.online_food_delivery.service;

public interface EmailSender {
    void sendOtp(String to, String otp, String name);
    void sendWelcomeHtml(String to, String name, String html);
}
