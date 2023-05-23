package io.github.fossure.web.rest;

import io.github.fossure.IntegrationTest;
import io.github.fossure.domain.Dependency;
import io.github.fossure.domain.Library;
import io.github.fossure.domain.Project;
import io.github.fossure.repository.DependencyRepository;
import io.github.fossure.web.rest.v1.DependencyResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link DependencyResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DependencyResourceIT {

    private static final LocalDate DEFAULT_ADDED_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_ADDED_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_ADDED_DATE = LocalDate.ofEpochDay(-1L);

    private static final Boolean DEFAULT_ADDED_MANUALLY = false;
    private static final Boolean UPDATED_ADDED_MANUALLY = true;

    private static final Boolean DEFAULT_HIDE_FOR_PUBLISHING = false;
    private static final Boolean UPDATED_HIDE_FOR_PUBLISHING = true;

    private static final String ENTITY_API_URL = "/api/dependencies";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private DependencyRepository dependencyRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDependencyMockMvc;

    private Dependency dependency;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Dependency createEntity(EntityManager em) {
        Dependency dependency = new Dependency()
            .addedDate(DEFAULT_ADDED_DATE)
            .addedManually(DEFAULT_ADDED_MANUALLY)
            .hideForPublishing(DEFAULT_HIDE_FOR_PUBLISHING);
        return dependency;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Dependency createUpdatedEntity(EntityManager em) {
        Dependency dependency = new Dependency()
            .addedDate(UPDATED_ADDED_DATE)
            .addedManually(UPDATED_ADDED_MANUALLY)
            .hideForPublishing(UPDATED_HIDE_FOR_PUBLISHING);
        return dependency;
    }

    @BeforeEach
    public void initTest() {
        dependency = createEntity(em);
    }

    @Test
    @Transactional
    void createDependency() throws Exception {
        int databaseSizeBeforeCreate = dependencyRepository.findAll().size();
        // Create the Dependency
        restDependencyMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(dependency))
            )
            .andExpect(status().isCreated());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeCreate + 1);
        Dependency testDependency = dependencyList.get(dependencyList.size() - 1);
        assertThat(testDependency.getAddedDate()).isEqualTo(DEFAULT_ADDED_DATE);
        assertThat(testDependency.getAddedManually()).isEqualTo(DEFAULT_ADDED_MANUALLY);
        assertThat(testDependency.getHideForPublishing()).isEqualTo(DEFAULT_HIDE_FOR_PUBLISHING);
    }

    @Test
    @Transactional
    void createDependencyWithExistingId() throws Exception {
        // Create the Dependency with an existing ID
        dependency.setId(1L);

        int databaseSizeBeforeCreate = dependencyRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDependencyMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(dependency))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllDependencies() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList
        restDependencyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(dependency.getId().intValue())))
            .andExpect(jsonPath("$.[*].addedDate").value(hasItem(DEFAULT_ADDED_DATE.toString())))
            .andExpect(jsonPath("$.[*].addedManually").value(hasItem(DEFAULT_ADDED_MANUALLY.booleanValue())))
            .andExpect(jsonPath("$.[*].hideForPublishing").value(hasItem(DEFAULT_HIDE_FOR_PUBLISHING.booleanValue())));
    }

    @Test
    @Transactional
    void getDependency() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get the dependency
        restDependencyMockMvc
            .perform(get(ENTITY_API_URL_ID, dependency.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(dependency.getId().intValue()))
            .andExpect(jsonPath("$.addedDate").value(DEFAULT_ADDED_DATE.toString()))
            .andExpect(jsonPath("$.addedManually").value(DEFAULT_ADDED_MANUALLY.booleanValue()))
            .andExpect(jsonPath("$.hideForPublishing").value(DEFAULT_HIDE_FOR_PUBLISHING.booleanValue()));
    }

    @Test
    @Transactional
    void getDependenciesByIdFiltering() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        Long id = dependency.getId();

        defaultDependencyShouldBeFound("id.equals=" + id);
        defaultDependencyShouldNotBeFound("id.notEquals=" + id);

        defaultDependencyShouldBeFound("id.greaterThanOrEqual=" + id);
        defaultDependencyShouldNotBeFound("id.greaterThan=" + id);

        defaultDependencyShouldBeFound("id.lessThanOrEqual=" + id);
        defaultDependencyShouldNotBeFound("id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedDate equals to DEFAULT_ADDED_DATE
        defaultDependencyShouldBeFound("addedDate.equals=" + DEFAULT_ADDED_DATE);

        // Get all the dependencyList where addedDate equals to UPDATED_ADDED_DATE
        defaultDependencyShouldNotBeFound("addedDate.equals=" + UPDATED_ADDED_DATE);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedDateIsNotEqualToSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedDate not equals to DEFAULT_ADDED_DATE
        defaultDependencyShouldNotBeFound("addedDate.notEquals=" + DEFAULT_ADDED_DATE);

        // Get all the dependencyList where addedDate not equals to UPDATED_ADDED_DATE
        defaultDependencyShouldBeFound("addedDate.notEquals=" + UPDATED_ADDED_DATE);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedDateIsInShouldWork() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedDate in DEFAULT_ADDED_DATE or UPDATED_ADDED_DATE
        defaultDependencyShouldBeFound("addedDate.in=" + DEFAULT_ADDED_DATE + "," + UPDATED_ADDED_DATE);

        // Get all the dependencyList where addedDate equals to UPDATED_ADDED_DATE
        defaultDependencyShouldNotBeFound("addedDate.in=" + UPDATED_ADDED_DATE);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedDate is not null
        defaultDependencyShouldBeFound("addedDate.specified=true");

        // Get all the dependencyList where addedDate is null
        defaultDependencyShouldNotBeFound("addedDate.specified=false");
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedDate is greater than or equal to DEFAULT_ADDED_DATE
        defaultDependencyShouldBeFound("addedDate.greaterThanOrEqual=" + DEFAULT_ADDED_DATE);

        // Get all the dependencyList where addedDate is greater than or equal to UPDATED_ADDED_DATE
        defaultDependencyShouldNotBeFound("addedDate.greaterThanOrEqual=" + UPDATED_ADDED_DATE);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedDate is less than or equal to DEFAULT_ADDED_DATE
        defaultDependencyShouldBeFound("addedDate.lessThanOrEqual=" + DEFAULT_ADDED_DATE);

        // Get all the dependencyList where addedDate is less than or equal to SMALLER_ADDED_DATE
        defaultDependencyShouldNotBeFound("addedDate.lessThanOrEqual=" + SMALLER_ADDED_DATE);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedDateIsLessThanSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedDate is less than DEFAULT_ADDED_DATE
        defaultDependencyShouldNotBeFound("addedDate.lessThan=" + DEFAULT_ADDED_DATE);

        // Get all the dependencyList where addedDate is less than UPDATED_ADDED_DATE
        defaultDependencyShouldBeFound("addedDate.lessThan=" + UPDATED_ADDED_DATE);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedDate is greater than DEFAULT_ADDED_DATE
        defaultDependencyShouldNotBeFound("addedDate.greaterThan=" + DEFAULT_ADDED_DATE);

        // Get all the dependencyList where addedDate is greater than SMALLER_ADDED_DATE
        defaultDependencyShouldBeFound("addedDate.greaterThan=" + SMALLER_ADDED_DATE);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedManuallyIsEqualToSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedManually equals to DEFAULT_ADDED_MANUALLY
        defaultDependencyShouldBeFound("addedManually.equals=" + DEFAULT_ADDED_MANUALLY);

        // Get all the dependencyList where addedManually equals to UPDATED_ADDED_MANUALLY
        defaultDependencyShouldNotBeFound("addedManually.equals=" + UPDATED_ADDED_MANUALLY);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedManuallyIsNotEqualToSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedManually not equals to DEFAULT_ADDED_MANUALLY
        defaultDependencyShouldNotBeFound("addedManually.notEquals=" + DEFAULT_ADDED_MANUALLY);

        // Get all the dependencyList where addedManually not equals to UPDATED_ADDED_MANUALLY
        defaultDependencyShouldBeFound("addedManually.notEquals=" + UPDATED_ADDED_MANUALLY);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedManuallyIsInShouldWork() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedManually in DEFAULT_ADDED_MANUALLY or UPDATED_ADDED_MANUALLY
        defaultDependencyShouldBeFound("addedManually.in=" + DEFAULT_ADDED_MANUALLY + "," + UPDATED_ADDED_MANUALLY);

        // Get all the dependencyList where addedManually equals to UPDATED_ADDED_MANUALLY
        defaultDependencyShouldNotBeFound("addedManually.in=" + UPDATED_ADDED_MANUALLY);
    }

    @Test
    @Transactional
    void getAllDependenciesByAddedManuallyIsNullOrNotNull() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where addedManually is not null
        defaultDependencyShouldBeFound("addedManually.specified=true");

        // Get all the dependencyList where addedManually is null
        defaultDependencyShouldNotBeFound("addedManually.specified=false");
    }

    @Test
    @Transactional
    void getAllDependenciesByHideForPublishingIsEqualToSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where hideForPublishing equals to DEFAULT_HIDE_FOR_PUBLISHING
        defaultDependencyShouldBeFound("hideForPublishing.equals=" + DEFAULT_HIDE_FOR_PUBLISHING);

        // Get all the dependencyList where hideForPublishing equals to UPDATED_HIDE_FOR_PUBLISHING
        defaultDependencyShouldNotBeFound("hideForPublishing.equals=" + UPDATED_HIDE_FOR_PUBLISHING);
    }

    @Test
    @Transactional
    void getAllDependenciesByHideForPublishingIsNotEqualToSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where hideForPublishing not equals to DEFAULT_HIDE_FOR_PUBLISHING
        defaultDependencyShouldNotBeFound("hideForPublishing.notEquals=" + DEFAULT_HIDE_FOR_PUBLISHING);

        // Get all the dependencyList where hideForPublishing not equals to UPDATED_HIDE_FOR_PUBLISHING
        defaultDependencyShouldBeFound("hideForPublishing.notEquals=" + UPDATED_HIDE_FOR_PUBLISHING);
    }

    @Test
    @Transactional
    void getAllDependenciesByHideForPublishingIsInShouldWork() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where hideForPublishing in DEFAULT_HIDE_FOR_PUBLISHING or UPDATED_HIDE_FOR_PUBLISHING
        defaultDependencyShouldBeFound("hideForPublishing.in=" + DEFAULT_HIDE_FOR_PUBLISHING + "," + UPDATED_HIDE_FOR_PUBLISHING);

        // Get all the dependencyList where hideForPublishing equals to UPDATED_HIDE_FOR_PUBLISHING
        defaultDependencyShouldNotBeFound("hideForPublishing.in=" + UPDATED_HIDE_FOR_PUBLISHING);
    }

    @Test
    @Transactional
    void getAllDependenciesByHideForPublishingIsNullOrNotNull() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        // Get all the dependencyList where hideForPublishing is not null
        defaultDependencyShouldBeFound("hideForPublishing.specified=true");

        // Get all the dependencyList where hideForPublishing is null
        defaultDependencyShouldNotBeFound("hideForPublishing.specified=false");
    }

    @Test
    @Transactional
    void getAllDependenciesByLibraryIsEqualToSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);
        Library library;
        if (TestUtil.findAll(em, Library.class).isEmpty()) {
            library = LibraryResourceIT.createEntity(em);
            em.persist(library);
            em.flush();
        } else {
            library = TestUtil.findAll(em, Library.class).get(0);
        }
        em.persist(library);
        em.flush();
        dependency.setLibrary(library);
        dependencyRepository.saveAndFlush(dependency);
        Long libraryId = library.getId();

        // Get all the dependencyList where library equals to libraryId
        defaultDependencyShouldBeFound("libraryId.equals=" + libraryId);

        // Get all the dependencyList where library equals to (libraryId + 1)
        defaultDependencyShouldNotBeFound("libraryId.equals=" + (libraryId + 1));
    }

    @Test
    @Transactional
    void getAllDependenciesByProjectIsEqualToSomething() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);
        Project project;
        if (TestUtil.findAll(em, Project.class).isEmpty()) {
            project = ProjectResourceIT.createEntity(em);
            em.persist(project);
            em.flush();
        } else {
            project = TestUtil.findAll(em, Project.class).get(0);
        }
        em.persist(project);
        em.flush();
        dependency.setProject(project);
        dependencyRepository.saveAndFlush(dependency);
        Long projectId = project.getId();

        // Get all the dependencyList where project equals to projectId
        defaultDependencyShouldBeFound("projectId.equals=" + projectId);

        // Get all the dependencyList where project equals to (projectId + 1)
        defaultDependencyShouldNotBeFound("projectId.equals=" + (projectId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDependencyShouldBeFound(String filter) throws Exception {
        restDependencyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(dependency.getId().intValue())))
            .andExpect(jsonPath("$.[*].addedDate").value(hasItem(DEFAULT_ADDED_DATE.toString())))
            .andExpect(jsonPath("$.[*].addedManually").value(hasItem(DEFAULT_ADDED_MANUALLY.booleanValue())))
            .andExpect(jsonPath("$.[*].hideForPublishing").value(hasItem(DEFAULT_HIDE_FOR_PUBLISHING.booleanValue())));

        // Check, that the count call also returns 1
        restDependencyMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDependencyShouldNotBeFound(String filter) throws Exception {
        restDependencyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDependencyMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDependency() throws Exception {
        // Get the dependency
        restDependencyMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewDependency() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        int databaseSizeBeforeUpdate = dependencyRepository.findAll().size();

        // Update the dependency
        Dependency updatedDependency = dependencyRepository.findById(dependency.getId()).get();
        // Disconnect from session so that the updates on updatedDependency are not directly saved in db
        em.detach(updatedDependency);
        updatedDependency
            .addedDate(UPDATED_ADDED_DATE)
            .addedManually(UPDATED_ADDED_MANUALLY)
            .hideForPublishing(UPDATED_HIDE_FOR_PUBLISHING);

        restDependencyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedDependency.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(updatedDependency))
            )
            .andExpect(status().isOk());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeUpdate);
        Dependency testDependency = dependencyList.get(dependencyList.size() - 1);
        assertThat(testDependency.getAddedDate()).isEqualTo(UPDATED_ADDED_DATE);
        assertThat(testDependency.getAddedManually()).isEqualTo(UPDATED_ADDED_MANUALLY);
        assertThat(testDependency.getHideForPublishing()).isEqualTo(UPDATED_HIDE_FOR_PUBLISHING);
    }

    @Test
    @Transactional
    void putNonExistingDependency() throws Exception {
        int databaseSizeBeforeUpdate = dependencyRepository.findAll().size();
        dependency.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDependencyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, dependency.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(dependency))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDependency() throws Exception {
        int databaseSizeBeforeUpdate = dependencyRepository.findAll().size();
        dependency.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDependencyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(dependency))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDependency() throws Exception {
        int databaseSizeBeforeUpdate = dependencyRepository.findAll().size();
        dependency.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDependencyMockMvc
            .perform(
                put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(dependency))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDependencyWithPatch() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        int databaseSizeBeforeUpdate = dependencyRepository.findAll().size();

        // Update the dependency using partial update
        Dependency partialUpdatedDependency = new Dependency();
        partialUpdatedDependency.setId(dependency.getId());

        partialUpdatedDependency.addedDate(UPDATED_ADDED_DATE);

        restDependencyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDependency.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedDependency))
            )
            .andExpect(status().isOk());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeUpdate);
        Dependency testDependency = dependencyList.get(dependencyList.size() - 1);
        assertThat(testDependency.getAddedDate()).isEqualTo(UPDATED_ADDED_DATE);
        assertThat(testDependency.getAddedManually()).isEqualTo(DEFAULT_ADDED_MANUALLY);
        assertThat(testDependency.getHideForPublishing()).isEqualTo(DEFAULT_HIDE_FOR_PUBLISHING);
    }

    @Test
    @Transactional
    void fullUpdateDependencyWithPatch() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        int databaseSizeBeforeUpdate = dependencyRepository.findAll().size();

        // Update the dependency using partial update
        Dependency partialUpdatedDependency = new Dependency();
        partialUpdatedDependency.setId(dependency.getId());

        partialUpdatedDependency
            .addedDate(UPDATED_ADDED_DATE)
            .addedManually(UPDATED_ADDED_MANUALLY)
            .hideForPublishing(UPDATED_HIDE_FOR_PUBLISHING);

        restDependencyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDependency.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedDependency))
            )
            .andExpect(status().isOk());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeUpdate);
        Dependency testDependency = dependencyList.get(dependencyList.size() - 1);
        assertThat(testDependency.getAddedDate()).isEqualTo(UPDATED_ADDED_DATE);
        assertThat(testDependency.getAddedManually()).isEqualTo(UPDATED_ADDED_MANUALLY);
        assertThat(testDependency.getHideForPublishing()).isEqualTo(UPDATED_HIDE_FOR_PUBLISHING);
    }

    @Test
    @Transactional
    void patchNonExistingDependency() throws Exception {
        int databaseSizeBeforeUpdate = dependencyRepository.findAll().size();
        dependency.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDependencyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, dependency.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(dependency))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDependency() throws Exception {
        int databaseSizeBeforeUpdate = dependencyRepository.findAll().size();
        dependency.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDependencyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(dependency))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDependency() throws Exception {
        int databaseSizeBeforeUpdate = dependencyRepository.findAll().size();
        dependency.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDependencyMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(dependency))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Dependency in the database
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDependency() throws Exception {
        // Initialize the database
        dependencyRepository.saveAndFlush(dependency);

        int databaseSizeBeforeDelete = dependencyRepository.findAll().size();

        // Delete the dependency
        restDependencyMockMvc
            .perform(delete(ENTITY_API_URL_ID, dependency.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Dependency> dependencyList = dependencyRepository.findAll();
        assertThat(dependencyList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
