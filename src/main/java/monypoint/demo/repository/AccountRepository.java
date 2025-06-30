package monypoint.demo.repository;

import monypoint.demo.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
     Optional<Account> findByUserId(Long userId);
     Optional<Account> findByAccountNumber(String accountNumber);
}