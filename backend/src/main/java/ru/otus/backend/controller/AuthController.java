package ru.otus.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.backend.model.AuthRequest;
import ru.otus.backend.model.AuthResponse;
import ru.otus.backend.model.User;
import ru.otus.backend.service.JwtService;
import ru.otus.backend.service.UserService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> createAuthenticationToken(@RequestBody AuthRequest authRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
        } catch (Exception e) {
            throw new Exception("Incorrect username or password", e);
        }

        User user = userService.findByUsername(authRequest.getUsername());
        String jwt = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}
