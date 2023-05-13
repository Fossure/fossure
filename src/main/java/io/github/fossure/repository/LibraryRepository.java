package io.github.fossure.repository;

import io.github.fossure.domain.Library;
import io.github.fossure.domain.License;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data SQL repository for the Library entity.
 */
@Repository
public interface LibraryRepository extends
    LibraryRepositoryWithBagRelationships, JpaRepository<Library, Long>, JpaSpecificationExecutor<Library> {

    @Query("select library from Library library where library.lastReviewedBy.login = ?#{principal.username}")
    List<Library> findByLastReviewedByIsCurrentUser();

    default Optional<Library> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<Library> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<Library> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select distinct library from Library library left join fetch library.lastReviewedBy",
        countQuery = "select count(distinct library) from Library library"
    )
    Page<Library> findAllWithToOneRelationships(Pageable pageable);

    @Query("select distinct library from Library library left join fetch library.lastReviewedBy")
    List<Library> findAllWithToOneRelationships();

    @Query("select library from Library library left join fetch library.lastReviewedBy where library.id =:id")
    Optional<Library> findOneWithToOneRelationships(@Param("id") Long id);

    @Query(
        "select library from Library library where lower(library.groupId) = lower(:groupId) and lower(library.artifactId) = lower(:artifactId) and lower(library.version) = lower(:version)"
    )
    Optional<Library> findByGroupIdAndArtifactIdAndVersion(
        @Param("groupId") String groupId,
        @Param("artifactId") String artifactId,
        @Param("version") String version
    );

    @Query(
        "select distinct library from Library library where library.licenseUrl is null or library.licenseUrl = '' or library.sourceCodeUrl is null or library.sourceCodeUrl = ''"
    )
    List<Library> findAllWhereUrlIsEmpty();

    @Query("select distinct library from Library library where library.md5 = lower(:hash) or library.sha1 = lower(:hash)")
    List<Library> findByHash(@Param("hash") String hash);

    List<Library> findByCopyrightNull();

    @Query(
        "select distinct library from Library library left join fetch library.licenseOfFiles " +
        "where :license member of library.licenseToPublishes or :license member of library.licenseOfFiles"
    )
    List<Library> findAllByLicenseToPublishAndLicenseOfFiles(@Param("license") License license);

    @Query("select count(library) from Library library where :license member of library.licenseToPublishes")
    long countByLicenseToPublishContainsLicense(@Param("license") License license);

    @Query("select count(library) from Library library where :unknown member of library.licenseToPublishes or :nonLicensed member of library.licenseToPublishes")
    long countByUnidentifiedLicense(@Param("unknown") License unknown, @Param("nonLicensed") License nonLicensed);
}
