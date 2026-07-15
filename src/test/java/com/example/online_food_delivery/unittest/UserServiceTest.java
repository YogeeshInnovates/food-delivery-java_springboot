package com.example.online_food_delivery.unittest;

import com.example.online_food_delivery.config.security.JwtUtil;
import com.example.online_food_delivery.dto.authdto.LoginRequest;
import com.example.online_food_delivery.dto.authdto.LoginResponse;
import com.example.online_food_delivery.dto.authdto.OwnerRegisterRequest;
import com.example.online_food_delivery.dto.authdto.UserRequest;
import com.example.online_food_delivery.dto.authdto.UserResponse;
import com.example.online_food_delivery.exception.DublicateResourceFoundException;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.model.Role;
import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.UserRepository;
import com.example.online_food_delivery.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userrepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userservice;

    private UserRequest userreq;

    @BeforeEach
    void setup() {
        userreq = new UserRequest();
        userreq.setName("Ram");
        userreq.setEmail("example@gmail.com");
        userreq.setPassword("12345");
        userreq.setPhoneNumber("9452617129");
        userreq.setAddress("Bangalore");
    }

    @Test
    public void test_create_user_success() {
        when(userrepo.existsByEmail("example@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("12345")).thenReturn("encodedPassword123");
        User savedUser = User.builder()
                .name("Ram")
                .email("example@gmail.com")
                .password("encodedPassword123")
                .role(Role.CUSTOMER)
                .phoneNumber("9452617129")
                .address("Bangalore")
                .build();
        when(userrepo.save(any())).thenReturn(savedUser);
        UserResponse response = userservice.create_user(userreq);
        assertEquals("Ram", response.getName());
        verify(userrepo).save(any());
    }

    @Test
    public void test_create_user_email_exists() {
        when(userrepo.existsByEmail("example@gmail.com")).thenReturn(true);
        assertThrows(DublicateResourceFoundException.class, () -> userservice.create_user(userreq));
        verify(userrepo, never()).save(any());
    }

    @Test
    public void test_create_owner_success() {
        OwnerRegisterRequest ownerReq = OwnerRegisterRequest.builder()
                .name("Ram")
                .email("example@gmail.com")
                .password("12345")
                .phoneNumber("9452617129")
                .address("Bangalore")
                .build();
        when(userrepo.existsByEmail("example@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("12345")).thenReturn("encodedPassword123");
        User savedUser = User.builder()
                .name("Ram")
                .email("example@gmail.com")
                .password("encodedPassword123")
                .role(Role.OWNER)
                .phoneNumber("9452617129")
                .address("Bangalore")
                .build();
        when(userrepo.save(any())).thenReturn(savedUser);
        UserResponse res = userservice.create_owner(ownerReq);
        assertEquals("Ram", res.getName());
    }

    @Test
    public void test_login_success() {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("example@gmail.com");
        loginReq.setPassword("12345");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(org.springframework.security.core.Authentication.class));
        User user = User.builder().email("example@gmail.com").build();
        when(userrepo.findByEmail("example@gmail.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");
        LoginResponse resp = userservice.login(loginReq);
        assertEquals("jwt-token", resp.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    public void test_login_user_not_found() {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("missing@example.com");
        loginReq.setPassword("pwd");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(org.springframework.security.core.Authentication.class));
        when(userrepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userservice.login(loginReq));
    }

    @Test
    public void test_getAllUsers_pagination() {
        User user1 = User.builder().name("Alice").build();
        User user2 = User.builder().name("Bob").build();
        Pageable pageable = PageRequest.of(0, 2);
        Page<User> userPage = new PageImpl<>(Arrays.asList(user1, user2), pageable, 2);
        when(userrepo.findAll(pageable)).thenReturn(userPage);
        Page<UserResponse> result = userservice.getAllUsers(pageable);
        assertEquals(2, result.getTotalElements());
        assertEquals("Alice", result.getContent().get(0).getName());
        assertEquals("Bob", result.getContent().get(1).getName());
    }

    @Test
    public void test_toggleUserStatus() {
        User user = User.builder().id(1L).isActive(true).build();
        when(userrepo.findById(1L)).thenReturn(Optional.of(user));
        userservice.toggleUserStatus(1L);
        assertFalse(user.getIsActive());
        verify(userrepo).save(user);
    }
}
