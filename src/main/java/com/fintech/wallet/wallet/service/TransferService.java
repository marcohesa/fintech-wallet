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

    @Transactional
    public void executeTransfer(Long sourceUserId, TransferRequestDto dto) {
        User targetUser = userRepository.findByEmail(dto.targetEmail().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("El destinatario no existe"));

        Wallet sourceWallet = walletRepository.findByUserId(sourceUserId)
                .orElseThrow(() -> new IllegalStateException("Billetera de origen no encontrada"));

        Wallet targetWallet = walletRepository.findByUserId(targetUser.getId())
                .orElseThrow(() -> new IllegalStateException("Billetera de destino no encontrada"));

        if (sourceWallet.getId().equals(targetWallet.getId())) {
            throw new IllegalArgumentException("No puedes realizar una transferencia a ti mismo");
        }

        // Ordenamiento explícito por ID para prevenir DEADLOCKS en transacciones concurrentes cruzadas
        Long firstId = sourceWallet.getId() < targetWallet.getId() ? sourceWallet.getId() : targetWallet.getId();
        Long secondId = sourceWallet.getId() < targetWallet.getId() ? targetWallet.getId() : sourceWallet.getId();

        Wallet firstLocked = walletRepository.findByIdWithLock(firstId).orElseThrow();
        Wallet secondLocked = walletRepository.findByIdWithLock(secondId).orElseThrow();

        Wallet lockedSource = sourceWallet.getId().equals(firstId) ? firstLocked : secondLocked;
        Wallet lockedTarget = targetWallet.getId().equals(firstId) ? firstLocked : secondLocked;

        if (lockedSource.getBalance().compareTo(dto.amount()) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para completar la transferencia");
        }

        // Actualización atómica del saldo
        lockedSource.setBalance(lockedSource.getBalance().subtract(dto.amount()));
        lockedTarget.setBalance(lockedTarget.getBalance().add(dto.amount()));

        walletRepository.save(lockedSource);
        walletRepository.save(lockedTarget);
    }
}