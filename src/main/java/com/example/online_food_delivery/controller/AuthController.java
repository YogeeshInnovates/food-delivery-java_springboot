package com.example.online_food_delivery.controller;

import com.example.online_food_delivery.dto.authdto.UserRequest;
import com.example.online_food_delivery.dto.authdto.UserResponse;
import com.example.online_food_delivery.dto.authdto.OwnerRegisterRequest;
import com.example.online_food_delivery.dto.authdto.LoginRequest;
import com.example.online_food_delivery.dto.authdto.LoginResponse;
import com.example.online_food_delivery.dto.authdto.VerifyOtpRequest;
import com.example.online_food_delivery.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Value("${app.otp.enabled:true}")
    private boolean otpEnabled;

    public AuthController(UserService userservice){
        this.userService = userservice;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRequest userrequest){
        if (otpEnabled) {
            userService.sendOtpForCustomer(userrequest);
            return ResponseEntity.ok(Map.of("message", "Verification code sent to your email", "email", userrequest.getEmail()));
        }
        UserResponse res = userService.create_user(userrequest);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/owner/register")
    public ResponseEntity<?> registerOwner(@Valid @RequestBody OwnerRegisterRequest request){
        if (otpEnabled) {
            userService.sendOtpForOwner(request);
            return ResponseEntity.ok(Map.of("message", "Verification code sent to your email", "email", request.getEmail()));
        }
        UserResponse res = userService.create_owner(request);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/verify-otp")
    public UserResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request){
        return userService.completeRegistrationWithOtp(request.getEmail(), request.getOtp());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginreq){
        LoginResponse res = userService.login(loginreq);
        return  ResponseEntity.ok(res);
    }
}
