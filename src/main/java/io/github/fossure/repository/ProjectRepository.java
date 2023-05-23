package io.github.fossure.repository;

import io.github.fossure.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data SQL repository for the Project entity.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    @Query(
        "select project from Project project where lower(project.label) = lower(:label) and lower(project.version) = lower(:version)"
    )
    Optional<Project> findByIdentifierAndVersion(@Param("label") String label, @Param("version") String version);

    @Query("select project from Project project where project.label = :label and project.delivered = false")
    Optional<Project> findByIdAndDeliveredIsFalse(@Param("label") String label);

    @Query("select count(project) from Project project where project.delivered = false")
    long countAllByDeliveredIsFalse();
}
