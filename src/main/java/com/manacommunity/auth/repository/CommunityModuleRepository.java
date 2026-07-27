package com.manacommunity.auth.repository;

import com.manacommunity.common.model.CommunityModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityModuleRepository extends JpaRepository<CommunityModule, Long> {

    List<CommunityModule> findByCommunityIdOrderBySortOrderAsc(Long communityId);

    @Query("""
        SELECT cm FROM CommunityModule cm
        WHERE cm.community.id = :communityId AND cm.isEnabled = true
        ORDER BY cm.sortOrder ASC
    """)
    List<CommunityModule> findEnabledByCommunityId(@Param("communityId") Long communityId);

    Optional<CommunityModule> findByCommunityIdAndModuleKey(Long communityId, String moduleKey);

    boolean existsByCommunityId(Long communityId);

    @Query("""
        SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END
        FROM CommunityModule cm
        WHERE cm.community.id = :communityId
          AND cm.moduleKey = :moduleKey
          AND cm.isEnabled = true
    """)
    boolean isModuleEnabled(@Param("communityId") Long communityId, @Param("moduleKey") String moduleKey);
}
