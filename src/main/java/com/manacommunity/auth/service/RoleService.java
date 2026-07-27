package com.manacommunity.auth.service;

import com.manacommunity.common.model.Role;
import java.util.List;

public interface RoleService {
    
    /** Returns a list of all created roles from the database. */
    List<Role> getAllRoles();
    
    /** Creates a new custom role in the database. */
    Role createRole(String name);
    Role createRole(String name, Long communityId);
    
    /** Resolves a role by name, creating it if it doesn't already exist. */
    Role findOrCreateRole(String name);
    Role findOrCreateRole(String name, Long communityId);
}
