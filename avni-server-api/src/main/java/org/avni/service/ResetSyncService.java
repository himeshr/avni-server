package org.avni.service;

import org.avni.dao.IndividualRepository;
import org.avni.dao.ResetSyncRepository;
import org.avni.dao.UserRepository;
import org.avni.domain.*;
import org.avni.web.request.CatchmentContract;
import org.avni.web.request.UserContract;
import org.avni.web.request.webapp.SubjectTypeContractWeb;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResetSyncService {
    private final ResetSyncRepository resetSyncRepository;
    private final UserRepository userRepository;
    private final IndividualRepository individualRepository;

    @Autowired
    public ResetSyncService(ResetSyncRepository resetSyncRepository, UserRepository userRepository, IndividualRepository individualRepository) {
        this.resetSyncRepository = resetSyncRepository;
        this.userRepository = userRepository;
        this.individualRepository = individualRepository;
    }

    public void recordCatchmentChange(Catchment savedCatchment, CatchmentContract request) {
        List<User> usersAssignedThisCatchment = userRepository.findByCatchmentAndIsVoidedFalse(savedCatchment);
        List<Long> savedLocationIds = savedCatchment.getAddressLevels().stream().map(AddressLevel::getId).collect(Collectors.toList());
        if (!usersAssignedThisCatchment.isEmpty() &&
                isCatchmentChanged(savedLocationIds, request.getLocationIds()) &&
                hasSubjectsInNewLocation(savedLocationIds, request.getLocationIds())) {
            List<ResetSync> resetSyncList = usersAssignedThisCatchment.stream().map(user -> {
                ResetSync resetSync = buildNewResetSync();
                resetSync.setUser(user);
                return resetSync;
            }).collect(Collectors.toList());
            resetSyncRepository.saveAll(resetSyncList);
        }
    }

    public void recordSyncAttributeChange(SubjectType savedSubjectType, SubjectTypeContractWeb request) {
        if (isChanged(savedSubjectType.getSyncRegistrationConcept1(), request.getSyncRegistrationConcept1()) ||
                isChanged(savedSubjectType.getSyncRegistrationConcept2(), request.getSyncRegistrationConcept2()) ||
                isChanged(savedSubjectType.isShouldSyncByLocation(), request.isShouldSyncByLocation())
        ) {
            ResetSync resetSync = buildNewResetSync();
            resetSync.setSubjectType(savedSubjectType);
            resetSyncRepository.save(resetSync);
        }
    }

    public void recordSyncAttributeValueChangeForUser(User savedUser, UserContract userContract) {
        JsonObject newSyncSettings = userContract.getSyncSettings() == null ? new JsonObject() : userContract.getSyncSettings();
        Long savedCatchmentId = savedUser.getCatchmentId().orElse(null);
        if (isSyncSettingsChanged(savedUser.getSyncSettings(), newSyncSettings) || isChanged(savedCatchmentId, userContract.getCatchmentId())) {
            ResetSync resetSync = buildNewResetSync();
            resetSync.setUser(savedUser);
            resetSyncRepository.save(resetSync);
        }
    }

    private boolean isSyncSettingsChanged(JsonObject olderSettings, JsonObject newSettings) {
        return isChanged(olderSettings.getOrDefault(User.SyncSettingKeys.syncConcept1.name(), null), newSettings.getOrDefault(User.SyncSettingKeys.syncConcept1.name(), null)) ||
                isChanged(olderSettings.getOrDefault(User.SyncSettingKeys.syncConcept2.name(), null), newSettings.getOrDefault(User.SyncSettingKeys.syncConcept2.name(), null)) ||
                isConceptValueChanged(olderSettings.getOrDefault(User.SyncSettingKeys.syncConcept1Values.name(), Collections.EMPTY_LIST), newSettings.getOrDefault(User.SyncSettingKeys.syncConcept1Values.name(), Collections.EMPTY_LIST)) ||
                isConceptValueChanged(olderSettings.getOrDefault(User.SyncSettingKeys.syncConcept2Values.name(), Collections.EMPTY_LIST), newSettings.getOrDefault(User.SyncSettingKeys.syncConcept2Values.name(), Collections.EMPTY_LIST));
    }

    private boolean isCatchmentChanged(List<Long> savedLocationIds, List<Long> locationIdsPassedInRequest) {
        return !(savedLocationIds.containsAll(locationIdsPassedInRequest) && locationIdsPassedInRequest.containsAll(savedLocationIds));
    }

    private boolean hasSubjectsInNewLocation(List<Long> savedLocationIds, List<Long> locationIdsPassedInRequest) {
        List<Long> newlyAddedIds = new ArrayList<>(locationIdsPassedInRequest);
        newlyAddedIds.removeAll(savedLocationIds);
        return individualRepository.existsByAddressLevelIdIn(newlyAddedIds.isEmpty() ? locationIdsPassedInRequest : newlyAddedIds);
    }

    private ResetSync buildNewResetSync() {
        ResetSync resetSync = new ResetSync();
        resetSync.assignUUID();
        return resetSync;
    }

    private boolean isConceptValueChanged(Object syncValue1, Object syncValue2) {
        List<String> syncValue1List = (List<String>) syncValue1;
        List<String> syncValue2List = (List<String>) syncValue2;
        return !(syncValue1List.containsAll(syncValue2List) && syncValue2List.containsAll(syncValue1List));
    }

    private boolean isChanged(Object str1, Object str2) {
        return !Objects.equals(str1, str2);
    }

    public Page<ResetSync> getByLastModifiedForUser(DateTime lastModifiedDateTime, DateTime now, User user, Pageable pageable) {
        return resetSyncRepository.findAllByUserIsNullOrUserAndLastModifiedDateTimeBetweenOrderByLastModifiedDateTimeAscIdAsc(user, lastModifiedDateTime.toDate(), now.toDate(), pageable);
    }
}
