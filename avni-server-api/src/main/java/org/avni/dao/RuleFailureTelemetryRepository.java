package org.avni.dao;

import org.avni.domain.RuleFailureTelemetry;
import org.avni.domain.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleFailureTelemetryRepository extends JpaRepository<RuleFailureTelemetry, Long> {
    Page<RuleFailureTelemetry> findByIsClosed(Boolean isClosed, Pageable pageable);
    default RuleFailureTelemetry findOne(Long id) {
        return findById(id).orElse(null);
    }
}
