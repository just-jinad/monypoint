package monypoint.demo.controller;

import monypoint.demo.entity.Account;
import monypoint.demo.entity.User;
import monypoint.demo.repository.AccountRepository;
import monypoint.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAccount(@RequestParam Long userId) {
        try {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!user.getId().equals(userId)) {
                logger.warn("Unauthorized account access attempt by userId: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Unauthorized access"));
            }
            Account account = accountRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found for userId: " + userId));
            account.setTransactions(null); // Prevent serialization issues
            logger.info("Fetched account for userId: {}, balance: {}", userId, account.getBalance());
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            logger.error("Error fetching account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error fetching account for userId {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch account"));
        }
    }
}