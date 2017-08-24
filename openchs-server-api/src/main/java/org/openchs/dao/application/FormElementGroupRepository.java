package org.openchs.dao.application;

import org.joda.time.DateTime;
import org.openchs.application.FormElementGroup;
import org.openchs.dao.ReferenceDataRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@RepositoryRestResource(collectionResourceRel = "formElementGroup", path = "formElementGroup")
public interface FormElementGroupRepository extends PagingAndSortingRepository<FormElementGroup, Long>, ReferenceDataRepository<FormElementGroup> {
    @RestResource(path = "lastModified", rel = "lastModified")
    Page<FormElementGroup> findByLastModifiedDateTimeGreaterThanOrderByLastModifiedDateTimeAscIdAsc(@Param("lastModifiedDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime lastModifiedDateTime, Pageable pageable);
}