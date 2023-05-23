package io.github.fossure.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import io.github.fossure.IntegrationTest;
import io.github.fossure.domain.Dependency;
import io.github.fossure.domain.Project;
import io.github.fossure.domain.enumeration.UploadState;
import io.github.fossure.repository.ProjectRepository;
import io.github.fossure.web.rest.v1.ProjectResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ProjectResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ProjectResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_IDENTIFIER = "AAAAAAAAAA";
    private static final String UPDATED_IDENTIFIER = "BBBBBBBBBB";

    private static final String DEFAULT_VERSION = "AAAAAAAAAA";
    private static final String UPDATED_VERSION = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_CREATED_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_CREATED_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_CREATED_DATE = LocalDate.ofEpochDay(-1L);

    private static final LocalDate DEFAULT_LAST_UPDATED_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_LAST_UPDATED_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_LAST_UPDATED_DATE = LocalDate.ofEpochDay(-1L);

    private static final UploadState DEFAULT_UPLOAD_STATE = UploadState.OK;
    private static final UploadState UPDATED_UPLOAD_STATE = UploadState.PROCESSING;

    private static final String DEFAULT_DISCLAIMER = "AAAAAAAAAA";
    private static final String UPDATED_DISCLAIMER = "BBBBBBBBBB";

    private static final Boolean DEFAULT_DELIVERED = false;
    private static final Boolean UPDATED_DELIVERED = true;

    private static final Instant DEFAULT_DELIVERED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DELIVERED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_CONTACT = "AAAAAAAAAA";
    private static final String UPDATED_CONTACT = "BBBBBBBBBB";

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

    private static final String DEFAULT_UPLOAD_FILTER = "AAAAAAAAAA";
    private static final String UPDATED_UPLOAD_FILTER = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/projects";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProjectMockMvc;

    private Project project;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Project createEntity(EntityManager em) {
        Project project = new Project()
            .name(DEFAULT_NAME)
            .label(DEFAULT_IDENTIFIER)
            .version(DEFAULT_VERSION)
            .createdDate(DEFAULT_CREATED_DATE)
            .lastUpdatedDate(DEFAULT_LAST_UPDATED_DATE)
            .uploadState(DEFAULT_UPLOAD_STATE)
            .disclaimer(DEFAULT_DISCLAIMER)
            .delivered(DEFAULT_DELIVERED)
            .deliveredDate(DEFAULT_DELIVERED_DATE)
            .contact(DEFAULT_CONTACT)
            .comment(DEFAULT_COMMENT)
            .uploadFilter(DEFAULT_UPLOAD_FILTER);
        return project;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Project createUpdatedEntity(EntityManager em) {
        Project project = new Project()
            .name(UPDATED_NAME)
            .label(UPDATED_IDENTIFIER)
            .version(UPDATED_VERSION)
            .createdDate(UPDATED_CREATED_DATE)
            .lastUpdatedDate(UPDATED_LAST_UPDATED_DATE)
            .uploadState(UPDATED_UPLOAD_STATE)
            .disclaimer(UPDATED_DISCLAIMER)
            .delivered(UPDATED_DELIVERED)
            .deliveredDate(UPDATED_DELIVERED_DATE)
            .contact(UPDATED_CONTACT)
            .comment(UPDATED_COMMENT)
            .uploadFilter(UPDATED_UPLOAD_FILTER);
        return project;
    }

    @BeforeEach
    public void initTest() {
        project = createEntity(em);
    }

    @Test
    @Transactional
    void createProject() throws Exception {
        int databaseSizeBeforeCreate = projectRepository.findAll().size();
        // Create the Project
        restProjectMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(project)))
            .andExpect(status().isCreated());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeCreate + 1);
        Project testProject = projectList.get(projectList.size() - 1);
        assertThat(testProject.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testProject.getLabel()).isEqualTo(DEFAULT_IDENTIFIER);
        assertThat(testProject.getVersion()).isEqualTo(DEFAULT_VERSION);
        assertThat(testProject.getCreatedDate()).isEqualTo(DEFAULT_CREATED_DATE);
        assertThat(testProject.getLastUpdatedDate()).isEqualTo(DEFAULT_LAST_UPDATED_DATE);
        assertThat(testProject.getUploadState()).isEqualTo(DEFAULT_UPLOAD_STATE);
        assertThat(testProject.getDisclaimer()).isEqualTo(DEFAULT_DISCLAIMER);
        assertThat(testProject.getDelivered()).isEqualTo(DEFAULT_DELIVERED);
        assertThat(testProject.getDeliveredDate()).isEqualTo(DEFAULT_DELIVERED_DATE);
        assertThat(testProject.getContact()).isEqualTo(DEFAULT_CONTACT);
        assertThat(testProject.getComment()).isEqualTo(DEFAULT_COMMENT);
        assertThat(testProject.getUploadFilter()).isEqualTo(DEFAULT_UPLOAD_FILTER);
    }

    @Test
    @Transactional
    void createProjectWithExistingId() throws Exception {
        // Create the Project with an existing ID
        project.setId(1L);

        int databaseSizeBeforeCreate = projectRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restProjectMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(project)))
            .andExpect(status().isBadRequest());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = projectRepository.findAll().size();
        // set the field null
        project.setName(null);

        // Create the Project, which fails.

        restProjectMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(project)))
            .andExpect(status().isBadRequest());

        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkIdentifierIsRequired() throws Exception {
        int databaseSizeBeforeTest = projectRepository.findAll().size();
        // set the field null
        project.setLabel(null);

        // Create the Project, which fails.

        restProjectMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(project)))
            .andExpect(status().isBadRequest());

        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkVersionIsRequired() throws Exception {
        int databaseSizeBeforeTest = projectRepository.findAll().size();
        // set the field null
        project.setVersion(null);

        // Create the Project, which fails.

        restProjectMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(project)))
            .andExpect(status().isBadRequest());

        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllProjects() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList
        restProjectMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(project.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].identifier").value(hasItem(DEFAULT_IDENTIFIER)))
            .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastUpdatedDate").value(hasItem(DEFAULT_LAST_UPDATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].uploadState").value(hasItem(DEFAULT_UPLOAD_STATE.toString())))
            .andExpect(jsonPath("$.[*].disclaimer").value(hasItem(DEFAULT_DISCLAIMER.toString())))
            .andExpect(jsonPath("$.[*].delivered").value(hasItem(DEFAULT_DELIVERED.booleanValue())))
            .andExpect(jsonPath("$.[*].deliveredDate").value(hasItem(DEFAULT_DELIVERED_DATE.toString())))
            .andExpect(jsonPath("$.[*].contact").value(hasItem(DEFAULT_CONTACT)))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)))
            .andExpect(jsonPath("$.[*].uploadFilter").value(hasItem(DEFAULT_UPLOAD_FILTER)));
    }

    @Test
    @Transactional
    void getProject() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get the project
        restProjectMockMvc
            .perform(get(ENTITY_API_URL_ID, project.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(project.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.identifier").value(DEFAULT_IDENTIFIER))
            .andExpect(jsonPath("$.version").value(DEFAULT_VERSION))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()))
            .andExpect(jsonPath("$.lastUpdatedDate").value(DEFAULT_LAST_UPDATED_DATE.toString()))
            .andExpect(jsonPath("$.uploadState").value(DEFAULT_UPLOAD_STATE.toString()))
            .andExpect(jsonPath("$.disclaimer").value(DEFAULT_DISCLAIMER.toString()))
            .andExpect(jsonPath("$.delivered").value(DEFAULT_DELIVERED.booleanValue()))
            .andExpect(jsonPath("$.deliveredDate").value(DEFAULT_DELIVERED_DATE.toString()))
            .andExpect(jsonPath("$.contact").value(DEFAULT_CONTACT))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT))
            .andExpect(jsonPath("$.uploadFilter").value(DEFAULT_UPLOAD_FILTER));
    }

    @Test
    @Transactional
    void getProjectsByIdFiltering() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        Long id = project.getId();

        defaultProjectShouldBeFound("id.equals=" + id);
        defaultProjectShouldNotBeFound("id.notEquals=" + id);

        defaultProjectShouldBeFound("id.greaterThanOrEqual=" + id);
        defaultProjectShouldNotBeFound("id.greaterThan=" + id);

        defaultProjectShouldBeFound("id.lessThanOrEqual=" + id);
        defaultProjectShouldNotBeFound("id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllProjectsByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where name equals to DEFAULT_NAME
        defaultProjectShouldBeFound("name.equals=" + DEFAULT_NAME);

        // Get all the projectList where name equals to UPDATED_NAME
        defaultProjectShouldNotBeFound("name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllProjectsByNameIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where name not equals to DEFAULT_NAME
        defaultProjectShouldNotBeFound("name.notEquals=" + DEFAULT_NAME);

        // Get all the projectList where name not equals to UPDATED_NAME
        defaultProjectShouldBeFound("name.notEquals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllProjectsByNameIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where name in DEFAULT_NAME or UPDATED_NAME
        defaultProjectShouldBeFound("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME);

        // Get all the projectList where name equals to UPDATED_NAME
        defaultProjectShouldNotBeFound("name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllProjectsByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where name is not null
        defaultProjectShouldBeFound("name.specified=true");

        // Get all the projectList where name is null
        defaultProjectShouldNotBeFound("name.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByNameContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where name contains DEFAULT_NAME
        defaultProjectShouldBeFound("name.contains=" + DEFAULT_NAME);

        // Get all the projectList where name contains UPDATED_NAME
        defaultProjectShouldNotBeFound("name.contains=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllProjectsByNameNotContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where name does not contain DEFAULT_NAME
        defaultProjectShouldNotBeFound("name.doesNotContain=" + DEFAULT_NAME);

        // Get all the projectList where name does not contain UPDATED_NAME
        defaultProjectShouldBeFound("name.doesNotContain=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllProjectsByIdentifierIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where identifier equals to DEFAULT_IDENTIFIER
        defaultProjectShouldBeFound("identifier.equals=" + DEFAULT_IDENTIFIER);

        // Get all the projectList where identifier equals to UPDATED_IDENTIFIER
        defaultProjectShouldNotBeFound("identifier.equals=" + UPDATED_IDENTIFIER);
    }

    @Test
    @Transactional
    void getAllProjectsByIdentifierIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where identifier not equals to DEFAULT_IDENTIFIER
        defaultProjectShouldNotBeFound("identifier.notEquals=" + DEFAULT_IDENTIFIER);

        // Get all the projectList where identifier not equals to UPDATED_IDENTIFIER
        defaultProjectShouldBeFound("identifier.notEquals=" + UPDATED_IDENTIFIER);
    }

    @Test
    @Transactional
    void getAllProjectsByIdentifierIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where identifier in DEFAULT_IDENTIFIER or UPDATED_IDENTIFIER
        defaultProjectShouldBeFound("identifier.in=" + DEFAULT_IDENTIFIER + "," + UPDATED_IDENTIFIER);

        // Get all the projectList where identifier equals to UPDATED_IDENTIFIER
        defaultProjectShouldNotBeFound("identifier.in=" + UPDATED_IDENTIFIER);
    }

    @Test
    @Transactional
    void getAllProjectsByIdentifierIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where identifier is not null
        defaultProjectShouldBeFound("identifier.specified=true");

        // Get all the projectList where identifier is null
        defaultProjectShouldNotBeFound("identifier.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByIdentifierContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where identifier contains DEFAULT_IDENTIFIER
        defaultProjectShouldBeFound("identifier.contains=" + DEFAULT_IDENTIFIER);

        // Get all the projectList where identifier contains UPDATED_IDENTIFIER
        defaultProjectShouldNotBeFound("identifier.contains=" + UPDATED_IDENTIFIER);
    }

    @Test
    @Transactional
    void getAllProjectsByIdentifierNotContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where identifier does not contain DEFAULT_IDENTIFIER
        defaultProjectShouldNotBeFound("identifier.doesNotContain=" + DEFAULT_IDENTIFIER);

        // Get all the projectList where identifier does not contain UPDATED_IDENTIFIER
        defaultProjectShouldBeFound("identifier.doesNotContain=" + UPDATED_IDENTIFIER);
    }

    @Test
    @Transactional
    void getAllProjectsByVersionIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where version equals to DEFAULT_VERSION
        defaultProjectShouldBeFound("version.equals=" + DEFAULT_VERSION);

        // Get all the projectList where version equals to UPDATED_VERSION
        defaultProjectShouldNotBeFound("version.equals=" + UPDATED_VERSION);
    }

    @Test
    @Transactional
    void getAllProjectsByVersionIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where version not equals to DEFAULT_VERSION
        defaultProjectShouldNotBeFound("version.notEquals=" + DEFAULT_VERSION);

        // Get all the projectList where version not equals to UPDATED_VERSION
        defaultProjectShouldBeFound("version.notEquals=" + UPDATED_VERSION);
    }

    @Test
    @Transactional
    void getAllProjectsByVersionIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where version in DEFAULT_VERSION or UPDATED_VERSION
        defaultProjectShouldBeFound("version.in=" + DEFAULT_VERSION + "," + UPDATED_VERSION);

        // Get all the projectList where version equals to UPDATED_VERSION
        defaultProjectShouldNotBeFound("version.in=" + UPDATED_VERSION);
    }

    @Test
    @Transactional
    void getAllProjectsByVersionIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where version is not null
        defaultProjectShouldBeFound("version.specified=true");

        // Get all the projectList where version is null
        defaultProjectShouldNotBeFound("version.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByVersionContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where version contains DEFAULT_VERSION
        defaultProjectShouldBeFound("version.contains=" + DEFAULT_VERSION);

        // Get all the projectList where version contains UPDATED_VERSION
        defaultProjectShouldNotBeFound("version.contains=" + UPDATED_VERSION);
    }

    @Test
    @Transactional
    void getAllProjectsByVersionNotContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where version does not contain DEFAULT_VERSION
        defaultProjectShouldNotBeFound("version.doesNotContain=" + DEFAULT_VERSION);

        // Get all the projectList where version does not contain UPDATED_VERSION
        defaultProjectShouldBeFound("version.doesNotContain=" + UPDATED_VERSION);
    }

    @Test
    @Transactional
    void getAllProjectsByCreatedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where createdDate equals to DEFAULT_CREATED_DATE
        defaultProjectShouldBeFound("createdDate.equals=" + DEFAULT_CREATED_DATE);

        // Get all the projectList where createdDate equals to UPDATED_CREATED_DATE
        defaultProjectShouldNotBeFound("createdDate.equals=" + UPDATED_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByCreatedDateIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where createdDate not equals to DEFAULT_CREATED_DATE
        defaultProjectShouldNotBeFound("createdDate.notEquals=" + DEFAULT_CREATED_DATE);

        // Get all the projectList where createdDate not equals to UPDATED_CREATED_DATE
        defaultProjectShouldBeFound("createdDate.notEquals=" + UPDATED_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByCreatedDateIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where createdDate in DEFAULT_CREATED_DATE or UPDATED_CREATED_DATE
        defaultProjectShouldBeFound("createdDate.in=" + DEFAULT_CREATED_DATE + "," + UPDATED_CREATED_DATE);

        // Get all the projectList where createdDate equals to UPDATED_CREATED_DATE
        defaultProjectShouldNotBeFound("createdDate.in=" + UPDATED_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByCreatedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where createdDate is not null
        defaultProjectShouldBeFound("createdDate.specified=true");

        // Get all the projectList where createdDate is null
        defaultProjectShouldNotBeFound("createdDate.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByCreatedDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where createdDate is greater than or equal to DEFAULT_CREATED_DATE
        defaultProjectShouldBeFound("createdDate.greaterThanOrEqual=" + DEFAULT_CREATED_DATE);

        // Get all the projectList where createdDate is greater than or equal to UPDATED_CREATED_DATE
        defaultProjectShouldNotBeFound("createdDate.greaterThanOrEqual=" + UPDATED_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByCreatedDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where createdDate is less than or equal to DEFAULT_CREATED_DATE
        defaultProjectShouldBeFound("createdDate.lessThanOrEqual=" + DEFAULT_CREATED_DATE);

        // Get all the projectList where createdDate is less than or equal to SMALLER_CREATED_DATE
        defaultProjectShouldNotBeFound("createdDate.lessThanOrEqual=" + SMALLER_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByCreatedDateIsLessThanSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where createdDate is less than DEFAULT_CREATED_DATE
        defaultProjectShouldNotBeFound("createdDate.lessThan=" + DEFAULT_CREATED_DATE);

        // Get all the projectList where createdDate is less than UPDATED_CREATED_DATE
        defaultProjectShouldBeFound("createdDate.lessThan=" + UPDATED_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByCreatedDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where createdDate is greater than DEFAULT_CREATED_DATE
        defaultProjectShouldNotBeFound("createdDate.greaterThan=" + DEFAULT_CREATED_DATE);

        // Get all the projectList where createdDate is greater than SMALLER_CREATED_DATE
        defaultProjectShouldBeFound("createdDate.greaterThan=" + SMALLER_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByLastUpdatedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where lastUpdatedDate equals to DEFAULT_LAST_UPDATED_DATE
        defaultProjectShouldBeFound("lastUpdatedDate.equals=" + DEFAULT_LAST_UPDATED_DATE);

        // Get all the projectList where lastUpdatedDate equals to UPDATED_LAST_UPDATED_DATE
        defaultProjectShouldNotBeFound("lastUpdatedDate.equals=" + UPDATED_LAST_UPDATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByLastUpdatedDateIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where lastUpdatedDate not equals to DEFAULT_LAST_UPDATED_DATE
        defaultProjectShouldNotBeFound("lastUpdatedDate.notEquals=" + DEFAULT_LAST_UPDATED_DATE);

        // Get all the projectList where lastUpdatedDate not equals to UPDATED_LAST_UPDATED_DATE
        defaultProjectShouldBeFound("lastUpdatedDate.notEquals=" + UPDATED_LAST_UPDATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByLastUpdatedDateIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where lastUpdatedDate in DEFAULT_LAST_UPDATED_DATE or UPDATED_LAST_UPDATED_DATE
        defaultProjectShouldBeFound("lastUpdatedDate.in=" + DEFAULT_LAST_UPDATED_DATE + "," + UPDATED_LAST_UPDATED_DATE);

        // Get all the projectList where lastUpdatedDate equals to UPDATED_LAST_UPDATED_DATE
        defaultProjectShouldNotBeFound("lastUpdatedDate.in=" + UPDATED_LAST_UPDATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByLastUpdatedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where lastUpdatedDate is not null
        defaultProjectShouldBeFound("lastUpdatedDate.specified=true");

        // Get all the projectList where lastUpdatedDate is null
        defaultProjectShouldNotBeFound("lastUpdatedDate.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByLastUpdatedDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where lastUpdatedDate is greater than or equal to DEFAULT_LAST_UPDATED_DATE
        defaultProjectShouldBeFound("lastUpdatedDate.greaterThanOrEqual=" + DEFAULT_LAST_UPDATED_DATE);

        // Get all the projectList where lastUpdatedDate is greater than or equal to UPDATED_LAST_UPDATED_DATE
        defaultProjectShouldNotBeFound("lastUpdatedDate.greaterThanOrEqual=" + UPDATED_LAST_UPDATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByLastUpdatedDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where lastUpdatedDate is less than or equal to DEFAULT_LAST_UPDATED_DATE
        defaultProjectShouldBeFound("lastUpdatedDate.lessThanOrEqual=" + DEFAULT_LAST_UPDATED_DATE);

        // Get all the projectList where lastUpdatedDate is less than or equal to SMALLER_LAST_UPDATED_DATE
        defaultProjectShouldNotBeFound("lastUpdatedDate.lessThanOrEqual=" + SMALLER_LAST_UPDATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByLastUpdatedDateIsLessThanSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where lastUpdatedDate is less than DEFAULT_LAST_UPDATED_DATE
        defaultProjectShouldNotBeFound("lastUpdatedDate.lessThan=" + DEFAULT_LAST_UPDATED_DATE);

        // Get all the projectList where lastUpdatedDate is less than UPDATED_LAST_UPDATED_DATE
        defaultProjectShouldBeFound("lastUpdatedDate.lessThan=" + UPDATED_LAST_UPDATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByLastUpdatedDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where lastUpdatedDate is greater than DEFAULT_LAST_UPDATED_DATE
        defaultProjectShouldNotBeFound("lastUpdatedDate.greaterThan=" + DEFAULT_LAST_UPDATED_DATE);

        // Get all the projectList where lastUpdatedDate is greater than SMALLER_LAST_UPDATED_DATE
        defaultProjectShouldBeFound("lastUpdatedDate.greaterThan=" + SMALLER_LAST_UPDATED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByUploadStateIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where uploadState equals to DEFAULT_UPLOAD_STATE
        defaultProjectShouldBeFound("uploadState.equals=" + DEFAULT_UPLOAD_STATE);

        // Get all the projectList where uploadState equals to UPDATED_UPLOAD_STATE
        defaultProjectShouldNotBeFound("uploadState.equals=" + UPDATED_UPLOAD_STATE);
    }

    @Test
    @Transactional
    void getAllProjectsByUploadStateIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where uploadState not equals to DEFAULT_UPLOAD_STATE
        defaultProjectShouldNotBeFound("uploadState.notEquals=" + DEFAULT_UPLOAD_STATE);

        // Get all the projectList where uploadState not equals to UPDATED_UPLOAD_STATE
        defaultProjectShouldBeFound("uploadState.notEquals=" + UPDATED_UPLOAD_STATE);
    }

    @Test
    @Transactional
    void getAllProjectsByUploadStateIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where uploadState in DEFAULT_UPLOAD_STATE or UPDATED_UPLOAD_STATE
        defaultProjectShouldBeFound("uploadState.in=" + DEFAULT_UPLOAD_STATE + "," + UPDATED_UPLOAD_STATE);

        // Get all the projectList where uploadState equals to UPDATED_UPLOAD_STATE
        defaultProjectShouldNotBeFound("uploadState.in=" + UPDATED_UPLOAD_STATE);
    }

    @Test
    @Transactional
    void getAllProjectsByUploadStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where uploadState is not null
        defaultProjectShouldBeFound("uploadState.specified=true");

        // Get all the projectList where uploadState is null
        defaultProjectShouldNotBeFound("uploadState.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByDeliveredIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where delivered equals to DEFAULT_DELIVERED
        defaultProjectShouldBeFound("delivered.equals=" + DEFAULT_DELIVERED);

        // Get all the projectList where delivered equals to UPDATED_DELIVERED
        defaultProjectShouldNotBeFound("delivered.equals=" + UPDATED_DELIVERED);
    }

    @Test
    @Transactional
    void getAllProjectsByDeliveredIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where delivered not equals to DEFAULT_DELIVERED
        defaultProjectShouldNotBeFound("delivered.notEquals=" + DEFAULT_DELIVERED);

        // Get all the projectList where delivered not equals to UPDATED_DELIVERED
        defaultProjectShouldBeFound("delivered.notEquals=" + UPDATED_DELIVERED);
    }

    @Test
    @Transactional
    void getAllProjectsByDeliveredIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where delivered in DEFAULT_DELIVERED or UPDATED_DELIVERED
        defaultProjectShouldBeFound("delivered.in=" + DEFAULT_DELIVERED + "," + UPDATED_DELIVERED);

        // Get all the projectList where delivered equals to UPDATED_DELIVERED
        defaultProjectShouldNotBeFound("delivered.in=" + UPDATED_DELIVERED);
    }

    @Test
    @Transactional
    void getAllProjectsByDeliveredIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where delivered is not null
        defaultProjectShouldBeFound("delivered.specified=true");

        // Get all the projectList where delivered is null
        defaultProjectShouldNotBeFound("delivered.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByDeliveredDateIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where deliveredDate equals to DEFAULT_DELIVERED_DATE
        defaultProjectShouldBeFound("deliveredDate.equals=" + DEFAULT_DELIVERED_DATE);

        // Get all the projectList where deliveredDate equals to UPDATED_DELIVERED_DATE
        defaultProjectShouldNotBeFound("deliveredDate.equals=" + UPDATED_DELIVERED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByDeliveredDateIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where deliveredDate not equals to DEFAULT_DELIVERED_DATE
        defaultProjectShouldNotBeFound("deliveredDate.notEquals=" + DEFAULT_DELIVERED_DATE);

        // Get all the projectList where deliveredDate not equals to UPDATED_DELIVERED_DATE
        defaultProjectShouldBeFound("deliveredDate.notEquals=" + UPDATED_DELIVERED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByDeliveredDateIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where deliveredDate in DEFAULT_DELIVERED_DATE or UPDATED_DELIVERED_DATE
        defaultProjectShouldBeFound("deliveredDate.in=" + DEFAULT_DELIVERED_DATE + "," + UPDATED_DELIVERED_DATE);

        // Get all the projectList where deliveredDate equals to UPDATED_DELIVERED_DATE
        defaultProjectShouldNotBeFound("deliveredDate.in=" + UPDATED_DELIVERED_DATE);
    }

    @Test
    @Transactional
    void getAllProjectsByDeliveredDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where deliveredDate is not null
        defaultProjectShouldBeFound("deliveredDate.specified=true");

        // Get all the projectList where deliveredDate is null
        defaultProjectShouldNotBeFound("deliveredDate.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByContactIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where contact equals to DEFAULT_CONTACT
        defaultProjectShouldBeFound("contact.equals=" + DEFAULT_CONTACT);

        // Get all the projectList where contact equals to UPDATED_CONTACT
        defaultProjectShouldNotBeFound("contact.equals=" + UPDATED_CONTACT);
    }

    @Test
    @Transactional
    void getAllProjectsByContactIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where contact not equals to DEFAULT_CONTACT
        defaultProjectShouldNotBeFound("contact.notEquals=" + DEFAULT_CONTACT);

        // Get all the projectList where contact not equals to UPDATED_CONTACT
        defaultProjectShouldBeFound("contact.notEquals=" + UPDATED_CONTACT);
    }

    @Test
    @Transactional
    void getAllProjectsByContactIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where contact in DEFAULT_CONTACT or UPDATED_CONTACT
        defaultProjectShouldBeFound("contact.in=" + DEFAULT_CONTACT + "," + UPDATED_CONTACT);

        // Get all the projectList where contact equals to UPDATED_CONTACT
        defaultProjectShouldNotBeFound("contact.in=" + UPDATED_CONTACT);
    }

    @Test
    @Transactional
    void getAllProjectsByContactIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where contact is not null
        defaultProjectShouldBeFound("contact.specified=true");

        // Get all the projectList where contact is null
        defaultProjectShouldNotBeFound("contact.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByContactContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where contact contains DEFAULT_CONTACT
        defaultProjectShouldBeFound("contact.contains=" + DEFAULT_CONTACT);

        // Get all the projectList where contact contains UPDATED_CONTACT
        defaultProjectShouldNotBeFound("contact.contains=" + UPDATED_CONTACT);
    }

    @Test
    @Transactional
    void getAllProjectsByContactNotContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where contact does not contain DEFAULT_CONTACT
        defaultProjectShouldNotBeFound("contact.doesNotContain=" + DEFAULT_CONTACT);

        // Get all the projectList where contact does not contain UPDATED_CONTACT
        defaultProjectShouldBeFound("contact.doesNotContain=" + UPDATED_CONTACT);
    }

    @Test
    @Transactional
    void getAllProjectsByCommentIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where comment equals to DEFAULT_COMMENT
        defaultProjectShouldBeFound("comment.equals=" + DEFAULT_COMMENT);

        // Get all the projectList where comment equals to UPDATED_COMMENT
        defaultProjectShouldNotBeFound("comment.equals=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    void getAllProjectsByCommentIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where comment not equals to DEFAULT_COMMENT
        defaultProjectShouldNotBeFound("comment.notEquals=" + DEFAULT_COMMENT);

        // Get all the projectList where comment not equals to UPDATED_COMMENT
        defaultProjectShouldBeFound("comment.notEquals=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    void getAllProjectsByCommentIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where comment in DEFAULT_COMMENT or UPDATED_COMMENT
        defaultProjectShouldBeFound("comment.in=" + DEFAULT_COMMENT + "," + UPDATED_COMMENT);

        // Get all the projectList where comment equals to UPDATED_COMMENT
        defaultProjectShouldNotBeFound("comment.in=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    void getAllProjectsByCommentIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where comment is not null
        defaultProjectShouldBeFound("comment.specified=true");

        // Get all the projectList where comment is null
        defaultProjectShouldNotBeFound("comment.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByCommentContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where comment contains DEFAULT_COMMENT
        defaultProjectShouldBeFound("comment.contains=" + DEFAULT_COMMENT);

        // Get all the projectList where comment contains UPDATED_COMMENT
        defaultProjectShouldNotBeFound("comment.contains=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    void getAllProjectsByCommentNotContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where comment does not contain DEFAULT_COMMENT
        defaultProjectShouldNotBeFound("comment.doesNotContain=" + DEFAULT_COMMENT);

        // Get all the projectList where comment does not contain UPDATED_COMMENT
        defaultProjectShouldBeFound("comment.doesNotContain=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    void getAllProjectsByUploadFilterIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where uploadFilter equals to DEFAULT_UPLOAD_FILTER
        defaultProjectShouldBeFound("uploadFilter.equals=" + DEFAULT_UPLOAD_FILTER);

        // Get all the projectList where uploadFilter equals to UPDATED_UPLOAD_FILTER
        defaultProjectShouldNotBeFound("uploadFilter.equals=" + UPDATED_UPLOAD_FILTER);
    }

    @Test
    @Transactional
    void getAllProjectsByUploadFilterIsNotEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where uploadFilter not equals to DEFAULT_UPLOAD_FILTER
        defaultProjectShouldNotBeFound("uploadFilter.notEquals=" + DEFAULT_UPLOAD_FILTER);

        // Get all the projectList where uploadFilter not equals to UPDATED_UPLOAD_FILTER
        defaultProjectShouldBeFound("uploadFilter.notEquals=" + UPDATED_UPLOAD_FILTER);
    }

    @Test
    @Transactional
    void getAllProjectsByUploadFilterIsInShouldWork() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where uploadFilter in DEFAULT_UPLOAD_FILTER or UPDATED_UPLOAD_FILTER
        defaultProjectShouldBeFound("uploadFilter.in=" + DEFAULT_UPLOAD_FILTER + "," + UPDATED_UPLOAD_FILTER);

        // Get all the projectList where uploadFilter equals to UPDATED_UPLOAD_FILTER
        defaultProjectShouldNotBeFound("uploadFilter.in=" + UPDATED_UPLOAD_FILTER);
    }

    @Test
    @Transactional
    void getAllProjectsByUploadFilterIsNullOrNotNull() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where uploadFilter is not null
        defaultProjectShouldBeFound("uploadFilter.specified=true");

        // Get all the projectList where uploadFilter is null
        defaultProjectShouldNotBeFound("uploadFilter.specified=false");
    }

    @Test
    @Transactional
    void getAllProjectsByUploadFilterContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where uploadFilter contains DEFAULT_UPLOAD_FILTER
        defaultProjectShouldBeFound("uploadFilter.contains=" + DEFAULT_UPLOAD_FILTER);

        // Get all the projectList where uploadFilter contains UPDATED_UPLOAD_FILTER
        defaultProjectShouldNotBeFound("uploadFilter.contains=" + UPDATED_UPLOAD_FILTER);
    }

    @Test
    @Transactional
    void getAllProjectsByUploadFilterNotContainsSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        // Get all the projectList where uploadFilter does not contain DEFAULT_UPLOAD_FILTER
        defaultProjectShouldNotBeFound("uploadFilter.doesNotContain=" + DEFAULT_UPLOAD_FILTER);

        // Get all the projectList where uploadFilter does not contain UPDATED_UPLOAD_FILTER
        defaultProjectShouldBeFound("uploadFilter.doesNotContain=" + UPDATED_UPLOAD_FILTER);
    }

    @Test
    @Transactional
    void getAllProjectsByLibraryIsEqualToSomething() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);
        Dependency library;
        if (TestUtil.findAll(em, Dependency.class).isEmpty()) {
            library = DependencyResourceIT.createEntity(em);
            em.persist(library);
            em.flush();
        } else {
            library = TestUtil.findAll(em, Dependency.class).get(0);
        }
        em.persist(library);
        em.flush();
        project.addLibrary(library);
        projectRepository.saveAndFlush(project);
        Long libraryId = library.getId();

        // Get all the projectList where library equals to libraryId
        defaultProjectShouldBeFound("libraryId.equals=" + libraryId);

        // Get all the projectList where library equals to (libraryId + 1)
        defaultProjectShouldNotBeFound("libraryId.equals=" + (libraryId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultProjectShouldBeFound(String filter) throws Exception {
        restProjectMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(project.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].identifier").value(hasItem(DEFAULT_IDENTIFIER)))
            .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastUpdatedDate").value(hasItem(DEFAULT_LAST_UPDATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].uploadState").value(hasItem(DEFAULT_UPLOAD_STATE.toString())))
            .andExpect(jsonPath("$.[*].disclaimer").value(hasItem(DEFAULT_DISCLAIMER.toString())))
            .andExpect(jsonPath("$.[*].delivered").value(hasItem(DEFAULT_DELIVERED.booleanValue())))
            .andExpect(jsonPath("$.[*].deliveredDate").value(hasItem(DEFAULT_DELIVERED_DATE.toString())))
            .andExpect(jsonPath("$.[*].contact").value(hasItem(DEFAULT_CONTACT)))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)))
            .andExpect(jsonPath("$.[*].uploadFilter").value(hasItem(DEFAULT_UPLOAD_FILTER)));

        // Check, that the count call also returns 1
        restProjectMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultProjectShouldNotBeFound(String filter) throws Exception {
        restProjectMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restProjectMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingProject() throws Exception {
        // Get the project
        restProjectMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewProject() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        int databaseSizeBeforeUpdate = projectRepository.findAll().size();

        // Update the project
        Project updatedProject = projectRepository.findById(project.getId()).get();
        // Disconnect from session so that the updates on updatedProject are not directly saved in db
        em.detach(updatedProject);
        updatedProject
            .name(UPDATED_NAME)
            .label(UPDATED_IDENTIFIER)
            .version(UPDATED_VERSION)
            .createdDate(UPDATED_CREATED_DATE)
            .lastUpdatedDate(UPDATED_LAST_UPDATED_DATE)
            .uploadState(UPDATED_UPLOAD_STATE)
            .disclaimer(UPDATED_DISCLAIMER)
            .delivered(UPDATED_DELIVERED)
            .deliveredDate(UPDATED_DELIVERED_DATE)
            .contact(UPDATED_CONTACT)
            .comment(UPDATED_COMMENT)
            .uploadFilter(UPDATED_UPLOAD_FILTER);

        restProjectMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedProject.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(updatedProject))
            )
            .andExpect(status().isOk());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeUpdate);
        Project testProject = projectList.get(projectList.size() - 1);
        assertThat(testProject.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testProject.getLabel()).isEqualTo(UPDATED_IDENTIFIER);
        assertThat(testProject.getVersion()).isEqualTo(UPDATED_VERSION);
        assertThat(testProject.getCreatedDate()).isEqualTo(UPDATED_CREATED_DATE);
        assertThat(testProject.getLastUpdatedDate()).isEqualTo(UPDATED_LAST_UPDATED_DATE);
        assertThat(testProject.getUploadState()).isEqualTo(UPDATED_UPLOAD_STATE);
        assertThat(testProject.getDisclaimer()).isEqualTo(UPDATED_DISCLAIMER);
        assertThat(testProject.getDelivered()).isEqualTo(UPDATED_DELIVERED);
        assertThat(testProject.getDeliveredDate()).isEqualTo(UPDATED_DELIVERED_DATE);
        assertThat(testProject.getContact()).isEqualTo(UPDATED_CONTACT);
        assertThat(testProject.getComment()).isEqualTo(UPDATED_COMMENT);
        assertThat(testProject.getUploadFilter()).isEqualTo(UPDATED_UPLOAD_FILTER);
    }

    @Test
    @Transactional
    void putNonExistingProject() throws Exception {
        int databaseSizeBeforeUpdate = projectRepository.findAll().size();
        project.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProjectMockMvc
            .perform(
                put(ENTITY_API_URL_ID, project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(project))
            )
            .andExpect(status().isBadRequest());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchProject() throws Exception {
        int databaseSizeBeforeUpdate = projectRepository.findAll().size();
        project.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProjectMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(project))
            )
            .andExpect(status().isBadRequest());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamProject() throws Exception {
        int databaseSizeBeforeUpdate = projectRepository.findAll().size();
        project.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProjectMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(project)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateProjectWithPatch() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        int databaseSizeBeforeUpdate = projectRepository.findAll().size();

        // Update the project using partial update
        Project partialUpdatedProject = new Project();
        partialUpdatedProject.setId(project.getId());

        partialUpdatedProject
            .lastUpdatedDate(UPDATED_LAST_UPDATED_DATE)
            .contact(UPDATED_CONTACT)
            .uploadFilter(UPDATED_UPLOAD_FILTER);

        restProjectMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProject.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedProject))
            )
            .andExpect(status().isOk());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeUpdate);
        Project testProject = projectList.get(projectList.size() - 1);
        assertThat(testProject.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testProject.getLabel()).isEqualTo(DEFAULT_IDENTIFIER);
        assertThat(testProject.getVersion()).isEqualTo(DEFAULT_VERSION);
        assertThat(testProject.getCreatedDate()).isEqualTo(DEFAULT_CREATED_DATE);
        assertThat(testProject.getLastUpdatedDate()).isEqualTo(UPDATED_LAST_UPDATED_DATE);
        assertThat(testProject.getUploadState()).isEqualTo(DEFAULT_UPLOAD_STATE);
        assertThat(testProject.getDisclaimer()).isEqualTo(DEFAULT_DISCLAIMER);
        assertThat(testProject.getDelivered()).isEqualTo(DEFAULT_DELIVERED);
        assertThat(testProject.getDeliveredDate()).isEqualTo(DEFAULT_DELIVERED_DATE);
        assertThat(testProject.getContact()).isEqualTo(UPDATED_CONTACT);
        assertThat(testProject.getComment()).isEqualTo(DEFAULT_COMMENT);
        assertThat(testProject.getUploadFilter()).isEqualTo(UPDATED_UPLOAD_FILTER);
    }

    @Test
    @Transactional
    void fullUpdateProjectWithPatch() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        int databaseSizeBeforeUpdate = projectRepository.findAll().size();

        // Update the project using partial update
        Project partialUpdatedProject = new Project();
        partialUpdatedProject.setId(project.getId());

        partialUpdatedProject
            .name(UPDATED_NAME)
            .label(UPDATED_IDENTIFIER)
            .version(UPDATED_VERSION)
            .createdDate(UPDATED_CREATED_DATE)
            .lastUpdatedDate(UPDATED_LAST_UPDATED_DATE)
            .uploadState(UPDATED_UPLOAD_STATE)
            .disclaimer(UPDATED_DISCLAIMER)
            .delivered(UPDATED_DELIVERED)
            .deliveredDate(UPDATED_DELIVERED_DATE)
            .contact(UPDATED_CONTACT)
            .comment(UPDATED_COMMENT)
            .uploadFilter(UPDATED_UPLOAD_FILTER);

        restProjectMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProject.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedProject))
            )
            .andExpect(status().isOk());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeUpdate);
        Project testProject = projectList.get(projectList.size() - 1);
        assertThat(testProject.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testProject.getLabel()).isEqualTo(UPDATED_IDENTIFIER);
        assertThat(testProject.getVersion()).isEqualTo(UPDATED_VERSION);
        assertThat(testProject.getCreatedDate()).isEqualTo(UPDATED_CREATED_DATE);
        assertThat(testProject.getLastUpdatedDate()).isEqualTo(UPDATED_LAST_UPDATED_DATE);
        assertThat(testProject.getUploadState()).isEqualTo(UPDATED_UPLOAD_STATE);
        assertThat(testProject.getDisclaimer()).isEqualTo(UPDATED_DISCLAIMER);
        assertThat(testProject.getDelivered()).isEqualTo(UPDATED_DELIVERED);
        assertThat(testProject.getDeliveredDate()).isEqualTo(UPDATED_DELIVERED_DATE);
        assertThat(testProject.getContact()).isEqualTo(UPDATED_CONTACT);
        assertThat(testProject.getComment()).isEqualTo(UPDATED_COMMENT);
        assertThat(testProject.getUploadFilter()).isEqualTo(UPDATED_UPLOAD_FILTER);
    }

    @Test
    @Transactional
    void patchNonExistingProject() throws Exception {
        int databaseSizeBeforeUpdate = projectRepository.findAll().size();
        project.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProjectMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, project.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(project))
            )
            .andExpect(status().isBadRequest());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchProject() throws Exception {
        int databaseSizeBeforeUpdate = projectRepository.findAll().size();
        project.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProjectMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(project))
            )
            .andExpect(status().isBadRequest());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamProject() throws Exception {
        int databaseSizeBeforeUpdate = projectRepository.findAll().size();
        project.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProjectMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(project)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Project in the database
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteProject() throws Exception {
        // Initialize the database
        projectRepository.saveAndFlush(project);

        int databaseSizeBeforeDelete = projectRepository.findAll().size();

        // Delete the project
        restProjectMockMvc
            .perform(delete(ENTITY_API_URL_ID, project.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Project> projectList = projectRepository.findAll();
        assertThat(projectList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
