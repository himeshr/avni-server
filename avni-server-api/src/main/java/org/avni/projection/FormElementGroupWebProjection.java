package org.avni.projection;

import org.avni.application.FormElementGroup;
import org.springframework.data.rest.core.config.Projection;

import java.util.Set;

@Projection(name = "FormElementGroupWebProjection", types = {FormElementGroup.class})
public interface FormElementGroupWebProjection {
    String getUuid();
    boolean isVoided();
    String getName();
    Double getDisplayOrder();
    String getRule();
    Set<FormElementWebProjection> getApplicableFormElements();
}
