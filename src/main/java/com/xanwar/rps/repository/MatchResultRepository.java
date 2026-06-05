package com.xanwar.rps.repository;

import com.xanwar.rps.model.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    List<MatchResult> findByPlayer1IdOrPlayer2IdOrderByCreatedAtDesc(String player1Id, String player2Id);
}
