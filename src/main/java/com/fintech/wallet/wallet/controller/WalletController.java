package com.fintech.wallet.wallet.controller;

import com.fintech.wallet.wallet.dto.TransferRequestDto;
import com.fintech.wallet.wallet.entity.Wallet;
import com.fintech.wallet.wallet.repository.WalletRepository;
import com.fintech.wallet.wallet.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final TransferService transferService;
    private final WalletRepository walletRepository;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        Wallet wallet = walletRepository.findByUserId(1L).orElseThrow();
        model.addAttribute("wallet", wallet);
        model.addAttribute("idempotencyKey", UUID.randomUUID().toString());
        return "wallet/dashboard";
    }

    @PostMapping("/transfer")
    public String handleTransfer(@Valid @ModelAttribute TransferRequestDto dto, Model model) {
        try {
            transferService.executeTransfer(1L, dto);
            model.addAttribute("successMessage", "Transferencia realizada con éxito.");
            return "wallet/fragments :: transfer-success";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "wallet/fragments :: transfer-error";
        }
    }
}