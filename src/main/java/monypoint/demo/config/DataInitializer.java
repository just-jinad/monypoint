package monypoint.demo.config;

import monypoint.demo.entity.Account;
import monypoint.demo.entity.Transaction;
import monypoint.demo.entity.User;
import monypoint.demo.repository.AccountRepository;
import monypoint.demo.repository.TransactionRepository;
import monypoint.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Check if user already exists
        String username = "zee";
        if (userRepository.findByUsername(username).isEmpty()) {
            logger.info("Creating test user: {}", username);
            // Create test user
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("1234"));
            user.setEmail("zee@example.com");
            user.setPhoneNumber("+2341234567890");
            user.setEmailVerified(true);
            user.setPhoneVerified(true);
            userRepository.save(user);

            // Create test account
            Account account = new Account();
            account.setUser(user);
            account.setAccountNumber("1234567890");
            account.setBalance(10000.00);
            accountRepository.save(account);

            // Create test recipient account
            Account recipientAccount = new Account();
            recipientAccount.setUser(user);
            recipientAccount.setAccountNumber("0987654321");
            recipientAccount.setBalance(5000.00);
            accountRepository.save(recipientAccount);

            // Create test transactions
            Transaction tx1 = new Transaction();
            tx1.setAccount(account);
            tx1.setType(Transaction.TransactionType.BANK_TRANSFER);
            tx1.setAmount(1000.00);
            tx1.setCounterparty("0987654321");
            tx1.setDescription("Test transfer");
            tx1.setStatus(Transaction.TransactionStatus.COMPLETED);
            tx1.setCreatedAt(LocalDateTime.now().minusDays(1));
            transactionRepository.save(tx1);

            Transaction tx2 = new Transaction();
            tx2.setAccount(account);
            tx2.setType(Transaction.TransactionType.AIRTIME_DATA);
            tx2.setAmount(500.00);
            tx2.setCounterparty("MTN");
            tx2.setDescription("Airtime top-up");
            tx2.setStatus(Transaction.TransactionStatus.COMPLETED);
            tx2.setCreatedAt(LocalDateTime.now().minusDays(2));
            transactionRepository.save(tx2);

            Transaction tx3 = new Transaction();
            tx3.setAccount(account);
            tx3.setType(Transaction.TransactionType.BILL_PAYMENT);
            tx3.setAmount(2000.00);
            tx3.setCounterparty("DSTV");
            tx3.setDescription("Cable subscription");
            tx3.setStatus(Transaction.TransactionStatus.COMPLETED);
            tx3.setCreatedAt(LocalDateTime.now().minusDays(3));
            transactionRepository.save(tx3);

            logger.info("Test data initialized successfully for user: {}", username);
        } else {
            logger.info("User {} already exists, skipping initialization", username);
        }
    }
}