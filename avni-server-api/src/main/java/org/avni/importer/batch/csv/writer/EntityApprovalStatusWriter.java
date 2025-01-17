package org.avni.importer.batch.csv.writer;

import org.avni.application.FormMapping;
import org.avni.domain.ApprovalStatus;
import org.avni.domain.EntityApprovalStatus;
import org.avni.service.EntityApprovalStatusService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class EntityApprovalStatusWriter {

    private final EntityApprovalStatusService entityApprovalStatusService;

    @Value("#{jobParameters['autoApprove']}")
    private String autoApprove;

    @Autowired
    public EntityApprovalStatusWriter(EntityApprovalStatusService entityApprovalStatusService) {
        this.entityApprovalStatusService = entityApprovalStatusService;
    }

    public void saveStatus(FormMapping formMapping, Long entityId, EntityApprovalStatus.EntityType entityType) {
        boolean isAutoApprove = Boolean.parseBoolean(autoApprove);
        if (formMapping.isEnableApproval()) {
            ApprovalStatus.Status status = isAutoApprove ? ApprovalStatus.Status.Approved : ApprovalStatus.Status.Pending;
            entityApprovalStatusService.createStatus(entityType, entityId, status);
        }
    }


}
