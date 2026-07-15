package com.example.online_food_delivery.integration;

import com.example.online_food_delivery.dto.authdto.LoginRequest;
import com.example.online_food_delivery.dto.authdto.LoginResponse;
import com.example.online_food_delivery.dto.authdto.UserRequest;
import com.example.online_food_delivery.dto.restaurantDto.RestaurantRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import com.example.online_food_delivery.repository.RestaurantRepository;
import com.example.online_food_delivery.model.Restaurant;
import com.example.online_food_delivery.model.RestaurantStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public class RestaurantControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private RestaurantRepository restaurantRepository;

        private final ObjectMapper json = new ObjectMapper();

        /* ------------------------------- helpers ------------------------------- */
        private String registerAndLogin(String name, String email, String password, String role) throws Exception {
                UserRequest reg = new UserRequest();
                reg.setName(name);
                reg.setEmail(email);
                reg.setPassword(password);
                reg.setPhoneNumber("9876543210");
                reg.setAddress("Bangalore");

                String registerUrl = role.equals("OWNER") ? "/api/auth/owner/register" : "/api/auth/register";
                mockMvc.perform(post(registerUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(reg)))
                                .andExpect(status().isOk());

                LoginRequest login = new LoginRequest();
                login.setEmail(email);
                login.setPassword(password);
                MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andReturn();
                LoginResponse resp = json.readValue(loginRes.getResponse().getContentAsString(), LoginResponse.class);
                return resp.getToken();
        }

        private Long createRestaurant(String ownerToken, String name, String city, String cuisine) throws Exception {
                String payload = String.format(
                                "{\"name\":\"%s\",\"city\":\"%s\",\"cuisineType\":\"%s\",\"address\":\"Test Address\"}",
                                name, city, cuisine);
                MvcResult res = mockMvc.perform(post("/api/owner/restaurants")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andReturn();
                return json.readTree(res.getResponse().getContentAsString()).path("id").asLong();
        }

        /* ------------------------------- tests -------------------------------- */

        @Test
        void test_addRestaurant_success() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_rest1@test.com", "ownerpass", "OWNER");

                String payload = "{\"name\":\"Spice Garden\",\"city\":\"Bangalore\",\"cuisineType\":\"Indian\",\"address\":\"Test Address\"}";

                mockMvc.perform(post("/api/owner/restaurants")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("Spice Garden"))
                                .andExpect(jsonPath("$.city").value("Bangalore"))
                                .andExpect(jsonPath("$.cuisineType").value("Indian"));
        }

        @Test
        void test_listRestaurants() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_rest2@test.com", "ownerpass", "OWNER");
                Long r1 = createRestaurant(ownerToken, "Rest 1", "City A", "Cuisine A");
                Long r2 = createRestaurant(ownerToken, "Rest 2", "City B", "Cuisine B");
                restaurantRepository.findById(r1).ifPresent(r -> { r.setStatus(RestaurantStatus.OPEN); restaurantRepository.save(r); });
                restaurantRepository.findById(r2).ifPresent(r -> { r.setStatus(RestaurantStatus.OPEN); restaurantRepository.save(r); });

                mockMvc.perform(get("/api/restaurants")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        void test_filterRestaurants() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_rest3@test.com", "ownerpass", "OWNER");
                Long restId = createRestaurant(ownerToken, "Filter Rest", "Mumbai", "Maharashtrian");
                restaurantRepository.findById(restId).ifPresent(r -> { r.setStatus(RestaurantStatus.OPEN); restaurantRepository.save(r); });

                mockMvc.perform(get("/api/restaurants/filter")
                                .param("city", "Mumbai")
                                .param("cuisine", "Maharashtrian")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                                .andExpect(jsonPath("$[0].city").value("Mumbai"));
        }

        @Test
        void test_getRestaurant() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_rest4@test.com", "ownerpass", "OWNER");
                Long restId = createRestaurant(ownerToken, "Get Rest", "Delhi", "North Indian");
                restaurantRepository.findById(restId).ifPresent(r -> { r.setStatus(RestaurantStatus.OPEN); restaurantRepository.save(r); });

                mockMvc.perform(get("/api/restaurants/" + restId)
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(restId))
                                .andExpect(jsonPath("$.name").value("Get Rest"));
        }

        @Test
        void test_updateRestaurant() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_rest5@test.com", "ownerpass", "OWNER");
                Long restId = createRestaurant(ownerToken, "Old Rest", "Old City", "Old Cuisine");

                String updatePayload = "{\"name\":\"New Rest\",\"city\":\"New City\",\"cuisineType\":\"New Cuisine\",\"address\":\"New Address\"}";

                mockMvc.perform(put("/api/owner/restaurants/" + restId)
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updatePayload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("New Rest"))
                                .andExpect(jsonPath("$.city").value("New City"));
        }

        @Test
        void test_toggleStatus() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_rest6@test.com", "ownerpass", "OWNER");
                Long restId = createRestaurant(ownerToken, "Status Rest", "City", "Cuisine");

                Restaurant restaurant = restaurantRepository.findById(restId).get();
                restaurant.setStatus(RestaurantStatus.OPEN);
                restaurantRepository.save(restaurant);

                mockMvc.perform(patch("/api/owner/restaurants/" + restId + "/status")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }
}
