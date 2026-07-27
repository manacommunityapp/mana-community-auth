package com.manacommunity.auth.user.repository;

import com.manacommunity.auth.user.model.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    List<UserSession> findByUserIdAndStatus(Long userId, String status);

    Page<UserSession> findAllByOrderByLoginAtDesc(Pageable pageable);

    long countByStatus(String status);

    long countByLoginAtAfter(LocalDateTime since);
}
