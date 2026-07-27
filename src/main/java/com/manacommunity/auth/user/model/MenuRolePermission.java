package com.manacommunity.auth.user.model;

import com.manacommunity.common.model.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Granular CRUD permission matrix: which {@link Role} can view / add / update / delete
 * a given {@link MenuItem}. This is checked at both the API layer (controller guard)
 * and the frontend (menu visibility + button enable/disable).
 *
 * <p>Separate from the existing {@link RolePermission} table, which stores flat
 * string-based permission keys. This table is menu-item-scoped and boolean-flagged.</p>
 */
@Entity
@Table(
    name = "menu_role_permission",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_mrp_role_menu",
        columnNames = {"role_id", "menu_id"}
    ),
    indexes = {
        @Index(name = "idx_mrp_role", columnList = "role_id"),
        @Index(name = "idx_mrp_menu", columnList = "menu_id"),
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"role", "menuItem"})
public class MenuRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "can_view", nullable = false)
    @Builder.Default
    private Boolean canView = false;

    @Column(name = "can_add", nullable = false)
    @Builder.Default
    private Boolean canAdd = false;

    @Column(name = "can_update", nullable = false)
    @Builder.Default
    private Boolean canUpdate = false;

    @Column(name = "can_delete", nullable = false)
    @Builder.Default
    private Boolean canDelete = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
