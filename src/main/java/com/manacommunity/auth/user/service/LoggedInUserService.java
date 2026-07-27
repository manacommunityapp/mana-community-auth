package com.manacommunity.auth.user.service;

import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.common.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Utility service to resolve the full AppUser entity from
 * a Spring Security UserPrincipal (JWT / session).
 *
 * Inject this into any controller or service that needs the
 * complete logged-in user object (community, role, flat, etc.).
 */
@Service
@RequiredArgsConstructor
public class LoggedInUserService {

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    private final AppUserRepository userRepository;

    /**
     * Resolves the full AppUser entity from the authenticated principal.
     *
     * @param principal the UserPrincipal injected by Spring Security
     * @return the AppUser entity with all fields loaded
     * @throws ResourceNotFoundException if the user ID no longer exists in the DB
     */
    public AppUser resolve(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId().toString()));
    }

    public ResolvedUser resolveContext(UserPrincipal principal) {
        return ResolvedUser.from(resolve(principal));
    }

    public record ResolvedUser(AppUser user, boolean superAdmin, Long communityId) {
        public static ResolvedUser from(AppUser user) {
            boolean sa = ROLE_SUPER_ADMIN.equals(user.getRole());
            Long cid = user.getCommunity() != null ? user.getCommunity().getId() : null;
            return new ResolvedUser(user, sa, cid);
        }

        public Long userId() {
            return user.getId();
        }

        public Long scopeCommunityId(Long requestedCommunityId) {
            return superAdmin ? requestedCommunityId : communityId;
        }
    }
}
