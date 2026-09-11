package ru.vilen.jmarket.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.vilen.jmarket.entity.User;
import ru.vilen.jmarket.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JMarketUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JMarketUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        String email = "ivan@example.com";
        String passwordHash = "$2b$12$R8Qm4WnVx0fEcYvS2G7mE.3JbH1eA9xXz6kKcLmNpQrStUvWxYz12";

        User user = new User();
        user.setName("ivan");
        user.setEmail(email);
        user.setPasswordHash(passwordHash);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(email);

        assertThat(result.getUsername()).isEqualTo(email);
        assertThat(result.getPassword()).isEqualTo(passwordHash);
        assertThat(result.getAuthorities()).isEmpty();
        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_shouldThrowException_whenUserDoesNotExist() {
        String email = "missing@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByEmail(email);
    }
}
