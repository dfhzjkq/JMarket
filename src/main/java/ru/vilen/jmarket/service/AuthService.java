package ru.vilen.jmarket.service;

import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import ru.vilen.jmarket.config.JwtProperties;
import ru.vilen.jmarket.dto.LoginRequest;
import ru.vilen.jmarket.dto.TokenResponse;
import ru.vilen.jmarket.security.JwtService;

@Service
@AllArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProperties jwtProperties;
    private final JwtService jwtService;

    private Authentication authenticate(LoginRequest request) {
        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.email(),
                        request.password()
                );
        return authenticationManager.authenticate(token);
    }

    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticate(request);

        String accessToken =
                jwtService.generateAccessToken(authentication.getName());

        return new TokenResponse(
                accessToken,
                "Bearer",
                jwtProperties.accessTokenTtl().toSeconds()
        );
    }
}
