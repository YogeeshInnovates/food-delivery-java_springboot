package com.example.online_food_delivery.config.security;

import com.example.online_food_delivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService  implements UserDetailsService{
private final UserRepository repo;

public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return repo.findByEmail(email).orElseThrow(()-> new UsernameNotFoundException("User not found"));
}



}
