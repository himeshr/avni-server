package org.avni.dao;

import org.avni.domain.ProgramOutcome;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@RepositoryRestResource(collectionResourceRel = "programOutcome", path = "programOutcome")
public interface ProgramOutcomeRepository extends TransactionalDataRepository<ProgramOutcome>, FindByLastModifiedDateTime<ProgramOutcome> {
}
