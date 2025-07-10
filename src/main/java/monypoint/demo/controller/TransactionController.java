package monypoint.demo.controller;

import monypoint.demo.entity.Account;
import monypoint.demo.entity.Transaction;
import monypoint.demo.entity.User;
import monypoint.demo.repository.AccountRepository;
import monypoint.demo.repository.TransactionRepository;
import monypoint.demo.repository.UserRepository;
import monypoint.demo.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> createTransfer(
            @RequestParam Long userId,
            @RequestParam String recipientAccountNumber,
            @RequestParam Double amount,
            @RequestParam(required = false) String description,
            @RequestParam String pin) {
        try {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!user.getId().equals(userId)) {
                logger.warn("Unauthorized transfer attempt by userId: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Unauthorized access"));
            }
            Transaction transaction = transactionService.createTransfer(
                    userId, recipientAccountNumber, amount, description, pin);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transfer successful");
            response.put("transaction", transaction);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | SecurityException e) {
            logger.error("Transfer failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected transfer error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Transfer failed"));
        }
    }

    @PostMapping("/top-up")
    public ResponseEntity<Map<String, Object>> createTopUp(
            @RequestParam Long userId,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @RequestParam String cardDetails,
            @RequestParam String pin) {
        try {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!user.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Unauthorized access"));
            }
            Transaction transaction = transactionService.createTopUp(
                    userId, amount, paymentMethod, cardDetails, pin);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Top-up successful");
            response.put("transaction", transaction);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Top-up failed"));
        }
    }

    @PostMapping("/bills")
    public ResponseEntity<Map<String, Object>> createBillPayment(
            @RequestParam Long userId,
            @RequestParam String biller,
            @RequestParam String customerId,
            @RequestParam Double amount,
            @RequestParam String pin) {
        try {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!user.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Unauthorized access"));
            }
            Transaction transaction = transactionService.createBillPayment(
                    userId, biller, customerId, amount, pin);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Bill payment successful");
            response.put("transaction", transaction);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Bill payment failed"));
        }
    }

    @PostMapping("/qr")
    public ResponseEntity<Map<String, Object>> createQRPayment(
            @RequestParam Long userId,
            @RequestParam String qrCode,
            @RequestParam Double amount,
            @RequestParam String pin) {
        try {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!user.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Unauthorized access"));
            }
            Transaction transaction = transactionService.createQRPayment(
                    userId, qrCode, amount, pin);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "QR payment successful");
            response.put("transaction", transaction);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "QR payment failed"));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportTransactions(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String type) {
        try {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!user.getId().equals(userId)) {
                throw new IllegalArgumentException("Unauthorized access");
            }
            Account account = accountRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            List<Transaction> transactions;
            if (startDate != null && endDate != null && type != null) {
                transactions = transactionRepository.findByAccountIdAndCreatedAtAndType(
                        account.getId(),
                        LocalDateTime.parse(startDate),
                        LocalDateTime.parse(endDate),
                        Transaction.TransactionType.valueOf(type));
            } else if (startDate != null && endDate != null) {
                transactions = transactionRepository.findByAccountIdAndCreatedAtBetween(
                        account.getId(),
                        LocalDateTime.parse(startDate),
                        LocalDateTime.parse(endDate));
            } else if (type != null) {
                transactions = transactionRepository.findByAccountIdAndType(
                        account.getId(),
                        Transaction.TransactionType.valueOf(type));
            } else {
                Page<Transaction> page = transactionRepository.findByAccountId(
                        account.getId(), PageRequest.of(0, Integer.MAX_VALUE));
                transactions = page.getContent();
            }

            StringBuilder csv = new StringBuilder();
            csv.append("ID,Type,Amount,Counterparty,Description,Status,CreatedAt\n");
            for (Transaction tx : transactions) {
                csv.append(String.format("%d,%s,%.2f,%s,%s,%s,%s\n",
                        tx.getId(), tx.getType(), tx.getAmount(),
                        tx.getCounterparty() != null ? tx.getCounterparty() : "",
                        tx.getDescription() != null ? tx.getDescription() : "",
                        tx.getStatus(), tx.getCreatedAt()));
            }

            ByteArrayResource resource = new ByteArrayResource(csv.toString().getBytes());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=transactions.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ByteArrayResource(("Error: " + e.getMessage()).getBytes()));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String type) {
        try {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!user.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Unauthorized access"));
            }
            Account account = accountRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            Page<Transaction> transactionPage;
            if (startDate != null && endDate != null && type != null) {
                transactionPage = transactionRepository.findByAccountIdAndCreatedAtBetweenAndType(
                        account.getId(),
                        LocalDateTime.parse(startDate),
                        LocalDateTime.parse(endDate),
                        Transaction.TransactionType.valueOf(type),
                        PageRequest.of(page, size, Sort.by("createdAt").descending()));
            } else if (startDate != null && endDate != null) {
                transactionPage = transactionRepository.findByAccountIdAndCreatedAtBetween(
                        account.getId(),
                        LocalDateTime.parse(startDate),
                        LocalDateTime.parse(endDate),
                        PageRequest.of(page, size, Sort.by("createdAt").descending()));
            } else if (type != null) {
                transactionPage = transactionRepository.findByAccountIdAndType(
                        account.getId(),
                        Transaction.TransactionType.valueOf(type),
                        PageRequest.of(page, size, Sort.by("createdAt").descending()));
            } else {
                transactionPage = transactionRepository.findByAccountId(
                        account.getId(),
                        PageRequest.of(page, size, Sort.by("createdAt").descending()));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("content", transactionPage.getContent());
            response.put("currentPage", transactionPage.getNumber());
            response.put("totalPages", transactionPage.getTotalPages());
            response.put("totalElements", transactionPage.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}