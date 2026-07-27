package com.manacommunity.auth.support;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for all @DataJpaTest repository tests.
 *
 * Uses H2 (PostgreSQL-mode) configured in application-test.yaml.
 * Replace.NONE tells Spring to use our configured H2 instead of
 * auto-replacing with its own embedded DB.
 *
 * Schema is created by src/test/resources/sql/init-schema.sql
 * before Hibernate runs DDL.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseRepositoryTest {
}
