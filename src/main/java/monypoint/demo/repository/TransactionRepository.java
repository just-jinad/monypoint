package monypoint.demo.repository;

import monypoint.demo.entity.Transaction;
import monypoint.demo.entity.Transaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Paginated transactions for an account
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    // Paginated by date range
    Page<Transaction> findByAccountIdAndCreatedAtBetween(
            Long accountId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Paginated by type
    Page<Transaction> findByAccountIdAndType(Long accountId, TransactionType type, Pageable pageable);

    // Paginated by date range and type
    Page<Transaction> findByAccountIdAndCreatedAtBetweenAndType(
            Long accountId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Transaction.TransactionType type,
            Pageable pageable
    );

    // Non-paginated for export
    List<Transaction> findByAccountIdAndCreatedAtBetween(
            Long accountId, LocalDateTime start, LocalDateTime end);

    List<Transaction> findByAccountIdAndType(Long accountId, TransactionType type);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.createdAt BETWEEN :start AND :end AND t.type = :type")
    List<Transaction> findByAccountIdAndCreatedAtAndType(
            @Param("accountId") Long accountId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("type") TransactionType type);
}