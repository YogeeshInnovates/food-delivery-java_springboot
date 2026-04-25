package com.example.online_food_delivery.config.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
   private final String secret;
   private final long expiration;
   private final Key keys;

   public JwtUtil(@Value("${jwt.secret}") String secret,@Value("${jwt.expiration}") long expiration){
this.secret = secret;
this.expiration = expiration;
this.keys = Keys.hmacShaKeyFor(secret.getBytes());
   }

   public String extractUsername(String token){
       return extractclaim(token,Claims::getSubject);
   }

   public Date extractExpiration(String token){
       return extractclaim(token,Claims::getExpiration);
   }

   public <T> T extractclaim(String token , Function<Claims,T> claimsResolver){
       final Claims claims =  extractAllClaims(token);
       return claimsResolver.apply(claims);
   }

   private Claims extractAllClaims(String token){
  return Jwts.parserBuilder().setSigningKey(keys).build().parseClaimsJws(token).getBody();
   }

   private Boolean isTokenExpired(String token){
       return extractExpiration(token).before(new Date());
   }

   public  String generateToken(UserDetails userDetails){
       Map<String,Object> claims = new HashMap<>();
       return createToken(claims,userDetails.getUsername());
   }

   private String createToken(Map<String,Object> claims,String subject){
       return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis()+expiration)).signWith(keys, SignatureAlgorithm.HS256).compact();
   }

public boolean validateToken(String token, UserDetails userDetails){
       final String username = extractUsername(token);
       return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
}

}
