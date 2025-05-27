package monypoint.demo.controller;

import monypoint.demo.entity.User;
import monypoint.demo.entity.Account;
import monypoint.demo.service.UserService;
import monypoint.demo.service.VerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import jakarta.mail.MessagingException;

@Controller
public class AllRoutes {
    private static final Logger logger = LoggerFactory.getLogger(AllRoutes.class);

    @Autowired
    private UserService userService;
    @Autowired
    private VerificationService verificationService;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

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
        model.addAttribute("step", 1);
        logger.debug("Displaying registration form");
        return "register";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @PostMapping("/register/step1")
    public String handleStep1(@RequestParam String phoneNumber, @RequestParam boolean terms, Model model) {
        logger.debug("Processing Step 1 with phone number: {}", phoneNumber);
        if (!terms) {
            model.addAttribute("error", "You must agree to the terms and conditions.");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 1);
            logger.warn("Terms not accepted");
            return "register";
        }

        String normalizedPhone = phoneNumber.replaceAll("[^0-9+]", "");
        if (!normalizedPhone.matches("^\\+234[0-9]{10}$")) {
            model.addAttribute("error", "Invalid phone number. Must be +234 followed by 10 digits.");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 1);
            logger.warn("Invalid phone number format: {}", normalizedPhone);
            return "register";
        }

        try {
            String otp = verificationService.sendOtp(normalizedPhone);
            model.addAttribute("phoneNumber", normalizedPhone);
            model.addAttribute("otp", otp);
            model.addAttribute("step", 2);
            logger.info("OTP generated for phone: {}", normalizedPhone);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to generate OTP. Please try again.");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 1);
            logger.error("Error generating OTP for phone {}: {}", normalizedPhone, e.getMessage(), e);
        }
        return "register";
    }

    @PostMapping("/register/step2")
    public String handleStep2(@RequestParam String phoneNumber, @RequestParam String otp1,
            @RequestParam String otp2, @RequestParam String otp3,
            @RequestParam String otp4, @RequestParam String otp5,
            @RequestParam String otp6, Model model) {
        String otp = otp1 + otp2 + otp3 + otp4 + otp5 + otp6;
        logger.debug("Verifying OTP for phone {}: {}", phoneNumber, otp);
        if (verificationService.verifyOtp(phoneNumber, otp)) {
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 3);
            logger.info("OTP verified for phone: {}", phoneNumber);
        } else {
            model.addAttribute("error", "Invalid OTP.");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 2);
            logger.warn("Invalid OTP entered for phone: {}", phoneNumber);
        }
        return "register";
    }

    @PostMapping("/register/step3")
    public String handleStep3(@RequestParam String phoneNumber, @RequestParam String email, Model model) {
        logger.debug("Processing Step 3 with email: {}", email);
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            model.addAttribute("error", "Invalid email address.");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 3);
            logger.warn("Invalid email format: {}", email);
            return "register";
        }
        try {
            verificationService.sendEmailVerification(email);
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("email", email);
            model.addAttribute("step", 4);
            logger.info("Email verification sent to: {}", email);
        } catch (MessagingException e) {
            model.addAttribute("error", "Failed to send verification email.");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("step", 3);
            logger.error("Error sending email verification to {}: {}", email, e.getMessage());
        }
        return "register";
    }

    @PostMapping("/register/complete")
    public String handleComplete(@RequestParam String phoneNumber, @RequestParam String email,
            @RequestParam String username, @RequestParam String password1,
            @RequestParam String password2, @RequestParam String password3,
            @RequestParam String password4, Model model) {
        String password = password1 + password2 + password3 + password4;
        logger.debug("Processing registration completion for username: {}", username);
        if (!password.matches("\\d{4}")) {
            model.addAttribute("error", "Password must be 4 digits.");
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("email", email);
            model.addAttribute("step", 4);
            logger.warn("Invalid password format");
            return "register";
        }
        try {
            userService.registerUser(username, password, email, phoneNumber);
            logger.info("User registered successfully: {}", username);
            SecurityContextHolder.clearContext();
            return "redirect:/register-success";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("email", email);
            model.addAttribute("step", 4);
            logger.error("Error registering user {}: {}", username, e.getMessage(), e);
            return "register";
        }
    }

    @GetMapping("/register/regenerate-otp")
    @ResponseBody
    public String regenerateOtp(@RequestParam String phoneNumber) {
        logger.debug("Regenerating OTP for phone: {}", phoneNumber);
        String otp = verificationService.sendOtp(phoneNumber);
        logger.info("New OTP generated for phone: {}", phoneNumber);
        return otp;
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String email, @RequestParam String token, Model model) {
        logger.debug("Verifying email: {} with token: {}", email, token);
        if (verificationService.verifyEmailToken(email, token)) {
            User user = userService.findByEmail(email);
            if (user != null) {
                user.setEmailVerified(true);
                userService.save(user);
                model.addAttribute("message", "Email verified successfully. Please log in.");
                logger.info("Email verified for: {}", email);
                return "login";
            }
        }
        model.addAttribute("error", "Invalid or expired verification token.");
        logger.warn("Email verification failed for: {}", email);
        return "register";
    }

    @GetMapping("/register-success")
    public String registerSuccess(Model model) {
        logger.debug("Displaying registration success page");
        model.addAttribute("message", "Registration successful! Please verify your email and log in.");
        return "register-success";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
    return "dashboard";
    }

    
    @GetMapping("/about")
    public String about(Model model) {
    return "about";
    }

}