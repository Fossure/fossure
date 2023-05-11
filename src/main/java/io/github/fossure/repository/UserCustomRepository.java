package io.github.fossure.repository;

import io.github.fossure.domain.User;
import org.springframework.stereotype.Repository;

/**
 * Custom Spring Data JPA repository for the {@link User} entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserCustomRepository extends UserRepository {}
