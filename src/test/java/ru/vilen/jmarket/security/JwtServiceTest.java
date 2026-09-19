package ru.vilen.jmarket.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import ru.vilen.jmarket.config.JwtProperties;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private Jwt encodedJwt;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("unused-test-secret", ACCESS_TOKEN_TTL);
        jwtService = new JwtService(jwtEncoder, jwtProperties);
    }

    @Test
    void generateAccessToken_shouldReturnEncodedTokenWithExpectedClaims() {
        String subject = "ivan@example.com";
        String tokenValue = "encoded-access-token";

        when(encodedJwt.getTokenValue()).thenReturn(tokenValue);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encodedJwt);

        Instant beforeGeneration = Instant.now();
        String actual = jwtService.generateAccessToken(subject);
        Instant afterGeneration = Instant.now();

        assertThat(actual).isEqualTo(tokenValue);

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        JwtClaimsSet claims = captor.getValue().getClaims();
        assertThat(claims.getSubject()).isEqualTo(subject);
        assertThat(claims.getIssuedAt())
                .isBetween(beforeGeneration, afterGeneration);
        assertThat(claims.getExpiresAt())
                .isEqualTo(claims.getIssuedAt().plus(ACCESS_TOKEN_TTL));
    }

    @Test
    void generateAccessToken_shouldPropagateException_whenEncodingFails() {
        String subject = "ivan@example.com";
        JwtEncodingException exception = new JwtEncodingException("Encoding failed");

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> jwtService.generateAccessToken(subject))
                .isSameAs(exception);

        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }
}
