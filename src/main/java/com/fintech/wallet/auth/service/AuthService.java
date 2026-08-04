package com.fintech.wallet.auth.service;

import com.fintech.wallet.auth.dto.RegisterDto;
import com.fintech.wallet.auth.entity.Role;
import com.fintech.wallet.auth.entity.User;
import com.fintech.wallet.auth.repository.RoleRepository;
import com.fintech.wallet.auth.repository.UserRepository;
import com.fintech.wallet.wallet.entity.Wallet;
import com.fintech.wallet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(RegisterDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Email address is already registered");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Default ROLE_USER not found in database"));

        User user = User.builder()
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .email(dto.email().toLowerCase().trim())
                .password(passwordEncoder.encode(dto.password()))
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        User savedUser = userRepository.save(user);

        // Crear billetera asociada
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .balance(BigDecimal.ZERO)
                .currency("USD")
                .build();

        walletRepository.save(wallet);

        return savedUser;
    }
}