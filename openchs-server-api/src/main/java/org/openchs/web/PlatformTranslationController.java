package org.openchs.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openchs.application.Platform;
import org.openchs.dao.PlatformTranslationRepository;
import org.openchs.domain.JsonObject;
import org.openchs.domain.PlatformTranslation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@RepositoryRestController
public class PlatformTranslationController implements RestControllerResourceProcessor<PlatformTranslation> {

    private final PlatformTranslationRepository platformTranslationRepository;
    private final Logger logger;

    public PlatformTranslationController(PlatformTranslationRepository platformTranslationRepository) {
        this.platformTranslationRepository = platformTranslationRepository;
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @RequestMapping(value = "/platformTranslation", method = RequestMethod.POST)
    @Transactional
    @PreAuthorize(value = "hasAnyAuthority('admin','organisation_admin')")
    public ResponseEntity<?> uploadPlatformTranslations(@RequestBody JsonObject translations,
                                                        @RequestParam(value = "platform") String platform) throws Exception {
        PlatformTranslation platformTranslation = platformTranslationRepository.findByPlatform(Platform.valueOf(platform));
        if (platformTranslation == null) {
            platformTranslation = new PlatformTranslation();
        }
        platformTranslation.setTranslationJson(translations);
        platformTranslation.assignUUIDIfRequired();
        platformTranslation.setPlatform(Platform.valueOf(platform));
        platformTranslationRepository.save(platformTranslation);
        logger.info(String.format("Saved Translation with UUID: %s", platformTranslation.getUuid()));
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @RequestMapping(value = "/platformTranslation", method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('admin','organisation_admin')")
    public ResponseEntity<?> downloadPlatformTranslations(@RequestParam(value = "platform") String platform) {
        PlatformTranslation platformTranslation = platformTranslationRepository.findByPlatform(Platform.valueOf(platform));
        if (platformTranslation == null) {
            return ResponseEntity.badRequest().body(String.format("No translation found for %s platform", platform));
        }
        return ResponseEntity.ok().body(platformTranslation.getTranslationJson());
    }
}