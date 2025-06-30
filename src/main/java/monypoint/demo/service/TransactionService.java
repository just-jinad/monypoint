package monypoint.demo.service;

import monypoint.demo.entity.Account;
import monypoint.demo.entity.User;
import monypoint.demo.entity.Transaction;
import monypoint.demo.repository.AccountRepository;
import monypoint.demo.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public Transaction createTransfer(Long userId, String recipientAccountNumber, Double amount, String description, String pin) {
        Account senderAccount = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        User user = senderAccount.getUser();
        if (!passwordEncoder.matches(pin, user.getPassword())) {
            throw new SecurityException("Invalid PIN");
        }

        if (amount < 100 || amount > senderAccount.getBalance()) {
            throw new IllegalArgumentException("Invalid amount: Must be ≥ ₦100 and ≤ balance");
        }

        Account recipientAccount = accountRepository.findByAccountNumber(recipientAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        senderAccount.setBalance(senderAccount.getBalance() - amount);
        recipientAccount.setBalance(recipientAccount.getBalance() + amount);

        Transaction transaction = new Transaction();
        transaction.setAccount(senderAccount);
        transaction.setType(Transaction.TransactionType.BANK_TRANSFER);
        transaction.setAmount(amount);
        transaction.setCounterparty(recipientAccountNumber);
        transaction.setDescription(description);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        accountRepository.save(senderAccount);
        accountRepository.save(recipientAccount);
        return transaction;
    }

    @Transactional
    public Transaction createTopUp(Long userId, Double amount, String paymentMethod, String cardDetails, String pin) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        User user = account.getUser();
        if (!passwordEncoder.matches(pin, user.getPassword())) {
            throw new SecurityException("Invalid PIN");
        }

        if (amount < 100) {
            throw new IllegalArgumentException("Invalid amount: Must be ≥ ₦100");
        }

        // Mock payment processing (e.g., card validation)
        if (!cardDetails.matches("\\d{16};\\d{2}/\\d{4};\\d{3,4}")) {
            throw new IllegalArgumentException("Invalid card details: Format 16-digit;MM/YYYY;CVV");
        }

        account.setBalance(account.getBalance() + amount);
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(Transaction.TransactionType.AIRTIME_DATA);
        transaction.setAmount(amount);
        transaction.setCounterparty(paymentMethod);
        transaction.setDescription("Top-up via " + paymentMethod);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        accountRepository.save(account);
        return transaction;
    }

    @Transactional
    public Transaction createBillPayment(Long userId, String biller, String customerId, Double amount, String pin) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        User user = account.getUser();
        if (!passwordEncoder.matches(pin, user.getPassword())) {
            throw new SecurityException("Invalid PIN");
        }

        if (amount < 100 || amount > account.getBalance()) {
            throw new IllegalArgumentException("Invalid amount: Must be ≥ ₦100 and ≤ balance");
        }

        // Mock biller validation
        if (!biller.matches("PHCN|MTN|DSTV")) {
            throw new IllegalArgumentException("Invalid biller");
        }

        account.setBalance(account.getBalance() - amount);
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(Transaction.TransactionType.BILL_PAYMENT);
        transaction.setAmount(amount);
        transaction.setCounterparty(customerId);
        transaction.setDescription("Payment to " + biller);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        accountRepository.save(account);
        return transaction;
    }

    @Transactional
    public Transaction createQRPayment(Long userId, String qrCode, Double amount, String pin) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        User user = account.getUser();
        if (!passwordEncoder.matches(pin, user.getPassword())) {
            throw new SecurityException("Invalid PIN");
        }

        if (amount < 100 || amount > account.getBalance()) {
            throw new IllegalArgumentException("Invalid amount: Must be ≥ ₦100 and ≤ balance");
        }

        // Mock QR validation
        if (qrCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid QR code");
        }

        account.setBalance(account.getBalance() - amount);
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(Transaction.TransactionType.QR_PAYMENT);
        transaction.setAmount(amount);
        transaction.setCounterparty(qrCode);
        transaction.setDescription("QR payment");
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        accountRepository.save(account);
        return transaction;
    }
}