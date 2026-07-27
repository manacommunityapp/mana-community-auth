package com.manacommunity.auth.repository;

import com.manacommunity.common.model.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {
    Optional<Community> findByInviteCode(String inviteCode);
    List<Community> findByTypeIgnoreCase(String type);

    // Active (not soft-deleted) only — used for the signup dropdown and admin list.
    List<Community> findByActiveTrueOrderByNameAsc();
    List<Community> findByActiveTrueAndTypeIgnoreCaseOrderByNameAsc(String type);
}
