package org.avni.common;

import org.avni.dao.CHSRepository;
import org.avni.domain.CHSEntity;
import org.avni.web.request.CHSRequest;

import java.util.UUID;

public class EntityHelper<T extends CHSEntity> {

    public static <T extends CHSEntity> T newOrExistingEntity(CHSRepository<T> chsRepository, CHSRequest chsRequest, T chsEntity) {
        return newOrExistingEntity(chsRepository, chsRequest.getUuid(), chsEntity);
    }

    public static <T extends CHSEntity> T newOrExistingEntity(CHSRepository<T> chsRepository, String uuid, T chsEntity) {
        T t = chsRepository.findByUuid(uuid);
        if (t == null) {
            t = chsEntity;
            t.setUuid(uuid != null ? uuid : UUID.randomUUID().toString());
        }
        return t;
    }
}
