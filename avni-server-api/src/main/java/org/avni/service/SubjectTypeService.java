package org.avni.service;

import org.avni.application.Subject;
import org.avni.dao.AvniJobRepository;
import org.avni.dao.ConceptRepository;
import org.avni.dao.OperationalSubjectTypeRepository;
import org.avni.dao.SubjectTypeRepository;
import org.avni.domain.*;
import org.avni.framework.security.UserContextHolder;
import org.avni.web.request.ConceptSyncAttributeContract;
import org.avni.web.request.OperationalSubjectTypeContract;
import org.avni.web.request.SubjectTypeContract;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class SubjectTypeService implements NonScopeAwareService {

    private final Logger logger;
    private final OperationalSubjectTypeRepository operationalSubjectTypeRepository;
    private SubjectTypeRepository subjectTypeRepository;
    private Job syncAttributesJob;
    private JobLauncher syncAttributesJobLauncher;
    private AvniJobRepository avniJobRepository;
    private ConceptRepository conceptRepository;

    @Autowired
    public SubjectTypeService(SubjectTypeRepository subjectTypeRepository,
                              OperationalSubjectTypeRepository operationalSubjectTypeRepository,
                              Job syncAttributesJob,
                              JobLauncher syncAttributesJobLauncher,
                              AvniJobRepository avniJobRepository,
                              ConceptRepository conceptRepository) {
        this.subjectTypeRepository = subjectTypeRepository;
        this.operationalSubjectTypeRepository = operationalSubjectTypeRepository;
        this.syncAttributesJob = syncAttributesJob;
        this.syncAttributesJobLauncher = syncAttributesJobLauncher;
        this.avniJobRepository = avniJobRepository;
        this.conceptRepository = conceptRepository;
        logger = LoggerFactory.getLogger(this.getClass());
    }

    public void saveSubjectType(SubjectTypeContract subjectTypeRequest) {
        logger.info(String.format("Creating subjectType: %s", subjectTypeRequest.toString()));
        SubjectType subjectType = subjectTypeRepository.findByUuid(subjectTypeRequest.getUuid());
        if (subjectType == null) {
            subjectType = new SubjectType();
        }
        subjectType.setUuid(subjectTypeRequest.getUuid());
        subjectType.setVoided(subjectTypeRequest.isVoided());
        subjectType.setName(subjectTypeRequest.getName());
        subjectType.setGroup(subjectTypeRequest.isGroup());
        subjectType.setHousehold(subjectTypeRequest.isHousehold());
        subjectType.setActive(subjectTypeRequest.getActive());
        subjectType.setAllowEmptyLocation(subjectTypeRequest.isAllowEmptyLocation());
        subjectType.setAllowProfilePicture(subjectTypeRequest.isAllowProfilePicture());
        subjectType.setUniqueName(subjectTypeRequest.isUniqueName());
        subjectType.setValidFirstNameFormat(subjectTypeRequest.getValidFirstNameFormat());
        subjectType.setValidLastNameFormat(subjectTypeRequest.getValidLastNameFormat());
        subjectType.setType(Subject.valueOf(subjectTypeRequest.getType()));
        subjectType.setSubjectSummaryRule(subjectTypeRequest.getSubjectSummaryRule());
        subjectType.setIconFileS3Key(subjectTypeRequest.getIconFileS3Key());
        subjectType.setShouldSyncByLocation(subjectTypeRequest.isShouldSyncByLocation());
        subjectType.setDirectlyAssignable(subjectTypeRequest.isDirectlyAssignable());
        subjectType.setSyncRegistrationConcept1(subjectTypeRequest.getSyncRegistrationConcept1());
        subjectType.setSyncRegistrationConcept1Usable(subjectTypeRequest.getSyncRegistrationConcept1Usable());
        subjectType.setSyncRegistrationConcept2(subjectTypeRequest.getSyncRegistrationConcept2());
        subjectType.setSyncRegistrationConcept2Usable(subjectTypeRequest.getSyncRegistrationConcept2Usable());
        subjectType.setNameHelpText(subjectTypeRequest.getNameHelpText());
        subjectTypeRepository.save(subjectType);
    }

    public void createOperationalSubjectType(OperationalSubjectTypeContract operationalSubjectTypeContract, Organisation organisation) {
        String subjectTypeUUID = operationalSubjectTypeContract.getSubjectType().getUuid();
        SubjectType subjectType = subjectTypeRepository.findByUuid(subjectTypeUUID);
        if (subjectType == null) {
            logger.info(String.format("SubjectType not found for uuid: '%s'", subjectTypeUUID));
        }

        OperationalSubjectType operationalSubjectType = operationalSubjectTypeRepository.findByUuid(operationalSubjectTypeContract.getUuid());

        if (operationalSubjectType == null) {
            operationalSubjectType = new OperationalSubjectType();
        }

        operationalSubjectType.setUuid(operationalSubjectTypeContract.getUuid());
        operationalSubjectType.setName(operationalSubjectTypeContract.getName());
        operationalSubjectType.setSubjectType(subjectType);
        operationalSubjectType.setOrganisationId(organisation.getId());
        operationalSubjectType.setVoided(operationalSubjectTypeContract.isVoided());
        operationalSubjectTypeRepository.save(operationalSubjectType);
    }

    public SubjectType createIndividualSubjectType() {
        SubjectType subjectType = new SubjectType();
        subjectType.assignUUID();
        subjectType.setName("Individual");
        SubjectType savedSubjectType = subjectTypeRepository.save(subjectType);
        saveIndividualOperationalSubjectType(savedSubjectType);
        return savedSubjectType;
    }

    private void saveIndividualOperationalSubjectType(SubjectType subjectType) {
        OperationalSubjectType operationalSubjectType = new OperationalSubjectType();
        operationalSubjectType.assignUUID();
        operationalSubjectType.setName(subjectType.getName());
        operationalSubjectType.setSubjectType(subjectType);
        operationalSubjectTypeRepository.save(operationalSubjectType);
    }

    @Override
    public boolean isNonScopeEntityChanged(DateTime lastModifiedDateTime) {
        return subjectTypeRepository.existsByLastModifiedDateTimeGreaterThan(lastModifiedDateTime);
    }

    public JsonObject getSyncAttributeData() {
        List<ConceptSyncAttributeContract> syncRegistrationConcept1List = new ArrayList<>();
        List<ConceptSyncAttributeContract> syncRegistrationConcept2List = new ArrayList<>();
        List<SubjectType> subjectTypes = subjectTypeRepository.findAllByIsVoidedFalse();
        subjectTypes.forEach(subjectType -> {
            if (subjectType.getSyncRegistrationConcept1() != null) {
                Concept concept = conceptRepository.findByUuid(subjectType.getSyncRegistrationConcept1());
                syncRegistrationConcept1List.add(ConceptSyncAttributeContract.fromConcept(concept));
            }
            if (subjectType.getSyncRegistrationConcept2() != null) {
                Concept concept = conceptRepository.findByUuid(subjectType.getSyncRegistrationConcept2());
                syncRegistrationConcept2List.add(ConceptSyncAttributeContract.fromConcept(concept));
            }
        });
        boolean isAnySyncByLocation = subjectTypes.stream().anyMatch(SubjectType::isShouldSyncByLocation);
        boolean isAnyDirectlyAssignable = subjectTypes.stream().anyMatch(SubjectType::isDirectlyAssignable);
        return new JsonObject()
                .with("syncConcept1", syncRegistrationConcept1List)
                .with("syncConcept2", syncRegistrationConcept2List)
                .with("isAnySubjectTypeSyncByLocation", isAnySyncByLocation)
                .with("isAnySubjectTypeDirectlyAssignable", isAnyDirectlyAssignable);
    }

    public Stream<SubjectType> getAll() {
        return operationalSubjectTypeRepository.findAllByIsVoidedFalse().stream().map(OperationalSubjectType::getSubjectType);
    }

    public void updateSyncAttributesIfRequired(SubjectType subjectType) {
        Boolean isSyncAttribute1Usable = subjectType.isSyncRegistrationConcept1Usable();
        Boolean isSyncAttribute2Usable = subjectType.isSyncRegistrationConcept2Usable();
        boolean isSyncAttributeChanged = (isSyncAttribute1Usable != null && !isSyncAttribute1Usable) || (isSyncAttribute2Usable != null && !isSyncAttribute2Usable);
        String lastJobStatus = avniJobRepository.getLastJobStatusForSubjectType(subjectType);
        if (isSyncAttributeChanged || (lastJobStatus != null && avniJobRepository.getLastJobStatusForSubjectType(subjectType).equals("FAILED"))) {
            UserContext userContext = UserContextHolder.getUserContext();
            User user = userContext.getUser();
            Organisation organisation = userContext.getOrganisation();
            String jobUUID = UUID.randomUUID().toString();
            JobParameters jobParameters =
                    new JobParametersBuilder()
                            .addString("uuid", jobUUID)
                            .addString("organisationUUID", organisation.getUuid())
                            .addLong("userId", user.getId(), false)
                            .addLong("subjectTypeId", subjectType.getId())
                            .toJobParameters();
            try {
                syncAttributesJobLauncher.run(syncAttributesJob, jobParameters);
            } catch (JobParametersInvalidException | JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException | JobRestartException e) {
                throw new RuntimeException(String.format("Error while starting the sync attribute job, %s", e.getMessage()));
            }
        }
    }
}
