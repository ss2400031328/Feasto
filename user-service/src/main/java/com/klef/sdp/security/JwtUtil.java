package com.klef.sdp.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil 
{
    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    public String extractUsername(String token) 
    {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) 
    {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) 
    {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) 
    {
        Map<String, Object> claims = new HashMap<>();
        if (!userDetails.getAuthorities().isEmpty()) 
        {
            claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        }
        return generateToken(claims, userDetails.getUsername());
    }

    public String generateToken(Map<String, Object> claims, String subject) 
    {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) 
    {
        try 
        {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } 
        catch (JwtException | IllegalArgumentException e) 
        {
            return false;
        }
    }

    private boolean isTokenExpired(String token) 
    {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) 
    {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) 
    {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() 
    {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
