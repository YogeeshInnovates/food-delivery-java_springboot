package com.example.online_food_delivery.config.security;

import com.example.online_food_delivery.dto.authdto.UserResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.rmi.server.ServerCloneException;


@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailService customUserDetailService;

    @Override
    protected  void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterchain) throws ServletException, IOException  {
final String authorizationHeader = request.getHeader("Authorization");

String username = null;;
String jwt = null;


    if(authorizationHeader !=null && authorizationHeader.startsWith("Bearer ")){
        jwt = authorizationHeader.substring(7);
        try{
            username = jwtUtil.extractUsername(jwt);
        }catch (Exception e){
            // I changed here: added print to see why JWT extraction might fail
            System.out.println("JWT Extraction Error: " + e.getMessage());
        }
    }

    if(username !=null && SecurityContextHolder.getContext().getAuthentication() == null){
        UserDetails userdetails = this.customUserDetailService.loadUserByUsername(username);

        if(jwtUtil.validateToken(jwt,userdetails)){
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                   userdetails, null,userdetails.getAuthorities()
            );
            usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            
            // I changed here: added print to see what authorities (roles) are assigned
            System.out.println("User Authorities set: " + usernamePasswordAuthenticationToken.getAuthorities());
        }
    }
    filterchain.doFilter(request,response);
    }


}
