package com.dmh.accountservice.repository;

import com.dmh.accountservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(Long userId);

    boolean existsByCvu(String cvu);

    boolean existsByAlias(String alias);

    boolean existsByUserId(Long userId);
}
