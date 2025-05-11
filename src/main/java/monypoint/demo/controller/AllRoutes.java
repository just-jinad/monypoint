package monypoint.demo.controller;

import monypoint.demo.entity.User;
import monypoint.demo.service.UserService;
import monypoint.demo.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;

@Controller
public class AllRoutes {
    @Autowired
    private UserService userService;
    @Autowired
    private VerificationService verificationService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/personal")
    public String personal() {
        return "personal";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("phoneNumber", "");
        return "register";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/register/step1")
    public String handleStep1(@RequestParam String phoneNumber, @RequestParam boolean terms, Model model) {
        if (!terms) {
            model.addAttribute("error", "You must agree to the terms and conditions");
            model.addAttribute("phoneNumber", phoneNumber);
            return "register";
        }
        if (!phoneNumber.matches("^\\+234[0-9]{10}$")) {
            model.addAttribute("error", "Invalid phone number. Must be +234 followed by 10 digits");
            model.addAttribute("phoneNumber", phoneNumber);
            return "register";
        }
        try {
            verificationService.sendOtp(phoneNumber);
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 2);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to send OTP. Please try again.");
            model.addAttribute("phoneNumber", phoneNumber);
        }
        return "register";
    }

    @PostMapping("/register/step2")
    public String handleStep2(@RequestParam String phoneNumber, @RequestParam String otp1,
                             @RequestParam String otp2, @RequestParam String otp3,
                             @RequestParam String otp4, @RequestParam String otp5,
                             @RequestParam String otp6, Model model) {
        String otp = otp1 + otp2 + otp3 + otp4 + otp5 + otp6;
        if (verificationService.verifyOtp(phoneNumber, otp)) {
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 3);
        } else {
            model.addAttribute("error", "Invalid OTP");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 2);
        }
        return "register";
    }

    @PostMapping("/register/step3")
    public String handleStep3(@RequestParam String phoneNumber, @RequestParam String email, Model model) {
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            model.addAttribute("error", "Invalid email address");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 3);
            return "register";
        }
        try {
            verificationService.sendEmailVerification(email);
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("email", email);
            model.addAttribute("step", 4);
        } catch (MessagingException e) {
            model.addAttribute("error", "Failed to send verification email");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 3);
        }
        return "register";
    }

    @PostMapping("/register/complete")
    public String handleComplete(@RequestParam String phoneNumber, @RequestParam String email,
                                @RequestParam String username, @RequestParam String passcode1,
                                @RequestParam String passcode2, @RequestParam String passcode3,
                                @RequestParam String passcode4, Model model) {
        String passcode = passcode1 + passcode2 + passcode3 + passcode4;
        if (!passcode.matches("\\d{4}")) {
            model.addAttribute("error", "Passcode must be 4 digits");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("email", email);
            model.addAttribute("step", 4);
            return "register";
        }
        try {
            userService.registerUser(username, passcode, email, phoneNumber);
            return "redirect:/login"; // Redirect to login on success
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("email", email);
            model.addAttribute("step", 4);
            return "register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String email, @RequestParam String token, Model model) {
        if (verificationService.verifyEmailToken(email, token)) {
            User user = userService.findByEmail(email);
            if (user != null) {
                user.setEmailVerified(true);
                userService.save(user);
                model.addAttribute("message", "Email verified successfully. Please log in.");
                return "login";
            }
        }
        model.addAttribute("error", "Invalid or expired verification token");
        return "register";
    }
}