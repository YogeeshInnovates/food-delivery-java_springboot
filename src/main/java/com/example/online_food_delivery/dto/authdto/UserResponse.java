package com.example.online_food_delivery.dto.authdto;

import com.example.online_food_delivery.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Role role;
    private Boolean isActive;
    private Date createdAt;
    private String address;
    private String profileImageUrl;
}
