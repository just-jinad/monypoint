package monypoint.demo.service;

import monypoint.demo.entity.Account;
import monypoint.demo.entity.User;
import monypoint.demo.repository.AccountRepository;
import monypoint.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public void registerUser(String username, String password, String email, String phoneNumber) {
        logger.debug("Attempting to register user: username={}, email={}, phoneNumber={}, password=****", username,
                email, phoneNumber);
        // Ensure password format matches login expectation (e.g., 4-digit PIN as
        // string)
        // If not, login will fail even with correct input.

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setEmailVerified(false); // Email verification pending
        user.setPhoneVerified(true); // Phone verified via OTP in registration
        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(10000.00);
        accountRepository.save(account);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).orElse(null);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public boolean isUserAuthenticated(User user) {
        return user != null; // Email verification deferred
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}