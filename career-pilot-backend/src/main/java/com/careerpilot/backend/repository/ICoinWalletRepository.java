package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.CoinWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ICoinWalletRepository extends JpaRepository<CoinWallet, Long> {
    Optional<CoinWallet> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from CoinWallet w where w.user.id = :userId")
    Optional<CoinWallet> findByUserIdForUpdate(Long userId);
}