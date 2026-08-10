package com.fintech.wallet.auth.controller;

import com.fintech.wallet.auth.dto.RegisterDto;
import com.fintech.wallet.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerDto", new RegisterDto("", "", "", ""));
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("registerDto") RegisterDto dto,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/register :: #register-form-container";
        }

        try {
            authService.registerUser(dto);
            return "redirect:/auth/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register :: #register-form-container";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }
}