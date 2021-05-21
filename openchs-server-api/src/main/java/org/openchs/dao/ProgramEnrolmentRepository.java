package org.openchs.dao;

import org.joda.time.DateTime;
import org.openchs.domain.AddressLevel;
import org.openchs.domain.ProgramEnrolment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(collectionResourceRel = "programEnrolment", path = "programEnrolment", exported = false)
@PreAuthorize("hasAnyAuthority('user','admin','organisation_admin')")
public interface ProgramEnrolmentRepository extends TransactionalDataRepository<ProgramEnrolment>, FindByLastModifiedDateTime<ProgramEnrolment>, OperatingIndividualScopeAwareRepository<ProgramEnrolment> {

    Page<ProgramEnrolment> findByIndividualAddressLevelInAndProgramIdAndAuditLastModifiedDateTimeIsBetweenOrderByAuditLastModifiedDateTimeAscIdAsc(
            List<AddressLevel> addressLevels,
            Long programId,
            DateTime lastModifiedDateTime,
            DateTime now,
            Pageable pageable);

    Page<ProgramEnrolment> findByIndividualFacilityIdAndProgramIdAndAuditLastModifiedDateTimeIsBetweenOrderByAuditLastModifiedDateTimeAscIdAsc(
            long facilityId,
            Long programId,
            DateTime lastModifiedDateTime,
            DateTime now,
            Pageable pageable);

    @Override
    default Page<ProgramEnrolment> syncByCatchment(SyncParameters syncParameters) {
        return findByIndividualAddressLevelInAndProgramIdAndAuditLastModifiedDateTimeIsBetweenOrderByAuditLastModifiedDateTimeAscIdAsc(syncParameters.getAddressLevels(), syncParameters.getFilter(), syncParameters.getLastModifiedDateTime(), syncParameters.getNow(), syncParameters.getPageable());
    }

    @Override
    default Page<ProgramEnrolment> syncByFacility(SyncParameters syncParameters) {
        return findByIndividualFacilityIdAndProgramIdAndAuditLastModifiedDateTimeIsBetweenOrderByAuditLastModifiedDateTimeAscIdAsc(syncParameters.getCatchmentId(), syncParameters.getFilter(), syncParameters.getLastModifiedDateTime(), syncParameters.getNow(), syncParameters.getPageable());
    }

    @Query("select enl from ProgramEnrolment enl " +
            "join enl.individual i " +
            "where enl.program.uuid = :programUUID and enl.isVoided = false and " +
            "i.isVoided = false " +
            "and (coalesce(:locationIds, null) is null OR i.addressLevel.id in :locationIds)")
    Page<ProgramEnrolment> findEnrolments(String programUUID, List<Long> locationIds, Pageable pageable);

    Page<ProgramEnrolment> findByAuditLastModifiedDateTimeIsBetweenAndProgramNameOrderByAuditLastModifiedDateTimeAscIdAsc(
            DateTime lastModifiedDateTime,
            DateTime now,
            String program,
            Pageable pageable);

    Page<ProgramEnrolment> findByProgramNameAndIndividualUuidOrderByAuditLastModifiedDateTimeAscIdAsc(
            String program,
            String individualUuid,
            Pageable pageable);

    ProgramEnrolment findByLegacyId(String legacyId);
}
