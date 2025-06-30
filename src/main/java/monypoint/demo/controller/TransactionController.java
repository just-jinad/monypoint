package monypoint.demo.controller;

import monypoint.demo.entity.Account;
import monypoint.demo.entity.Transaction;
import monypoint.demo.repository.AccountRepository;
import monypoint.demo.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private monypoint.demo.repository.TransactionRepository transactionRepository;

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> createTransfer(
            @RequestParam Long userId,
            @RequestParam String recipientAccountNumber,
            @RequestParam Double amount,
            @RequestParam(required = false) String description,
            @RequestParam String pin) {
        Transaction transaction = transactionService.createTransfer(
                userId, recipientAccountNumber, amount, description, pin);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/top-up")
    public ResponseEntity<Transaction> createTopUp(
            @RequestParam Long userId,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @RequestParam String cardDetails,
            @RequestParam String pin) {
        Transaction transaction = transactionService.createTopUp(
                userId, amount, paymentMethod, cardDetails, pin);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/bills")
    public ResponseEntity<Transaction> createBillPayment(
            @RequestParam Long userId,
            @RequestParam String biller,
            @RequestParam String customerId,
            @RequestParam Double amount,
            @RequestParam String pin) {
        Transaction transaction = transactionService.createBillPayment(
                userId, biller, customerId, amount, pin);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/qr")
    public ResponseEntity<Transaction> createQRPayment(
            @RequestParam Long userId,
            @RequestParam String qrCode,
            @RequestParam Double amount,
            @RequestParam String pin) {
        Transaction transaction = transactionService.createQRPayment(
                userId, qrCode, amount, pin);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportTransactions(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String type) {
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
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String type) {
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
    }
}