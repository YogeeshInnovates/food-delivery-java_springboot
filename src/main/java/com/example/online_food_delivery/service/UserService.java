package com.example.online_food_delivery.service;

import com.example.online_food_delivery.config.security.JwtUtil;
import com.example.online_food_delivery.dto.authdto.*;
import com.example.online_food_delivery.model.Role;
import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userrepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public UserResponse create_user(UserRequest user){
    User users = User.builder().name(user.getName()).email(user.getEmail()).password(passwordEncoder.encode(user.getPassword())).role(Role.CUSTOMER).phoneNumber(user.getPhoneNumber()).address(user.getAddress()).build();
    User savedrepo = userrepo.save(users);
    return new UserResponse(savedrepo.getName());
    }

    public UserResponse create_owner(UserRequest user){
        User users = User.builder().name(user.getName()).email(user.getEmail()).password(passwordEncoder.encode(user.getPassword())).role(Role.OWNER).phoneNumber(user.getPhoneNumber()).address(user.getAddress()).build();
        User savedrepo = userrepo.save(users);
        return new UserResponse(savedrepo.getName());
    }

    public LoginResponse login(LoginRequest request){
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword())
        );
        User user_repo = userrepo.findByEmail(request.getEmail()).orElseThrow(()->new RuntimeException("User not found"));
        String token = jwtUtil.generateToken(user_repo);
        return new LoginResponse(token);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userrepo.findAll(pageable)
                .map(u -> new UserResponse(u.getName()));
    }

    public void toggleUserStatus(Long userId) {
        User user = userrepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(!user.getIsActive());
        userrepo.save(user);
    }
}

