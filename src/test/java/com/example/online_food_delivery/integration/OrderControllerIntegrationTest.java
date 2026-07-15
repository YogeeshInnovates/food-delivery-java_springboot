package com.example.online_food_delivery.integration;

import com.example.online_food_delivery.dto.authdto.LoginRequest;
import com.example.online_food_delivery.dto.authdto.LoginResponse;
import com.example.online_food_delivery.dto.authdto.UserRequest;
import com.example.online_food_delivery.dto.cart_dto.CartItemRequest;
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
class OrderControllerIntegrationTest {

        @Autowired
        private MockMvc mvc;

        private final ObjectMapper json = new ObjectMapper()
                        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        private String registerAndLogin(String name, String email,
                        String password, String role) throws Exception {
                UserRequest reg = new UserRequest();
                reg.setName(name);
                reg.setEmail(email);
                reg.setPassword(password);
                reg.setPhoneNumber("9876543210");
                reg.setAddress("Bangalore");

                String registerUrl = role.equals("OWNER")
                                ? "/api/auth/owner/register"
                                : "/api/auth/register";

                mvc.perform(post(registerUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(reg)))
                                .andExpect(status().isOk());

                LoginRequest login = new LoginRequest();
                login.setEmail(email);
                login.setPassword(password);

                MvcResult loginResult = mvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andReturn();

                LoginResponse resp = json.readValue(
                                loginResult.getResponse().getContentAsString(),
                                LoginResponse.class);
                return resp.getToken();
        }

        private Long createRestaurant(String ownerToken, String name) throws Exception {
                String payload = String.format(
                                "{\"name\":\"%s\",\"city\":\"Bangalore\",\"cuisineType\":\"Indian\",\"address\":\"Test Address\"}",
                                name);

                MvcResult result = mvc.perform(post("/api/owner/restaurants")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andReturn();

                return json.readTree(result.getResponse().getContentAsString())
                                .path("id").asLong();
        }

        private Long addMenuItem(Long restaurantId, String ownerToken) throws Exception {
                String payload = "{\"name\":\"Pizza\",\"price\":9.99,\"category\":\"PIZZA\",\"description\":\"Delicious pizza\"}";

                MvcResult result = mvc.perform(post("/api/owner/restaurants/" + restaurantId + "/menu-items")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andReturn();

                return json.readTree(result.getResponse().getContentAsString())
                                .path("id").asLong();
        }

        private void addToCart(String token, Long menuItemId) throws Exception {
                CartItemRequest req = new CartItemRequest();
                req.setMenuItemId(menuItemId);
                req.setQuantity(1);

                mvc.perform(post("/api/cart/items")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                                .andExpect(status().isOk());
        }

        @Test
        void test_placeOrder_success() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner1@test.com",
                                "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "Spice Garden");
                Long menuItemId = addMenuItem(restaurantId, ownerToken);

                String custToken = registerAndLogin("Ram", "cust1@test.com",
                                "pass123", "CUSTOMER");
                addToCart(custToken, menuItemId);

                mvc.perform(post("/api/orders")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists());
        }

        @Test
        void test_getMyOrders() throws Exception {
                String token = registerAndLogin("Ram", "cust2@test.com",
                                "pass123", "CUSTOMER");

                mvc.perform(get("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void test_getOrderDetails_notFound() throws Exception {
                String token = registerAndLogin("Ram", "cust3@test.com",
                                "pass123", "CUSTOMER");

                mvc.perform(get("/api/orders/9999")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());
        }

        @Test
        void test_getOrderDetails_found() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner2@test.com",
                                "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "Tasty Corner");
                Long menuItemId = addMenuItem(restaurantId, ownerToken);

                String custToken = registerAndLogin("Ram", "cust4@test.com",
                                "pass123", "CUSTOMER");
                addToCart(custToken, menuItemId);

                MvcResult orderResult = mvc.perform(post("/api/orders")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andReturn();

                Long orderId = json.readTree(orderResult.getResponse().getContentAsString())
                                .path("id").asLong();

                mvc.perform(get("/api/orders/" + orderId)
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(orderId.intValue())));
        }

        @Test
        void test_updateOrderStatus_success() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner3@test.com",
                                "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "Flavor Town");
                Long menuItemId = addMenuItem(restaurantId, ownerToken);

                String custToken = registerAndLogin("Ram", "cust5@test.com",
                                "pass123", "CUSTOMER");
                addToCart(custToken, menuItemId);

                MvcResult placeResult = mvc.perform(post("/api/orders")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isCreated())
                                .andReturn();

                Long orderId = json.readTree(placeResult.getResponse().getContentAsString())
                                .path("id").asLong();

                mvc.perform(patch("/api/orders/" + orderId + "/status")
                                .param("status", "PREPARING")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("PREPARING")));
        }

        @Test
        void test_cancelOrder_success() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner4@test.com",
                                "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "Quick Bites");
                Long menuItemId = addMenuItem(restaurantId, ownerToken);

                String custToken = registerAndLogin("Ram", "cust6@test.com",
                                "pass123", "CUSTOMER");
                addToCart(custToken, menuItemId);

                MvcResult placeResult = mvc.perform(post("/api/orders")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isCreated())
                                .andReturn();

                Long orderId = json.readTree(placeResult.getResponse().getContentAsString())
                                .path("id").asLong();

                mvc.perform(post("/api/orders/" + orderId + "/cancel")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Changed mind\"}"))
                                .andExpect(status().isOk());
        }

        @Test
        void test_getMyTotalSpend() throws Exception {
                String ownerToken = registerAndLogin("Suresh", "owner5@test.com",
                                "ownerpass", "OWNER");
                Long restaurantId = createRestaurant(ownerToken, "Spend Test");
                Long menuItemId = addMenuItem(restaurantId, ownerToken);

                String custToken = registerAndLogin("Ram", "cust7@test.com",
                                "pass123", "CUSTOMER");
                addToCart(custToken, menuItemId);

                MvcResult orderRes = mvc.perform(post("/api/orders")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isCreated())
                                .andReturn();

                Long orderId = json.readTree(orderRes.getResponse().getContentAsString()).path("id").asLong();

                mvc.perform(patch("/api/orders/" + orderId + "/status")
                                .header("Authorization", "Bearer " + ownerToken)
                                .param("status", "DELIVERED")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                mvc.perform(get("/api/orders/summary")
                                .header("Authorization", "Bearer " + custToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", greaterThanOrEqualTo(9.0)));
        }
}
