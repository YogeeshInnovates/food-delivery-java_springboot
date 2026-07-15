package com.example.online_food_delivery.integration;

import com.example.online_food_delivery.dto.authdto.LoginRequest;
import com.example.online_food_delivery.dto.authdto.LoginResponse;
import com.example.online_food_delivery.dto.authdto.OwnerRegisterRequest;
import com.example.online_food_delivery.dto.authdto.UserRequest;
import com.example.online_food_delivery.model.Role;
import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Create an admin user directly in the database if it doesn't exist
        String adminEmail = "admin@test.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .name("Admin User")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("adminpass"))
                    .role(Role.ADMIN)
                    .phoneNumber("0000000000")
                    .address("Admin Tower")
                    .isActive(true)
                    .build();
            userRepository.save(admin);
        }

        // Login to get token
        LoginRequest login = new LoginRequest();
        login.setEmail(adminEmail);
        login.setPassword("adminpass");
        MvcResult loginRes = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse resp = json.readValue(loginRes.getResponse().getContentAsString(), LoginResponse.class);
        adminToken = resp.getToken();
    }

    private Long createCustomerUser() throws Exception {
        UserRequest reg = new UserRequest();
        reg.setName("Customer");
        reg.setEmail("customer_admin_test@test.com");
        reg.setPassword("pass123");
        reg.setPhoneNumber("1234567890");
        reg.setAddress("Customer House");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(reg)))
                .andExpect(status().isOk());

        return userRepository.findByEmail("customer_admin_test@test.com").get().getId();
    }

    private Long createOwnerAndRestaurant() throws Exception {
        OwnerRegisterRequest reg = OwnerRegisterRequest.builder()
                .name("Owner")
                .email("owner_admin_test@test.com")
                .password("pass123")
                .phoneNumber("0987654321")
                .address("Owner House")
                .build();

        mvc.perform(post("/api/auth/owner/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest();
        login.setEmail("owner_admin_test@test.com");
        login.setPassword("pass123");
        MvcResult loginRes = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        String ownerToken = json.readValue(loginRes.getResponse().getContentAsString(), LoginResponse.class).getToken();

        String payload = "{\"name\":\"Admin Test Restaurant\",\"city\":\"Bangalore\",\"cuisineType\":\"Indian\",\"address\":\"Test Address\"}";
        MvcResult res = mvc.perform(post("/api/owner/restaurants")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).path("id").asLong();
    }

    @Test
    void test_listAllUsers() throws Exception {
        createCustomerUser();
        mvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void test_toggleUserBlock() throws Exception {
        Long userId = createCustomerUser();
        mvc.perform(patch("/api/admin/users/" + userId + "/block")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void test_viewAllRestaurants() throws Exception {
        createOwnerAndRestaurant();
        mvc.perform(get("/api/admin/restaurants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void test_approveRestaurant() throws Exception {
        Long restaurantId = createOwnerAndRestaurant();
        mvc.perform(patch("/api/admin/restaurants/" + restaurantId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void test_viewAllOrders() throws Exception {
        mvc.perform(get("/api/admin/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void test_getDashboardStats() throws Exception {
        mvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").exists());
    }
}
