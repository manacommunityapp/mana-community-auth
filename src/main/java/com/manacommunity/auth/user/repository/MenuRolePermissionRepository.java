package com.manacommunity.auth.user.repository;

import com.manacommunity.auth.user.model.MenuRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRolePermissionRepository extends JpaRepository<MenuRolePermission, Long> {

    /** All permissions for a given role (one row per menu item). */
    List<MenuRolePermission> findByRoleId(Long roleId);

    /** All permissions for a given menu item across all roles. */
    List<MenuRolePermission> findByMenuItemId(Long menuId);

    /** Single permission row for a specific role + menu item combination. */
    Optional<MenuRolePermission> findByRoleIdAndMenuItemId(Long roleId, Long menuId);

    /** Check if a role has a specific permission on a menu item. */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM MenuRolePermission p
        WHERE p.role.id = :roleId
          AND p.menuItem.id = :menuId
          AND p.canView = true
    """)
    boolean canView(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM MenuRolePermission p
        WHERE p.role.id = :roleId
          AND p.menuItem.id = :menuId
          AND p.canAdd = true
    """)
    boolean canAdd(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM MenuRolePermission p
        WHERE p.role.id = :roleId
          AND p.menuItem.id = :menuId
          AND p.canUpdate = true
    """)
    boolean canUpdate(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM MenuRolePermission p
        WHERE p.role.id = :roleId
          AND p.menuItem.id = :menuId
          AND p.canDelete = true
    """)
    boolean canDelete(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    /** All menu items a role can view (for building the sidebar). */
    @Query("""
        SELECT p FROM MenuRolePermission p
        JOIN FETCH p.menuItem m
        WHERE p.role.id = :roleId
          AND p.canView = true
          AND m.isActive = true
        ORDER BY m.sortOrder ASC
    """)
    List<MenuRolePermission> findViewableByRoleId(@Param("roleId") Long roleId);

    /** Delete all permissions for a role (used when bulk-replacing). */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MenuRolePermission p WHERE p.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    /** Delete a single role+menu combination. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MenuRolePermission p WHERE p.role.id = :roleId AND p.menuItem.id = :menuId")
    void deleteByRoleIdAndMenuItemId(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
}
