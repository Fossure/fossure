package io.github.fossure.service.dto;

import io.github.fossure.domain.LicenseConflict;
import io.github.fossure.domain.LicenseRisk;
import io.github.fossure.domain.enumeration.CompatibilityState;

/**
 * A DTO representing a {@link LicenseConflict} entity with the minimum of information and the license risk.
 */
public class LicenseConflictWithRiskDTO extends LicenseConflictSimpleDTO {

    public LicenseConflictWithRiskDTO(
        Long id,
        Long licenseId,
        String fullName,
        String shortIdentifier,
        CompatibilityState compatibilityState,
        String comment,
        LicenseRisk licenseRisk
    ) {
        super(id, licenseId, fullName, shortIdentifier, compatibilityState, comment);
        this.setSecondLicenseConflict(new LicenseWithRiskDTO(licenseId, fullName, shortIdentifier, licenseRisk));
    }

    @Override
    public String toString() {
        return (
            "LicenseConflictSimpleDTO{" +
            "id=" +
            getId() +
            ", secondLicenseConflictId=" +
            getSecondLicenseConflict().getId() +
            ", compatibility=" +
            getCompatibility() +
            ", comment='" +
            getComment() +
            '\'' +
            '}'
        );
    }
}
