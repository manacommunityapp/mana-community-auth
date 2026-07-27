package com.manacommunity.auth.repository;

import com.manacommunity.common.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    Optional<Role> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);
    Optional<Role> findByNameIgnoreCaseAndCommunityIdIsNull(String name);
    boolean existsByNameIgnoreCaseAndCommunityId(String name, Long communityId);
    boolean existsByNameIgnoreCaseAndCommunityIdIsNull(String name);
}
