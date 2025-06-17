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

    // Filtered by date range
    List<Transaction> findByAccountIdAndCreatedAtBetween(
            Long accountId, LocalDateTime start, LocalDateTime end);

    // Filtered by type
    List<Transaction> findByAccountIdAndType(Long accountId, TransactionType type);

    // Filtered by date and type
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.createdAt BETWEEN :start AND :end AND t.type = :type")
    List<Transaction> findByAccountIdAndCreatedAtAndType(
            @Param("accountId") Long accountId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("type") TransactionType type);
}