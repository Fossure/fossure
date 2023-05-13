package io.github.fossure.repository;

import io.github.fossure.domain.LicensePerLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

/**
 * Spring Data SQL repository for the LicensePerLibrary entity.
 */
@Repository
public interface LicensePerLibraryRepository extends JpaRepository<LicensePerLibrary, Long>, JpaSpecificationExecutor<LicensePerLibrary> {
    @Transactional
    @Modifying
    @Query("delete from LicensePerLibrary lpl where lpl.library.id = :libraryId")
    void deleteByLibraryId(@Param("libraryId") Long libraryId);
}
