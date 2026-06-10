package com.xanwar.rps.repository;

import com.xanwar.rps.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTornId(String tornId);
    boolean existsByTornId(String tornId);
    List<User> findTop10ByOrderByTotalMatchesWonDesc();
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE (u.totalMoolaWon - u.totalMoolaLost) > 0 ORDER BY (u.totalMoolaWon - u.totalMoolaLost) DESC")
    List<User> findTopProfitableUsers(org.springframework.data.domain.Pageable pageable);

    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByRememberToken(String rememberToken);
}
