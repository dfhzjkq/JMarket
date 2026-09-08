package ru.vilen.jmarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.vilen.jmarket.dto.UserCreateRequest;
import ru.vilen.jmarket.dto.UserResponse;
import ru.vilen.jmarket.entity.User;
import ru.vilen.jmarket.exception.EmailAlreadyExistsException;
import ru.vilen.jmarket.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void create_shouldReturnUserResponse_whenEmailDoesNotExist() {
        UserCreateRequest request = new UserCreateRequest(
                "ivan",
                "ivan@example.com",
                "password"
        );

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("ivan");
        savedUser.setEmail("ivan@example.com");

        when(passwordEncoder.encode(request.password()))
                .thenReturn("encoded-password");
        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        UserResponse actual = userService.create(request);
        assertThat(actual.id()).isEqualTo(1L);
        assertThat(actual.name()).isEqualTo("ivan");
        assertThat(actual.email()).isEqualTo("ivan@example.com");

        verify(userRepository).existsByEmail(request.email());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(passwordEncoder).encode(request.password());
        verify(userRepository).save(captor.capture());

        User actualUserToSave = captor.getValue();
        assertThat(actualUserToSave.getName()).isEqualTo(request.name());
        assertThat(actualUserToSave.getEmail()).isEqualTo(request.email());
        assertThat(actualUserToSave.getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void create_shouldThrowEmailAlreadyExistsException_whenEmailAlreadyExists() {
        UserCreateRequest request = new UserCreateRequest(
                "ivan",
                "ivan@example.com",
                "password"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("User with email ivan@example.com already exists");

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}
