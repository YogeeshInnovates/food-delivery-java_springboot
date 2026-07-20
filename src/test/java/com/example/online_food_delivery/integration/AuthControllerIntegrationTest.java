package com.example.online_food_delivery.integration;

import com.example.online_food_delivery.dto.authdto.LoginRequest;
import com.example.online_food_delivery.dto.authdto.OwnerRegisterRequest;
import com.example.online_food_delivery.dto.authdto.UserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.ResultMatcher;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Use a direct instance instead of autowiring to avoid missing bean
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void test_register_user_success() throws Exception {
        UserRequest userreq = new UserRequest();

        userreq.setName("Ram");
        userreq.setEmail("example@gmail.com");
        userreq.setPassword("12345");
        userreq.setPhoneNumber("9452617129");
        userreq.setAddress("Bangalore");

        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                .content(objectMapper.writeValueAsString(userreq))).andExpect(status().isOk());


        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                .content(objectMapper.writeValueAsString(userreq))).andExpect(status().isConflict());

    }


    @Test
    void test_register_owner_success() throws Exception {
        OwnerRegisterRequest ownerReq = OwnerRegisterRequest.builder()
                .name("Satish")
                .email("example1@gmail.com")
                .password("12789")
                .phoneNumber("9454561290")
                .address("Bangalore")
                .build();

        mockMvc.perform(post("/api/auth/owner/register").contentType("application/json").content(objectMapper.writeValueAsString(ownerReq))).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/owner/register").contentType("application/json").content(objectMapper.writeValueAsString(ownerReq))).andExpect(status().isConflict());

    }

    @Test
    void test_login_success() throws Exception{
        UserRequest userreq = new UserRequest();

        userreq.setName("Ram");
        userreq.setEmail("example@gmail.com");
        userreq.setPassword("12345");
        userreq.setPhoneNumber("9452617129");
        userreq.setAddress("Bangalore");

        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                .content(objectMapper.writeValueAsString(userreq))).andExpect(status().isOk());

        LoginRequest requests =  new LoginRequest();
        requests.setEmail("example@gmail.com");
        requests.setPassword("12345");
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(objectMapper.writeValueAsString(requests))).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

    }

}