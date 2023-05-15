package io.github.fossure.service.dto;

import io.github.fossure.domain.License;
import io.github.fossure.domain.LicenseRisk;

/**
 * A DTO representing a {@link License} entity with the minimum of information and license risk.
 */
public class LicenseWithRiskDTO extends LicenseSimpleDTO {

    private LicenseRisk licenseRisk;

    public LicenseWithRiskDTO(Long id, String fullName, String shortIdentifier, LicenseRisk licenseRisk) {
        super(id, fullName, shortIdentifier);
        this.licenseRisk = licenseRisk;
    }

    public LicenseRisk getLicenseRisk() {
        return licenseRisk;
    }

    public void setLicenseRisk(LicenseRisk licenseRisk) {
        this.licenseRisk = licenseRisk;
    }

    @Override
    public String toString() {
        return (
            "LicenseSimpleDTO{" +
            "id=" +
            getId() +
            ", fullName='" +
            getFullName() +
            '\'' +
            ", shortIdentifier='" +
            getShortIdentifier() +
            '\'' +
            '}'
        );
    }
}
