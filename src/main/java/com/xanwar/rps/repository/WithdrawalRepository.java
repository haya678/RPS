package com.xanwar.rps.repository;

import com.xanwar.rps.model.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByStatus(String status);

    List<Withdrawal> findByStatusOrderByCreatedAtAsc(String status);

    List<Withdrawal> findByTornId(String tornId);
}
