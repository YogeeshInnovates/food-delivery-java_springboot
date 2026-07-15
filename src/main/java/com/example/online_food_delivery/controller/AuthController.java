package com.example.online_food_delivery.controller;

import com.example.online_food_delivery.dto.authdto.UserRequest;
import com.example.online_food_delivery.dto.authdto.UserResponse;
import com.example.online_food_delivery.dto.authdto.OwnerRegisterRequest;
import com.example.online_food_delivery.dto.authdto.LoginRequest;
import com.example.online_food_delivery.dto.authdto.LoginResponse;
import com.example.online_food_delivery.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userservice){
        this.userService = userservice;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody UserRequest userrequest){
            return userService.create_user(userrequest);
    }

    @PostMapping("/owner/register")
    public UserResponse registerOwner(@Valid @RequestBody OwnerRegisterRequest request){
        return userService.create_owner(request);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginreq){
        LoginResponse res = userService.login(loginreq);
        return  ResponseEntity.ok(res);
    }
}
