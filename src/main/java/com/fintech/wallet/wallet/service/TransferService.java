package com.fintech.wallet.wallet.service;

import com.fintech.wallet.auth.entity.User;
import com.fintech.wallet.auth.repository.UserRepository;
import com.fintech.wallet.wallet.dto.TransferRequestDto;
import com.fintech.wallet.wallet.entity.Wallet;
import com.fintech.wallet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    // Inyectar WalletService o aplicar @CacheEvict directo
    private final WalletService walletService;

    @Transactional
    public void executeTransfer(Long sourceUserId, TransferRequestDto dto) {
        User targetUser = userRepository.findByEmail(dto.targetEmail().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("El destinatario no existe"));

        if (sourceUserId.equals(targetUser.getId())) {
            throw new IllegalArgumentException("No puedes realizar una transferencia a ti mismo");
        }

        // Ordenamiento explícito por ID de usuario para prevenir DEADLOCKS en transacciones concurrentes cruzadas
        Long firstUserId = sourceUserId < targetUser.getId() ? sourceUserId : targetUser.getId();
        Long secondUserId = sourceUserId < targetUser.getId() ? targetUser.getId() : sourceUserId;

        // Adquisición directa de bloqueos pesimistas para asegurar la lectura del saldo actualizado sin stale L1 cache
        Wallet firstWallet = walletRepository.findByUserIdWithLock(firstUserId)
                .orElseThrow(() -> new IllegalStateException("Billetera no encontrada para el usuario: " + firstUserId));
        Wallet secondWallet = walletRepository.findByUserIdWithLock(secondUserId)
                .orElseThrow(() -> new IllegalStateException("Billetera no encontrada para el usuario: " + secondUserId));

        Wallet sourceWallet = sourceUserId.equals(firstUserId) ? firstWallet : secondWallet;
        Wallet targetWallet = sourceUserId.equals(firstUserId) ? secondWallet : firstWallet;

        if (sourceWallet.getBalance().compareTo(dto.amount()) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para completar la transferencia");
        }

        // Actualización atómica del saldo
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(dto.amount()));
        targetWallet.setBalance(targetWallet.getBalance().add(dto.amount()));

        walletRepository.save(sourceWallet);
        walletRepository.save(targetWallet);

        walletService.evictWalletCache(sourceUserId);
        walletService.evictWalletCache(targetUser.getId());
    }
}