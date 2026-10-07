package com.manacommunity.auth.slice.repository;

import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.auth.support.BaseRepositoryTest;
import com.manacommunity.auth.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppUserRepository")
class AppUserRepositoryTest extends BaseRepositoryTest {

    @Autowired AppUserRepository userRepo;
    @Autowired TestEntityManager em;

    private AppUser savedUser;

    @BeforeEach
    void setUp() {
        // Community must exist before AppUser (FK)
        var community = em.persistAndFlush(TestDataBuilder.community());

        AppUser user = TestDataBuilder.adminUser();
        user.setId(null);             // let H2 generate
        user.setCommunity(community);
        savedUser = em.persistAndFlush(user);
        em.clear();
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("returns user for existing email")
        void found() {
            Optional<AppUser> result = userRepo.findByEmail(savedUser.getEmail());
            assertThat(result).isPresent();
            assertThat(result.get().getFullName()).isEqualTo(savedUser.getFullName());
        }

        @Test
        @DisplayName("returns empty for unknown email")
        void notFound() {
            assertThat(userRepo.findByEmail("nobody@test.com")).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("true for existing email")
        void exists() {
            assertThat(userRepo.existsByEmail(savedUser.getEmail())).isTrue();
        }

        @Test
        @DisplayName("false for new email")
        void notExists() {
            assertThat(userRepo.existsByEmail("new@test.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByPhone")
    class ExistsByPhone {

        @Test
        @DisplayName("true for existing phone")
        void exists() {
            assertThat(userRepo.existsByPhone(savedUser.getPhone())).isTrue();
        }

        @Test
        @DisplayName("false for new phone")
        void notExists() {
            assertThat(userRepo.existsByPhone("0000000000")).isFalse();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns user by id")
        void found() {
            assertThat(userRepo.findById(savedUser.getId())).isPresent();
        }

        @Test
        @DisplayName("returns empty for unknown id")
        void notFound() {
            assertThat(userRepo.findById(99999L)).isEmpty();
        }
    }

    @Test
    @DisplayName("save persists all required fields")
    void saveAllFields() {
        var community = em.find(com.manacommunity.common.model.Community.class,
                savedUser.getCommunity().getId());

        AppUser newUser = TestDataBuilder.memberUser();
        newUser.setId(null);
        newUser.setEmail("member_unique@test.com");
        newUser.setPhone("8000000001");
        newUser.setCommunity(community);

        AppUser saved = userRepo.save(newUser);
        em.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getIsActive()).isTrue();
    }
}
