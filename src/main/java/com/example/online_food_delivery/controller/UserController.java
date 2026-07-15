package com.example.online_food_delivery.controller;

import com.example.online_food_delivery.dto.authdto.UserResponse;
import com.example.online_food_delivery.service.CloudinaryService;
import com.example.online_food_delivery.service.UserService;
import com.example.online_food_delivery.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;
    private final AuthUtil authUtil;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(userService.updateCurrentUser(updates));
    }

    @PostMapping("/me/image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        String folder = "profiles/" + authUtil.currentUser().getId();
        String imageUrl = cloudinaryService.uploadImage(file, folder);
        userService.updateProfileImage(imageUrl);
        return ResponseEntity.ok(Map.of("profileImageUrl", imageUrl));
    }
}
