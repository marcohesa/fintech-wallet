package com.fintech.wallet.wallet.service;

import com.fintech.wallet.auth.entity.User;
import com.fintech.wallet.auth.repository.UserRepository;
import com.fintech.wallet.wallet.dto.TransferRequestDto;
import com.fintech.wallet.wallet.entity.Wallet;
import com.fintech.wallet.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrentTransferTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    private Long sourceUserId;
    private Long targetUserId;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
        userRepository.deleteAll();

        User source = userRepository.save(User.builder().firstName("Alice").lastName("Sender").email("alice@test.com").password("pass").build());
        User target = userRepository.save(User.builder().firstName("Bob").lastName("Receiver").email("bob@test.com").password("pass").build());

        sourceUserId = source.getId();
        targetUserId = target.getId();

        walletRepository.save(Wallet.builder().user(source).balance(new BigDecimal("100.00")).currency("USD").build());
        walletRepository.save(Wallet.builder().user(target).balance(new BigDecimal("0.00")).currency("USD").build());
    }

    @Test
    void shouldHandleConcurrentTransfersWithoutRaceCondition() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        // Intentar transferir $20 en 10 hilos simultáneos (Total solicitado: $200, pero solo hay $100)
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    latch.await(); // Espera a que todos los hilos estén listos para arrancar al mismo milisegundo
                    TransferRequestDto dto = new TransferRequestDto("bob@test.com", new BigDecimal("20.00"), null);
                    transferService.executeTransfer(sourceUserId, dto);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // Esperamos que exactamente 5 tengan éxito y 5 fallen por saldo insuficiente
                }
            });
        }

        latch.countDown(); // Liberar todos los hilos simultáneamente
        executorService.shutdown();

        // Esperar a que todos los hilos terminen
        while (!executorService.isTerminated()) {
            Thread.sleep(50);
        }

        Wallet finalSourceWallet = walletRepository.findByUserId(sourceUserId).orElseThrow();
        Wallet finalTargetWallet = walletRepository.findByUserId(targetUserId).orElseThrow();

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(finalSourceWallet.getBalance()).isEqualByComparingTo("0.00");
        assertThat(finalTargetWallet.getBalance()).isEqualByComparingTo("100.00");
    }
}