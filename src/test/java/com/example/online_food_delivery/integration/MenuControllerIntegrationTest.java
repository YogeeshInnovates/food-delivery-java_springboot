package com.example.online_food_delivery.integration;

import com.example.online_food_delivery.dto.authdto.LoginRequest;
import com.example.online_food_delivery.dto.authdto.LoginResponse;
import com.example.online_food_delivery.dto.authdto.UserRequest;
import com.example.online_food_delivery.dto.menu_dto.Menu_Request;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class MenuControllerIntegrationTest {

        @Autowired
        private MockMvc mvc;

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
                mvc.perform(post(registerUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(reg)))
                                .andExpect(status().isOk());

                LoginRequest login = new LoginRequest();
                login.setEmail(email);
                login.setPassword(password);
                MvcResult loginRes = mvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andReturn();
                LoginResponse resp = json.readValue(loginRes.getResponse().getContentAsString(), LoginResponse.class);
                return resp.getToken();
        }

        private Long createRestaurant(String ownerToken, String name) throws Exception {
                String payload = String.format(
                                "{\"name\":\"%s\",\"city\":\"Bangalore\",\"cuisineType\":\"Indian\",\"address\":\"Test Address\"}",
                                name);
                MvcResult res = mvc.perform(post("/api/owner/restaurants")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andReturn();
                return json.readTree(res.getResponse().getContentAsString()).path("id").asLong();
        }

        private Long addMenuItem(String ownerToken, Long restaurantId, String itemName, double price, String category) throws Exception {
                Menu_Request item = new Menu_Request();
                item.setName(itemName);
                item.setPrice(price);
                item.setCategory(category);
                item.setDescription("Test item " + itemName);

                MvcResult res = mvc.perform(post("/api/owner/restaurants/" + restaurantId + "/menu-items")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(item)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andReturn();
                return json.readTree(res.getResponse().getContentAsString()).path("id").asLong();
        }

        /* ------------------------------- tests -------------------------------- */
        @Test
        void test_addMenuItem_success() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_menu_it@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "MenuPlace");

                Menu_Request item = new Menu_Request();
                item.setName("Burger");
                item.setPrice(5.50);
                item.setCategory("BURGER");
                item.setDescription("Juicy beef burger");

                mvc.perform(post("/api/owner/restaurants/" + restaurantId + "/menu-items")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(item)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.name", is("Burger")));
        }

        @Test
        void test_getMenuItems_forRestaurant() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_menu_it2@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "MenuList");

                addMenuItem(ownerToken, restaurantId, "Pizza", 9.99, "PIZZA");
                addMenuItem(ownerToken, restaurantId, "Soda", 1.99, "BEVERAGES");

                mvc.perform(get("/api/restaurants/" + restaurantId + "/menu")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        void test_updateMenuItem_success() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_menu_it3@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "MenuUpdate");
                Long itemId = addMenuItem(ownerToken, restaurantId, "Wrap", 4.99, "ROLLS");

                Menu_Request update = new Menu_Request();
                update.setName("Wrap");
                update.setPrice(5.49);
                update.setCategory("ROLLS");
                update.setDescription("Chicken wrap");

                mvc.perform(put("/api/owner/menu-items/" + itemId)
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(update)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.price", is(5.49)));
        }

        @Test
        void test_deleteMenuItem_success() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_menu_it4@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "MenuDelete");
                Long itemId = addMenuItem(ownerToken, restaurantId, "Fries", 2.99, "STARTERS");

                mvc.perform(delete("/api/owner/menu-items/" + itemId)
                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isNoContent());

                // Verify item is no longer in the menu list
                mvc.perform(get("/api/restaurants/" + restaurantId + "/menu")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[*].id").value(not(hasItem(itemId.intValue()))));
        }

        @Test
        void test_toggleAvailability_success() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_menu_it5@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "MenuToggle");
                Long itemId = addMenuItem(ownerToken, restaurantId, "Ice Cream", 3.99, "DESSERTS");

                mvc.perform(patch("/api/owner/menu-items/" + itemId + "/availability")
                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isNoContent());
        }

        @Test
        void test_searchMenuItems() throws Exception {
                String ownerToken = registerAndLogin("Rajesh", "search_menu_test@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "SearchPlace");

                addMenuItem(ownerToken, restaurantId, "Chicken Pizza", 8.99, "PIZZA");
                addMenuItem(ownerToken, restaurantId, "Veg Pizza", 7.99, "PIZZA");
                addMenuItem(ownerToken, restaurantId, "Pasta", 6.99, "ITALIAN");

                mvc.perform(get("/api/menu-items/search?q=pizza")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
        }

        @Test
        void test_getPopularItems_paginated() throws Exception {
                String ownerToken = registerAndLogin("Ramesh", "popular_menu_test@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "PopularPlace");

                addMenuItem(ownerToken, restaurantId, "Burger", 5.99, "BURGER");
                addMenuItem(ownerToken, restaurantId, "Fries", 2.99, "STARTERS");
                addMenuItem(ownerToken, restaurantId, "Shake", 3.99, "BEVERAGES");

                mvc.perform(get("/api/menu-items/popular?page=0&size=2")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)))
                                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(1)))
                                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
        }
}
