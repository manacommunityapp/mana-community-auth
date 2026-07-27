package com.manacommunity.auth.service.impl;

import com.manacommunity.common.exception.DuplicateResourceException;
import com.manacommunity.common.exception.InvalidInputException;
import com.manacommunity.common.model.Role;
import com.manacommunity.auth.repository.RoleRepository;
import com.manacommunity.auth.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepo;

    @Override
    public List<Role> getAllRoles() {
        return roleRepo.findAll();
    }

    @Override
    @Transactional
    public Role createRole(String name) {
        return createRole(name, null);
    }

    @Override
    @Transactional
    public Role createRole(String name, Long communityId) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Role name is required");
        }
        
        String normalizedName = name.trim().toUpperCase();
        boolean exists = (communityId != null)
                ? roleRepo.existsByNameIgnoreCaseAndCommunityId(normalizedName, communityId)
                : roleRepo.existsByNameIgnoreCaseAndCommunityIdIsNull(normalizedName);
                
        if (exists) {
            throw new DuplicateResourceException("Role", "name", name);
        }
        
        Role newRole = Role.builder()
                .name(normalizedName)
                .communityId(communityId)
                .permissions(new java.util.HashSet<>())
                .build();
                
        return roleRepo.save(newRole);
    }

    @Override
    @Transactional
    public Role findOrCreateRole(String name) {
        return findOrCreateRole(name, null);
    }

    @Override
    @Transactional
    public Role findOrCreateRole(String name, Long communityId) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Role name is required");
        }
        
        String normalizedName = name.trim().toUpperCase();
        if (communityId != null) {
            return roleRepo.findByNameIgnoreCaseAndCommunityId(normalizedName, communityId)
                    .orElseGet(() -> roleRepo.save(Role.builder()
                            .name(normalizedName)
                            .communityId(communityId)
                            .permissions(new java.util.HashSet<>())
                            .build()));
        } else {
            return roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(normalizedName)
                    .orElseGet(() -> roleRepo.save(Role.builder()
                            .name(normalizedName)
                            .permissions(new java.util.HashSet<>())
                            .build()));
        }
    }
}
