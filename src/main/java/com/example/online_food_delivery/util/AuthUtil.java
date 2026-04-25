package com.example.online_food_delivery.util;

import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUtil {
    private final UserRepository userrepo;

    public User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();
        User user = userrepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));;
        return user;
    }
}




