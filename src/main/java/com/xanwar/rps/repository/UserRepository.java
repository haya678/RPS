package com.xanwar.rps.repository;

import com.xanwar.rps.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTornId(String tornId);
    boolean existsByTornId(String tornId);
    List<User> findTop10ByOrderByTotalMatchesWonDesc();
    Optional<User> findByUsernameIgnoreCase(String username);
}
