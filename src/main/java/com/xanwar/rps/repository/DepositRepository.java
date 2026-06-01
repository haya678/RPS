package com.xanwar.rps.repository;

import com.xanwar.rps.model.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
    Optional<Deposit> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}
