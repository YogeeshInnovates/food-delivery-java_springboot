package com.example.online_food_delivery.service;

import com.example.online_food_delivery.dto.authdto.RegistrationOtpData;
import com.example.online_food_delivery.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Profile("!test")
public class OtpVerificationService {

    private static final Logger log = LoggerFactory.getLogger(OtpVerificationService.class);
    private static final String OTP_PREFIX = "otp:";
    private static final long OTP_TTL_MINUTES = 10;
    private static final long OTP_RETRY_COOLDOWN_SECONDS = 60;

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;

    public OtpVerificationService(RedisTemplate<String, Object> redisTemplate, EmailService emailService) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    public String generateOtp() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.valueOf(otp);
    }

    public void sendAndStoreOtp(RegistrationOtpData data) {
        String key = OTP_PREFIX + data.getEmail();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            RegistrationOtpData existing = (RegistrationOtpData) redisTemplate.opsForValue().get(key);
            if (existing != null && existing.getSentAt() != null) {
                long elapsed = System.currentTimeMillis() - existing.getSentAt();
                if (elapsed < OTP_RETRY_COOLDOWN_SECONDS * 1000) {
                    long wait = OTP_RETRY_COOLDOWN_SECONDS - (elapsed / 1000);
                    throw new BadRequestException("A verification code was already sent. Please wait " + wait + " seconds before requesting a new one.");
                }
            }
        }

        String otp = generateOtp();
        data.setOtp(otp);
        data.setSentAt(System.currentTimeMillis());

        try {
            redisTemplate.opsForValue().set(key, data, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to store OTP in Redis for {}: {}", data.getEmail(), e.getMessage());
            throw new BadRequestException("Service temporarily unavailable. Please try again.");
        }

        try {
            emailService.sendOtpEmail(data.getEmail(), otp, data.getName());
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", data.getEmail(), e.getMessage());
            redisTemplate.delete(key);
            throw new BadRequestException("Failed to send verification email. Please try again.");
        }
    }

    public RegistrationOtpData verifyOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        RegistrationOtpData data = (RegistrationOtpData) redisTemplate.opsForValue().get(key);

        if (data == null) {
            throw new BadRequestException("No verification code found. Please register again.");
        }

        if (!data.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid verification code.");
        }

        redisTemplate.delete(key);
        return data;
    }
}
