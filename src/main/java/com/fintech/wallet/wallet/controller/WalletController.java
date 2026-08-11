package com.fintech.wallet.wallet.controller;

import com.fintech.wallet.auth.entity.User;
import com.fintech.wallet.auth.repository.UserRepository;
import com.fintech.wallet.wallet.dto.TransferRequestDto;
import com.fintech.wallet.wallet.entity.Wallet;
import com.fintech.wallet.wallet.service.TransferService;
import com.fintech.wallet.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final TransferService transferService;
    private final WalletService walletService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String showDashboard(Principal principal, Model model) {
        // Obtenemos el email del usuario autenticado actualmente
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + email));

        // Consultamos la billetera utilizando la caché optimizada de Redis
        Wallet wallet = walletService.getWalletByUserId(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("wallet", wallet);
        model.addAttribute("idempotencyKey", UUID.randomUUID().toString());
        return "wallet/dashboard";
    }

    @PostMapping("/transfer")
    public String handleTransfer(
            Principal principal,
            @Valid @ModelAttribute TransferRequestDto dto,
            Model model) {
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

            // Transferencia atómica con el ID real del usuario en sesión
            transferService.executeTransfer(user.getId(), dto);

            model.addAttribute("successMessage", "Transferencia realizada con éxito.");
            return "wallet/fragments :: transfer-success";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "wallet/fragments :: transfer-error";
        }
    }
}