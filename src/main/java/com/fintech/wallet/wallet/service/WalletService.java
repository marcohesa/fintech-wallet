package com.fintech.wallet.wallet.service;

import com.fintech.wallet.wallet.entity.Wallet;
import com.fintech.wallet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "wallets", key = "#userId")
    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for user: " + userId));
    }

    @CacheEvict(value = "wallets", key = "#userId")
    public void evictWalletCache(Long userId) {
        // Método utilitario para invalidar la caché explícitamente
    }
}