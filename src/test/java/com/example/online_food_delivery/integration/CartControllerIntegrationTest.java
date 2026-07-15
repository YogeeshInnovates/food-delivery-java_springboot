package com.example.online_food_delivery.integration;

import com.example.online_food_delivery.dto.authdto.LoginRequest;
import com.example.online_food_delivery.dto.authdto.LoginResponse;
import com.example.online_food_delivery.dto.authdto.UserRequest;
import com.example.online_food_delivery.dto.cart_dto.CartItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class CartControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper json = new ObjectMapper();

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

        private Long createRestaurant(String ownerToken, String name) throws Exception {
                String payload = String.format(
                                "{\"name\":\"%s\",\"city\":\"Bangalore\",\"cuisineType\":\"Indian\",\"address\":\"Test Address\"}",
                                name);
                MvcResult result = mockMvc.perform(post("/api/owner/restaurants")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andReturn();
                return json.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        }

        private Long addMenuItem(Long restaurantId, String ownerToken, String itemName) throws Exception {
                String payload = String.format(
                                "{\"name\":\"%s\",\"price\":9.99,\"category\":\"MAIN_COURSE\",\"description\":\"Delicious\"}",
                                itemName);
                MvcResult result = mockMvc.perform(post("/api/owner/restaurants/" + restaurantId + "/menu-items")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andReturn();
                return json.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        }

        @Test
        void test_addItemToCart_and_getCart() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_cart1@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "Cart Rest");
                Long menuItemId = addMenuItem(restaurantId, ownerToken, "Burger");

                String custToken = registerAndLogin("Customer", "cust_cart1@test.com", "custpass", "CUSTOMER");

                CartItemRequest req = new CartItemRequest();
                req.setMenuItemId(menuItemId);
                req.setQuantity(2);

                mockMvc.perform(post("/api/cart/items")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/cart")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].menuItemId").value(menuItemId))
                                .andExpect(jsonPath("$[0].quantity", greaterThanOrEqualTo(2)));
        }

        @Test
        void test_updateItemQuantity() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_cart2@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "Cart Rest 2");
                Long menuItemId = addMenuItem(restaurantId, ownerToken, "Pizza");

                String custToken = registerAndLogin("Customer", "cust_cart2@test.com", "custpass", "CUSTOMER");

                CartItemRequest req = new CartItemRequest();
                req.setMenuItemId(menuItemId);
                req.setQuantity(1);

                mockMvc.perform(post("/api/cart/items")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                                .andExpect(status().isOk());

                mockMvc.perform(put("/api/cart/items/" + menuItemId)
                                .param("quantity", "5")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/cart")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].quantity").value(5));
        }

        @Test
        void test_removeItemFromCart() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_cart3@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "Cart Rest 3");
                Long menuItemId = addMenuItem(restaurantId, ownerToken, "Fries");

                String custToken = registerAndLogin("Customer", "cust_cart3@test.com", "custpass", "CUSTOMER");

                CartItemRequest req = new CartItemRequest();
                req.setMenuItemId(menuItemId);
                req.setQuantity(1);

                mockMvc.perform(post("/api/cart/items")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                                .andExpect(status().isOk());

                mockMvc.perform(delete("/api/cart/items/" + menuItemId)
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/cart")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void test_clearCart() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner_cart4@test.com", "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "Cart Rest 4");
                Long menuItemId1 = addMenuItem(restaurantId, ownerToken, "Coke");
                Long menuItemId2 = addMenuItem(restaurantId, ownerToken, "Pepsi");

                String custToken = registerAndLogin("Customer", "cust_cart4@test.com", "custpass", "CUSTOMER");

                mockMvc.perform(post("/api/cart/items")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(new CartItemRequest(menuItemId1, 1))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/cart/items")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(new CartItemRequest(menuItemId2, 2))))
                                .andExpect(status().isOk());

                mockMvc.perform(delete("/api/cart")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/cart")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));
        }
}
