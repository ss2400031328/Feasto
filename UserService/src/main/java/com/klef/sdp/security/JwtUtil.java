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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil
{
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    public String extractUsername(String token)
    {
        return extractClaim(
            token,
            Claims::getSubject
        );
    }

    public String extractRole(String token)
    {
        return extractClaim(
            token,
            claims -> claims.get(
                "role",
                String.class
            )
        );
    }

    public <T> T extractClaim(
        String token,
        Function<Claims, T> resolver
    )
    {
        return resolver.apply(
            extractAllClaims(token)
        );
    }

    public String generateToken(
        UserDetails userDetails
    )
    {
        Map<String, Object> claims =
                new HashMap<>();

        if (!userDetails.getAuthorities().isEmpty())
        {
            claims.put(
                "role",
                userDetails.getAuthorities()
                    .iterator()
                    .next()
                    .getAuthority()
            );
        }

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(
                    new Date(
                        System.currentTimeMillis()
                            + expiration
                    )
                )
                .signWith(getSignInKey())
                .compact();
    }

    public boolean validateToken(
        String token,
        UserDetails userDetails
    )
    {
        try
        {
            String username =
                    extractUsername(token);

            return username.equals(
                    userDetails.getUsername()
                )
                && !isTokenExpired(token);
        }
        catch (JwtException |
               IllegalArgumentException e)
        {
            return false;
        }
    }

    private boolean isTokenExpired(String token)
    {
        return extractClaim(
            token,
            Claims::getExpiration
        ).before(new Date());
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
        return Keys.hmacShaKeyFor(
            secretKey.getBytes(
                StandardCharsets.UTF_8
            )
        );
    }
}