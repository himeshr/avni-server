package org.avni.web;

import org.avni.dao.AddressLevelTypeRepository;
import org.avni.dao.LocationRepository;
import org.avni.domain.AddressLevel;
import org.avni.domain.AddressLevelType;
import org.avni.service.LocationService;
import org.avni.util.ReactAdminUtil;
import org.avni.web.request.AddressLevelTypeContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class AddressLevelTypeController extends AbstractController<AddressLevelType> {

    private final AddressLevelTypeRepository addressLevelTypeRepository;
    private final LocationRepository locationRepository;
    private Logger logger;
    private LocationService locationService;
    private final ProjectionFactory projectionFactory;

    @Autowired
    public AddressLevelTypeController(AddressLevelTypeRepository addressLevelTypeRepository, LocationRepository locationRepository, LocationService locationService, ProjectionFactory projectionFactory) {
        this.addressLevelTypeRepository = addressLevelTypeRepository;
        this.locationRepository = locationRepository;
        this.locationService = locationService;
        this.projectionFactory = projectionFactory;
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @GetMapping(value = "/addressLevelType")
    @PreAuthorize(value = "hasAnyAuthority('admin', 'organisation_admin')")
    @ResponseBody
    public Page<AddressLevelType> getAllNonVoidedAddressLevelType(Pageable pageable) {
        return addressLevelTypeRepository.findPageByIsVoidedFalse(pageable);
    }

    @GetMapping(value = "/web/addressLevelType")
    @PreAuthorize(value = "hasAnyAuthority('admin', 'organisation_admin')")
    @ResponseBody
    public List<AddressLevelType.AddressLevelTypeProjection> findAll() {
        return addressLevelTypeRepository.findAllByIsVoidedFalse()
                .stream()
                .map(t -> projectionFactory.createProjection(AddressLevelType.AddressLevelTypeProjection.class, t))
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/addressLevelType/{id}")
    public ResponseEntity<?> getSingle(@PathVariable Long id) {
        return new ResponseEntity<>(addressLevelTypeRepository.findOne(id), HttpStatus.OK);
    }

    @PostMapping(value = "/addressLevelType")
    @PreAuthorize(value = "hasAnyAuthority('admin', 'organisation_admin')")
    @Transactional
    public ResponseEntity<?> createAddressLevelType(@RequestBody AddressLevelTypeContract contract) {
        //Do not allow to create location type when there is already another location type with the same name
        if (addressLevelTypeRepository.findByName(contract.getName()) != null)
            return ResponseEntity.badRequest().body(ReactAdminUtil.generateJsonError(String.format("Location Type with name %s already exists", contract.getName())));

        AddressLevelType addressLevelType = locationService.createAddressLevelType(contract);
        return new ResponseEntity<>(addressLevelType, HttpStatus.CREATED);
    }

    @PostMapping(value = "/addressLevelTypes")
    @PreAuthorize(value = "hasAnyAuthority('admin', 'organisation_admin')")
    @Transactional
    public ResponseEntity<?> save(@RequestBody List<AddressLevelTypeContract> addressLevelTypeContracts) {
        for (AddressLevelTypeContract addressLevelTypeContract : addressLevelTypeContracts) {
            logger.info(String.format("Processing addressLevelType request: %s", addressLevelTypeContract.getUuid()));
            locationService.createAddressLevelType(addressLevelTypeContract);
        }
        return ResponseEntity.ok(null);
    }

    @PutMapping(value = "/addressLevelType/{id}")
    @PreAuthorize(value = "hasAnyAuthority('admin', 'organisation_admin')")
    @Transactional
    public ResponseEntity<?> updateAddressLevelType(@PathVariable("id") Long id, @RequestBody AddressLevelTypeContract contract) {
        AddressLevelType addressLevelType = addressLevelTypeRepository.findByUuid(contract.getUuid());
        AddressLevelType addressLevelTypeWithSameName = addressLevelTypeRepository.findByName(contract.getName());
        //Do not allow to change location type name when there is already another location type with the same name
        if (addressLevelTypeWithSameName != null && addressLevelTypeWithSameName.getUuid() != addressLevelType.getUuid())
            return ResponseEntity.badRequest().body(ReactAdminUtil.generateJsonError(String.format("Location Type with name %s already exists", contract.getName())));

        addressLevelType.setName(contract.getName());
        addressLevelType.setLevel(contract.getLevel());
        Set<AddressLevel> addressLevels = addressLevelType.getAddressLevels();
        addressLevels.forEach(addressLevel -> addressLevel.updateAudit());
        addressLevelTypeRepository.save(addressLevelType);
        return new ResponseEntity<>(addressLevelType, HttpStatus.CREATED);
    }

    @DeleteMapping(value = "/addressLevelType/{id}")
    @PreAuthorize(value = "hasAnyAuthority('admin', 'organisation_admin')")
    @Transactional
    public ResponseEntity<?> voidAddressLevelType(@PathVariable("id") Long id) {
        AddressLevelType addressLevelType = addressLevelTypeRepository.findOne(id);
        if (addressLevelType == null) {
            return ResponseEntity.badRequest().body(ReactAdminUtil.generateJsonError(String.format("AddressLevelType with id %d not found", id)));
        }
        if (!addressLevelType.isVoidable()) {
            return ResponseEntity.badRequest().body(ReactAdminUtil.generateJsonError(
                    String.format("Cannot delete Type '%s' until all SubTypes are deleted or there are non-voided addresses depending on it", addressLevelType.getName())));
        }
        addressLevelType.setVoided(true);
        return new ResponseEntity<>(addressLevelType, HttpStatus.OK);
    }
}
