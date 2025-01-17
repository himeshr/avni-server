package org.avni.importer.batch.sync.attributes;


import org.avni.dao.SubjectTypeRepository;
import org.avni.domain.SubjectType;
import org.avni.framework.security.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@JobScope
public class SyncAttributesJobListener extends JobExecutionListenerSupport {
    private Logger logger;
    private SubjectTypeRepository subjectTypeRepository;
    private AuthService authService;

    @Value("#{jobParameters['uuid']}")
    private String uuid;

    @Value("#{jobParameters['subjectTypeId']}")
    private Long subjectTypeId;

    @Value("#{jobParameters['organisationUUID']}")
    private String organisationUUID;

    @Value("#{jobParameters['userId']}")
    private Long userId;

    @Autowired
    public SyncAttributesJobListener(SubjectTypeRepository subjectTypeRepository, AuthService authService) {
        this.subjectTypeRepository = subjectTypeRepository;
        this.authService = authService;
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            SubjectType subjectType = subjectTypeRepository.findOne(subjectTypeId);
            if (subjectType.getSyncRegistrationConcept1() != null) {
                subjectType.setSyncRegistrationConcept1Usable(true);
            }
            if (subjectType.getSyncRegistrationConcept2() != null) {
                subjectType.setSyncRegistrationConcept2Usable(true);
            }
            subjectTypeRepository.save(subjectType);
            logger.info("Sync attribute Job with uuid {} finished", jobExecution.getJobParameters().getString("uuid"));
        } else {
            logger.info("Sync attribute job finished with status {}", jobExecution.getStatus());
            for (Throwable t : jobExecution.getAllFailureExceptions()) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Starting sync attribute job with uuid {}", jobExecution.getJobParameters().getString("uuid"));
        authService.authenticateByUserId(userId, organisationUUID);
    }
}
