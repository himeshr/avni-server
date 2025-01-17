package org.avni.dao;

import java.util.Date;
import org.avni.domain.User;
import org.joda.time.DateTime;
import org.avni.domain.Group;
import org.avni.domain.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

import org.joda.time.DateTime;
import java.util.List;


@Repository
@RepositoryRestResource(collectionResourceRel = "myGroups", path = "myGroups")
@PreAuthorize("hasAnyAuthority('user','admin')")
public interface UserGroupRepository extends ReferenceDataRepository<UserGroup> {

    Page<UserGroup> findByUserIdAndLastModifiedDateTimeIsBetweenOrderByLastModifiedDateTimeAscIdAsc(
            Long userId,
            Date lastModifiedDateTime,
            Date now,
            Pageable pageable);

    default UserGroup findByName(String name) {
        throw new UnsupportedOperationException("No field 'name' in UserGroup.");
    }

    default UserGroup findByNameIgnoreCase(String name) {
        throw new UnsupportedOperationException("No field 'name' in UserGroup.");
    }

    List<UserGroup> findByGroup_IdAndIsVoidedFalse(Long groupId);

    List<UserGroup> findByOrganisationId(Long organisationId);

    Long deleteAllByGroupIsNotIn(List<Group> groups);

    boolean existsByUserIdAndLastModifiedDateTimeGreaterThan(Long userId, Date lastModifiedDateTime);

    List<UserGroup> findByUserAndGroupHasAllPrivilegesTrueAndIsVoidedFalse(User user);

}
