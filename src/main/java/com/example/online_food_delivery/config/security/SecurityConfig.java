package com.example.online_food_delivery.config.security;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
private final CustomUserDetailService  customUserDetailService;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
  http
          .csrf(AbstractHttpConfigurer::disable)
          .authorizeHttpRequests(auth->auth
                  .requestMatchers("/api/auth/**").permitAll()
//                  .requestMatchers("/api/restaurant/add_restaurant").hasRole("OWNER")
                  .requestMatchers("/api/restaurant/**").permitAll()
          .anyRequest().authenticated())
          .sessionManagement(session-> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authenticationProvider(daoAuthenticationProvider())
          .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);


  return http.build();
   }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(){

        DaoAuthenticationProvider authprovider = new DaoAuthenticationProvider(customUserDetailService);
        authprovider.setPasswordEncoder(passwordEncoder());
        return authprovider;
    }

   @Bean
   public AuthenticationManager authenticationmanager(AuthenticationConfiguration config) throws  Exception{
       return  config.getAuthenticationManager();
   }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
