package ru.otus.backend.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final HttpServletRequest request;

    @Autowired
    public JwtTokenProvider(HttpServletRequest request) {
        this.request = request;
    }

    public String getToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
