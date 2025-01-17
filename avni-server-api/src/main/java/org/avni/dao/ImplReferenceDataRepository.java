package org.avni.dao;

import org.avni.domain.CHSEntity;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.security.access.prepost.PreAuthorize;

@NoRepositoryBean
@PreAuthorize(value = "hasAnyAuthority('user')")
public interface ImplReferenceDataRepository<T extends CHSEntity> extends ReferenceDataRepository<T> {

    @PreAuthorize("hasAnyAuthority('organisation_admin')")
    <S extends T> S save(S concept);
}
