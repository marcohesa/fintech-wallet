package com.fintech.wallet.auth.service;

import com.fintech.wallet.auth.dto.RegisterDto;
import com.fintech.wallet.auth.entity.Role;
import com.fintech.wallet.auth.entity.User;
import com.fintech.wallet.auth.repository.RoleRepository;
import com.fintech.wallet.auth.repository.UserRepository;
import com.fintech.wallet.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterDto registerDto;

    @BeforeEach
    void setUp() {
        registerDto = new RegisterDto("Marco", "Hernandez", "marco@test.com", "password123");
    }

    @Test
    void shouldRegisterUserAndCreateWalletSuccessfully() {
        when(userRepository.existsByEmail(registerDto.email())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(new Role(1L, "ROLE_USER")));
        when(passwordEncoder.encode(registerDto.password())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User registeredUser = authService.registerUser(registerDto);

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getEmail()).isEqualTo("marco@test.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(walletRepository, times(1)).save(any());
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerDto.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(registerDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email address is already registered");

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }
}