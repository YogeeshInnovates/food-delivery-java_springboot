package com.example.online_food_delivery.service;

import com.example.online_food_delivery.config.security.JwtUtil;
import com.example.online_food_delivery.dto.authdto.*;
import com.example.online_food_delivery.exception.BadRequestException;
import com.example.online_food_delivery.exception.DublicateResourceFoundException;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.exception.UnauthorizedException;
import com.example.online_food_delivery.model.Restaurant;
import com.example.online_food_delivery.model.RestaurantStatus;
import com.example.online_food_delivery.model.Role;
import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.RestaurantRepository;
import com.example.online_food_delivery.repository.UserRepository;
import com.example.online_food_delivery.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userrepo;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuthUtil authUtil;
    private final OtpVerificationService otpVerificationService;

    public void sendOtpForCustomer(UserRequest user) {
        if (userrepo.existsByEmail(user.getEmail())) {
            throw new DublicateResourceFoundException("Email already registered with this email");
        }
        RegistrationOtpData data = RegistrationOtpData.builder()
                .role("CUSTOMER")
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .build();
        otpVerificationService.sendAndStoreOtp(data);
    }

    public void sendOtpForOwner(OwnerRegisterRequest req) {
        if (userrepo.existsByEmail(req.getEmail())) {
            throw new DublicateResourceFoundException("Email already registered with this email");
        }
        RegistrationOtpData data = RegistrationOtpData.builder()
                .role("OWNER")
                .name(req.getName())
                .email(req.getEmail())
                .password(req.getPassword())
                .phoneNumber(req.getPhoneNumber())
                .address(req.getAddress())
                .restaurantName(req.getRestaurantName())
                .licenseNumber(req.getLicenseNumber())
                .restaurantAddress(req.getRestaurantAddress())
                .build();
        otpVerificationService.sendAndStoreOtp(data);
    }

    @Transactional
    public UserResponse completeRegistrationWithOtp(String email, String otp) {
        RegistrationOtpData data = otpVerificationService.verifyOtp(email, otp);

        if ("CUSTOMER".equals(data.getRole())) {
            User user = User.builder()
                    .name(data.getName())
                    .email(data.getEmail())
                    .password(passwordEncoder.encode(data.getPassword()))
                    .role(Role.CUSTOMER)
                    .phoneNumber(data.getPhoneNumber())
                    .address(data.getAddress())
                    .build();
            User saved = userrepo.save(user);
            return mapToUserResponse(saved);
        } else {
            User user = User.builder()
                    .name(data.getName())
                    .email(data.getEmail())
                    .password(passwordEncoder.encode(data.getPassword()))
                    .role(Role.OWNER)
                    .phoneNumber(data.getPhoneNumber())
                    .address(data.getAddress())
                    .build();
            User savedUser = userrepo.save(user);

            if (data.getRestaurantName() != null && !data.getRestaurantName().isBlank()) {
                Restaurant restaurant = Restaurant.builder()
                        .name(data.getRestaurantName())
                        .address(data.getRestaurantAddress() != null ? data.getRestaurantAddress() : data.getAddress())
                        .owner(savedUser)
                        .build();
                restaurantRepository.save(restaurant);
            }
            return mapToUserResponse(savedUser);
        }
    }

    public UserResponse create_user(UserRequest user) {

        if (userrepo.existsByEmail(user.getEmail())) {
            throw new DublicateResourceFoundException("Email already registered with this email");
        }
        User users = User.builder().name(user.getName()).email(user.getEmail())
                .password(passwordEncoder.encode(user.getPassword())).role(Role.CUSTOMER)
                .phoneNumber(user.getPhoneNumber()).address(user.getAddress()).build();
        User savedrepo = userrepo.save(users);
        return mapToUserResponse(savedrepo);
    }

    @Transactional
    public UserResponse create_owner(OwnerRegisterRequest req) {
        if (userrepo.existsByEmail(req.getEmail())) {
            throw new DublicateResourceFoundException("Email already registered with this email");
        }
        // Role is hardcoded to OWNER — never accepted from request body
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.OWNER)
                .phoneNumber(req.getPhoneNumber())
                .address(req.getAddress())
                .build();
        User savedUser = userrepo.save(user);

        // If optional restaurant fields are provided, auto-create a restaurant
        if (req.getRestaurantName() != null && !req.getRestaurantName().isBlank()) {
            Restaurant restaurant = Restaurant.builder()
                    .name(req.getRestaurantName())
                    .address(req.getRestaurantAddress() != null ? req.getRestaurantAddress() : req.getAddress())
                    .owner(savedUser)
                    .build();
            restaurantRepository.save(restaurant);
        }

        return mapToUserResponse(savedUser);
    }

    @Transactional
    public UserResponse createAdmin(CreateAdminRequest req) {
        if (userrepo.existsByEmail(req.getEmail())) {
            throw new DublicateResourceFoundException("Email already registered");
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.ADMIN)
                .phoneNumber(req.getPhoneNumber())
                .address("")
                .build();
        User savedUser = userrepo.save(user);
        return mapToUserResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            System.out.println("Authentication failed: " + e.getMessage());
            throw new UnauthorizedException("Invalid email or password");
        }
        User user_repo = userrepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));
        String token = jwtUtil.generateToken(user_repo);
        return new LoginResponse(token);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userrepo.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    public void toggleUserStatus(Long userId) {
        User user = userrepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(!user.getIsActive());
        userrepo.save(user);
    }

    public UserResponse getCurrentUser() {
        User user = authUtil.currentUser();
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(Map<String, String> updates) {
        User user = authUtil.currentUser();
        if (updates.containsKey("name")) {
            user.setName(updates.get("name"));
        }
        if (updates.containsKey("phoneNumber")) {
            user.setPhoneNumber(updates.get("phoneNumber"));
        }
        if (updates.containsKey("address")) {
            user.setAddress(updates.get("address"));
        }
        if (updates.containsKey("profileImageUrl")) {
            user.setProfileImageUrl(updates.get("profileImageUrl"));
        }
        userrepo.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public void updateProfileImage(String imageUrl) {
        User user = authUtil.currentUser();
        user.setProfileImageUrl(imageUrl);
        userrepo.save(user);
    }

    private UserResponse mapToUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .role(u.getRole())
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .address(u.getAddress())
                .profileImageUrl(u.getProfileImageUrl())
                .build();
    }
}
