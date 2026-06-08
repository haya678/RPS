package com.xanwar.rps.repository;

import com.xanwar.rps.model.PendingDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PendingDepositRepository extends JpaRepository<PendingDeposit, Long> {
    Optional<PendingDeposit> findByTornId(String tornId);
    void deleteByTornId(String tornId);
    boolean existsByTornId(String tornId);
}
