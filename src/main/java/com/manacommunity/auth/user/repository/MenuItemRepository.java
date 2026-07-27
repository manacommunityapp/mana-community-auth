package com.manacommunity.auth.user.repository;

import com.manacommunity.auth.user.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    boolean existsByMenuKey(String menuKey);

    Optional<MenuItem> findByMenuKey(String menuKey);

    List<MenuItem> findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();

    @Query("""
        SELECT m FROM MenuItem m
        WHERE m.parent IS NULL
          AND m.isActive = true
          AND (m.community.id = :communityId OR m.community IS NULL)
        ORDER BY m.sortOrder ASC
    """)
    List<MenuItem> findActiveRootsByCommunity(@Param("communityId") Long communityId);
}
