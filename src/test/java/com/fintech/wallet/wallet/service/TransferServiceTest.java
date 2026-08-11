package com.fintech.wallet.wallet.service;

import com.fintech.wallet.auth.entity.User;
import com.fintech.wallet.auth.repository.UserRepository;
import com.fintech.wallet.wallet.dto.TransferRequestDto;
import com.fintech.wallet.wallet.entity.Wallet;
import com.fintech.wallet.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransferService transferService;

    private User sourceUser;
    private User targetUser;
    private Wallet sourceWallet;
    private Wallet targetWallet;

    @BeforeEach
    void setUp() {
        sourceUser = User.builder().id(1L).email("source@wallet.com").build();
        targetUser = User.builder().id(2L).email("target@wallet.com").build();

        sourceWallet = Wallet.builder().id(10L).user(sourceUser).balance(new BigDecimal("500.00")).build();
        targetWallet = Wallet.builder().id(20L).user(targetUser).balance(new BigDecimal("100.00")).build();
    }

    @Test
    void shouldExecuteTransferSuccessfully() {
        TransferRequestDto dto = new TransferRequestDto("target@wallet.com", new BigDecimal("200.00"), "key-123");

        when(userRepository.findByEmail("target@wallet.com")).thenReturn(Optional.of(targetUser));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(targetWallet));
        when(walletRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByIdWithLock(20L)).thenReturn(Optional.of(targetWallet));

        transferService.executeTransfer(1L, dto);

        assertThat(sourceWallet.getBalance()).isEqualByComparingTo("300.00");
        assertThat(targetWallet.getBalance()).isEqualByComparingTo("300.00");
    }

    @Test
    void shouldThrowExceptionWhenInsufficientBalance() {
        TransferRequestDto dto = new TransferRequestDto("target@wallet.com", new BigDecimal("1000.00"), "key-123");

        when(userRepository.findByEmail("target@wallet.com")).thenReturn(Optional.of(targetUser));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(targetWallet));
        when(walletRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByIdWithLock(20L)).thenReturn(Optional.of(targetWallet));

        assertThatThrownBy(() -> transferService.executeTransfer(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Saldo insuficiente para completar la transferencia");
    }
}