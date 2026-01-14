package com.dmh.accountservice.repository;

import com.dmh.accountservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(Long userId);

    boolean existsByCvu(String cvu);

    boolean existsByAlias(String alias);

    boolean existsByUserId(Long userId);

    /**
     * Busca una cuenta por CVU o alias
     */
    @Query("SELECT a FROM Account a WHERE a.cvu = :cvu OR a.alias = :alias")
    java.util.Optional<Account> findByCvuOrAlias(@Param("cvu") String cvu, @Param("alias") String alias);
}
