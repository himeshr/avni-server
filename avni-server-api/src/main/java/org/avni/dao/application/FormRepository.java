package org.avni.dao.application;

import org.avni.application.Form;
import org.avni.application.FormType;
import org.avni.dao.FindByLastModifiedDateTime;
import org.avni.dao.ReferenceDataRepository;
import org.avni.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(collectionResourceRel = "form", path = "form")
public interface FormRepository extends ReferenceDataRepository<Form>, FindByLastModifiedDateTime<Form>, JpaSpecificationExecutor<Form> {

    @RestResource(exported = false)
    Page<Form> findByOrganisationIdAndNameIgnoreCaseContaining(Long organisationId, String name, Pageable pageable);

    @Query("select f.name from Form f where f.isVoided = false")
    List<String> getAllNames();

    List<Form> findByFormTypeAndIsVoidedFalse(FormType formType);
}
