package com.klef.sdp.security;

import java.io.IOException;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.klef.sdp.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter
{
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public JwtFilter(
        JwtUtil jwtUtil,
        @Lazy UserService userService
    )
    {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException
    {
        String header =
                request.getHeader("Authorization");

        if (header == null ||
            !header.startsWith("Bearer "))
        {
            filterChain.doFilter(
                request,
                response
            );
            return;
        }

        String token =
                header.substring(7);

        try
        {
            String username =
                    jwtUtil.extractUsername(token);

            if (username != null &&
                SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null)
            {
                UserDetails userDetails =
                        userService
                            .loadUserByUsername(username);

                if (jwtUtil.validateToken(
                        token,
                        userDetails))
                {
                    UsernamePasswordAuthenticationToken
                        authentication =
                            new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                            .buildDetails(request)
                    );

                    SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                            authentication
                        );
                }
            }
        }
        catch (Exception e)
        {
            // Invalid JWT - request continues
        }

        filterChain.doFilter(
            request,
            response
        );
    }
}