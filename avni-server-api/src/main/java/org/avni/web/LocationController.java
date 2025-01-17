package org.avni.web;


import org.avni.application.projections.LocationProjection;
import org.avni.builder.BuilderException;
import org.avni.dao.LocationRepository;
import org.avni.dao.SyncParameters;
import org.avni.domain.AddressLevel;
import org.avni.service.LocationService;
import org.avni.service.ScopeBasedSyncService;
import org.avni.service.UserService;
import org.avni.util.ReactAdminUtil;
import org.avni.web.request.AddressLevelContractWeb;
import org.avni.web.request.LocationContract;
import org.avni.web.request.LocationEditContract;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RepositoryRestController
public class LocationController implements RestControllerResourceProcessor<AddressLevel> {

    private LocationRepository locationRepository;
    private Logger logger;
    private UserService userService;
    private LocationService locationService;
    private ScopeBasedSyncService<AddressLevel> scopeBasedSyncService;

    @Autowired
    public LocationController(LocationRepository locationRepository, UserService userService, LocationService locationService, ScopeBasedSyncService<AddressLevel> scopeBasedSyncService) {
        this.locationRepository = locationRepository;
        this.userService = userService;
        this.locationService = locationService;
        this.scopeBasedSyncService = scopeBasedSyncService;
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @RequestMapping(value = "/locations", method = RequestMethod.POST)
    @PreAuthorize(value = "hasAnyAuthority('admin','organisation_admin')")
    @Transactional
    public ResponseEntity<?> save(@RequestBody List<LocationContract> locationContracts) {
        try {
            List<AddressLevel> list = locationService.saveAll(locationContracts);
            if (list.size() == 1) {
                return new ResponseEntity<>(list.get(0), HttpStatus.CREATED);
            }
        } catch (BuilderException e) {
            logger.error(e.getMessage());
            return ResponseEntity.badRequest().body(ReactAdminUtil.generateJsonError(e.getMessage()));
        }
        return ResponseEntity.ok(null);
    }

    @GetMapping(value = "/locations")
    @PreAuthorize(value = "hasAnyAuthority('admin','organisation_admin')")
    @ResponseBody
    public Page<LocationProjection> getAll(Pageable pageable) {
        return locationRepository.findNonVoidedLocations(pageable);
    }

    @GetMapping(value = "locations/search/find")
    @PreAuthorize(value = "hasAnyAuthority('admin', 'user')")
    @ResponseBody
    public Page<LocationProjection> find(
            @RequestParam(value = "title") String title,
            Pageable pageable) {
        return locationRepository.findByIsVoidedFalseAndTitleIgnoreCaseStartingWithOrderByTitleAsc(title, pageable);
    }

    @GetMapping(value = "/locations/search/findAllById")
    @PreAuthorize(value = "hasAnyAuthority('admin','organisation_admin')")
    @ResponseBody
    public List<LocationProjection> findByIdIn(@Param("ids") Long[] ids) {
        if(ids == null || ids.length == 0) {
            return new ArrayList<>();
        }
        return locationRepository.findByIdIn(ids);
    }

    @RequestMapping(value = {"/locations/search/lastModified", "/locations/search/byCatchmentAndLastModified"}, method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('user','admin')")
    @ResponseBody
    public PagedResources<Resource<AddressLevel>> getAddressLevelsByOperatingIndividualScope(
            @RequestParam("lastModifiedDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime lastModifiedDateTime,
            @RequestParam("now") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime now,
            Pageable pageable) {
        return wrap(scopeBasedSyncService.getSyncResult(locationRepository, userService.getCurrentUser(), lastModifiedDateTime, now, null, pageable, null, SyncParameters.SyncEntityName.Location));
    }

    @PutMapping(value = "/locations/{id}")
    @PreAuthorize(value = "hasAnyAuthority('admin', 'organisation_admin')")
    @Transactional
    public ResponseEntity updateLocation(@RequestBody LocationEditContract locationEditContract,
                                         @PathVariable("id") Long id) {
        logger.info(String.format("Processing location update request: %s", locationEditContract.toString()));
        AddressLevel location;
        try {
            location = locationService.update(locationEditContract, id);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.badRequest().body(ReactAdminUtil.generateJsonError(e.getMessage()));
        }
        return new ResponseEntity<>(location, HttpStatus.OK);
    }

    @DeleteMapping(value = "/locations/{id}")
    @PreAuthorize(value = "hasAnyAuthority('admin', 'organisation_admin')")
    @Transactional
    public ResponseEntity voidLocation(@PathVariable("id") Long id) {
        AddressLevel location = locationRepository.findOne(id);
        if (location == null)
            return ResponseEntity.badRequest().body(String.format("Location with id '%d' not found", id));

        if (location.getNonVoidedSubLocations().size() > 0)
            return ResponseEntity.badRequest().body(ReactAdminUtil.generateJsonError(
                    String.format("Cannot delete location '%s' until all sub locations are deleted", location.getTitle()))
            );

        location.setTitle(String.format("%s (voided~%d)", location.getTitle(), location.getId()));
        location.setVoided(true);
        location.updateAudit();
        locationRepository.save(location);

        return ResponseEntity.ok(null);
    }

    @GetMapping(value = "/locations/web/getAll")
    @PreAuthorize(value = "hasAnyAuthority('admin','user')")
    @ResponseBody
    public List<AddressLevelContractWeb> getAllLocations() {
        return locationRepository.findAllNonVoided().stream()
                .map(AddressLevelContractWeb::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/locations/search/typeId/{typeId}")
    @PreAuthorize(value = "hasAnyAuthority('user', 'admin')")
    @ResponseBody
    public List<AddressLevelContractWeb> getLocationsByTypeId(@PathVariable("typeId") Long typeId) {
        return locationRepository.findNonVoidedLocationsByTypeId(typeId).stream()
                .map(AddressLevelContractWeb::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/locations/web")
    @PreAuthorize(value = "hasAnyAuthority('user')")
    @ResponseBody
    public ResponseEntity getLocationByParam(@RequestParam("uuid") String uuid) {
        LocationProjection addressLevel = locationRepository.findNonVoidedLocationsByUuid(uuid);
        if(addressLevel == null) {
            return ResponseEntity.notFound().build();
        }
        return new ResponseEntity<>(AddressLevelContractWeb.fromEntity(addressLevel), HttpStatus.OK);
    }
}
