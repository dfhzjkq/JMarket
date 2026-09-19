package ru.vilen.jmarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import ru.vilen.jmarket.config.JwtProperties;
import ru.vilen.jmarket.dto.LoginRequest;
import ru.vilen.jmarket.dto.TokenResponse;
import ru.vilen.jmarket.security.JwtService;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnTokenResponse_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest(
                "ivan@example.com",
                "password"
        );
        Authentication expected = UsernamePasswordAuthenticationToken.authenticated(
                request.email(),
                null,
                List.of()
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(expected);
        when(jwtService.generateAccessToken(request.email()))
                .thenReturn("encoded-access-token");
        when(jwtProperties.accessTokenTtl())
                .thenReturn(Duration.ofMinutes(15));

        TokenResponse actual = authService.login(request);

        assertThat(actual.accessToken()).isEqualTo("encoded-access-token");
        assertThat(actual.tokenType()).isEqualTo("Bearer");
        assertThat(actual.expiresIn()).isEqualTo(900);

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());

        Authentication authenticationRequest = captor.getValue();
        assertThat(authenticationRequest.getPrincipal()).isEqualTo(request.email());
        assertThat(authenticationRequest.getCredentials()).isEqualTo(request.password());
        assertThat(authenticationRequest.isAuthenticated()).isFalse();
        verify(jwtService).generateAccessToken(request.email());
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest(
                "ivan@example.com",
                "wrong-password"
        );
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> authService.login(request))
                .isSameAs(exception);

        verify(authenticationManager).authenticate(any(Authentication.class));
        verifyNoInteractions(jwtService, jwtProperties);
    }
}
