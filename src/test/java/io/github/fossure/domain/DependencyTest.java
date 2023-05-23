package io.github.fossure.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.fossure.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DependencyTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Dependency.class);
        Dependency dependency1 = new Dependency();
        dependency1.setId(1L);
        Dependency dependency2 = new Dependency();
        dependency2.setId(dependency1.getId());
        assertThat(dependency1).isEqualTo(dependency2);
        dependency2.setId(2L);
        assertThat(dependency1).isNotEqualTo(dependency2);
        dependency1.setId(null);
        assertThat(dependency1).isNotEqualTo(dependency2);
    }
}
