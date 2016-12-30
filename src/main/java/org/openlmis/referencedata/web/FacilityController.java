package org.openlmis.referencedata.web;

import org.javers.core.diff.Change;
import org.javers.repository.jql.QueryBuilder;
import org.openlmis.referencedata.domain.*;
import org.openlmis.referencedata.dto.ApprovedProductDto;
import org.openlmis.referencedata.dto.FacilityDto;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.FacilityTypeApprovedProductRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.service.SupplyLineService;
import org.openlmis.referencedata.util.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
public class FacilityController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(FacilityController.class);

  private static final String KEY_ERROR_PROGRAM_NOT_FOUND = "referencedata.error.program.not-found";
  private static final String KEY_ERROR_FACILITY_NOT_FOUND =
      "referencedata.error.facility.not-found";

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private FacilityTypeApprovedProductRepository facilityTypeApprovedProductRepository;

  @Autowired
  private ProgramRepository programRepository;

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Autowired
  private SupplyLineService supplyLineService;



  //TEMPORARY TEST CODE
  @RequestMapping(value = "/message", method = RequestMethod.GET)
  public ResponseEntity<?> getMessage()
  {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    LocalDateTime now = LocalDateTime.now();
    return ResponseEntity.status(HttpStatus.OK).body("hello world, at " + dateTimeFormatter.format(now));
  }

  //TEMPORARY TEST CODE
  @RequestMapping(value = "/updateFacility", method = RequestMethod.GET)
  public ResponseEntity<?> updateFacility()
  {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    LocalDateTime now = LocalDateTime.now();
    Facility facility = facilityRepository.findFirstByCode("FAC004");

    String comment = "facility id " + facility.getId() + " last updated on " + dateTimeFormatter.format(now) + ".";
    facility.setComment(comment);
    facilityRepository.save(facility);
    return ResponseEntity.status(HttpStatus.OK).body(comment);
  }

  //TEMPORARY TEST CODE
  @RequestMapping(value = "/userInfo", method = RequestMethod.GET)
  public ResponseEntity<?> getUserInfo()
  {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String returnVal = "";

    try
    {
      User user = (User)auth.getPrincipal();
      returnVal = user.getUsername();
    }
    catch (Exception e)
    {
      returnVal = "unknown user";
    }

    return ResponseEntity.status(HttpStatus.OK).body("(v3) " + returnVal);
  }


  /**
   * Allows creating new facilities. If the id is specified, it will be ignored.
   *
   * @param facilityDto A facility bound to the request body
   * @return ResponseEntity containing the created facility
   */
  @RequestMapping(value = "/facilities", method = RequestMethod.POST)
  public ResponseEntity<?> createFacility(@RequestBody FacilityDto facilityDto) {
    LOGGER.debug("Creating new facility");
    facilityDto.setId(null);
    Facility newFacility = Facility.newFacility(facilityDto);

    boolean addSuccessful = addSupportedProgramsToFacility(facilityDto.getSupportedPrograms(),
        newFacility);
    if (!addSuccessful) {
      return ResponseEntity
          .badRequest()
          .body(buildErrorResponse(KEY_ERROR_PROGRAM_NOT_FOUND));
    }

    newFacility = facilityRepository.save(newFacility);
    LOGGER.debug("Created new facility with id: " + facilityDto.getId());
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(toDto(newFacility));
  }

  /**
   * Get all facilities.
   *
   * @return Facilities.
   */
  @RequestMapping(value = "/facilities", method = RequestMethod.GET)
  public ResponseEntity<?> getAllFacilities() {
    Iterable<Facility> facilities = facilityRepository.findAll();
    return ok(facilities);
  }

  /**
   * Get the audit log for all facilities.
   *
   * @return Facilities.
   */
  @RequestMapping(value = "/facilities/audit", method = RequestMethod.GET)
  public ResponseEntity<?> getAllFacilitiesAudit() {
    return ResponseEntity.status(HttpStatus.OK).body( getChangesByClass(Facility.class) );
  }


  /**
   * Allows updating facilities.
   *
   * @param facilityDto A facility bound to the request body
   * @param facilityId  UUID of facility which we want to update
   * @return ResponseEntity containing the updated facility
   */
  @RequestMapping(value = "/facilities/{id}", method = RequestMethod.PUT)
  public ResponseEntity<?> saveFacility(@RequestBody FacilityDto facilityDto,
                                        @PathVariable("id") UUID facilityId) {

    Facility facilityToSave = Facility.newFacility(facilityDto);
    facilityToSave.setId(facilityId);

    boolean addSuccessful = addSupportedProgramsToFacility(facilityDto.getSupportedPrograms(),
        facilityToSave);
    if (!addSuccessful) {
      return ResponseEntity
          .badRequest()
          .body(buildErrorResponse(KEY_ERROR_PROGRAM_NOT_FOUND));
    }
    facilityToSave = facilityRepository.save(facilityToSave);

    LOGGER.debug("Saved facility with id: " + facilityToSave.getId());
    return ok(facilityToSave);
  }

  /**
   * Get chosen facility.
   *
   * @param facilityId UUID of facility which we want to get
   * @return Facility.
   */
  @RequestMapping(value = "/facilities/{id}", method = RequestMethod.GET)
  public ResponseEntity<?> getFacility(@PathVariable("id") UUID facilityId) {
    Facility facility = facilityRepository.findOne(facilityId);
    if (facility == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } else {
      return ok(facility);
    }
  }

  /**
   * Returns full or non-full supply approved products for the given facility.
   *
   * @param facilityId ID of the facility
   * @param programId  ID of the program
   * @param fullSupply true to retrieve full-supply products, false to retrieve non-full supply
   *                   products
   * @return collection of approved products
   */
  @RequestMapping(value = "/facilities/{id}/approvedProducts")
  public ResponseEntity<?> getApprovedProducts(@PathVariable("id") UUID facilityId,
                                               @RequestParam(required = false, value = "programId")
                                                   UUID programId,
                                               @RequestParam(value = "fullSupply")
                                                   boolean fullSupply) {

    Facility facility = facilityRepository.findOne(facilityId);
    if (facility == null) {
      return ResponseEntity.badRequest().body(buildErrorResponse(KEY_ERROR_FACILITY_NOT_FOUND));
    }

    Collection<FacilityTypeApprovedProduct> products = facilityTypeApprovedProductRepository
        .searchProducts(facilityId, programId, fullSupply);

    return ResponseEntity.ok(toDto(products));
  }

  /**
   * Allows deleting facility.
   *
   * @param facilityId UUID of facility which we want to delete
   * @return ResponseEntity containing the HTTP Status
   */
  @RequestMapping(value = "/facilities/{id}", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteFacility(@PathVariable("id") UUID facilityId) {
    Facility facility = facilityRepository.findOne(facilityId);
    if (facility == null) {
      return new ResponseEntity(HttpStatus.NOT_FOUND);
    } else {
      facilityRepository.delete(facility);
      return new ResponseEntity<Facility>(HttpStatus.NO_CONTENT);
    }
  }

  /**
   * Retrieves all available supplying facilities for program and supervisory node.
   *
   * @param programId         program to filter facilities
   * @param supervisoryNodeId supervisoryNode to filter facilities
   * @return ResponseEntity containing matched facilities
   */
  @RequestMapping(value = "/facilities/supplying", method = RequestMethod.GET)
  public ResponseEntity<?> getSupplyingDepots(
      @RequestParam(value = "programId") UUID programId,
      @RequestParam(value = "supervisoryNodeId") UUID supervisoryNodeId) {
    Program program = programRepository.findOne(programId);
    SupervisoryNode supervisoryNode = supervisoryNodeRepository.findOne(supervisoryNodeId);

    if (program == null) {
      final String errorMessage = "Given Program does not exist";
      final String errorDescription = "programId: " + programId;

      ErrorResponse errorResponse = new ErrorResponse(errorMessage, errorDescription);
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    if (supervisoryNode == null) {
      final String errorMessage = "Given SupervisorNode does not exist";
      final String errorDescription = "supervisorNodeId: " + supervisoryNodeId;

      ErrorResponse errorResponse = new ErrorResponse(errorMessage, errorDescription);
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    List<SupplyLine> supplyLines = supplyLineService.searchSupplyLines(program,
        supervisoryNode);
    List<Facility> facilities = supplyLines.stream()
        .map(SupplyLine::getSupplyingFacility).distinct().collect(Collectors.toList());
    return ok(facilities);
  }

  /**
   * Retrieves all Facilities with facilitCode similar to code parameter or facilityName similar to
   * name parameter.
   *
   * @param code Part of wanted facility code.
   * @param name Part of wanted facility name.
   * @return List of wanted Facilities.
   */
  @RequestMapping(value = "/facilities/search",
      method = RequestMethod.GET)
  public ResponseEntity<?> findFacilitiesWithSimilarCodeOrName(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "name", required = false) String name) {
    if (code == null && name == null) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    List<Facility> foundFacilities =
        facilityRepository.findFacilitiesByCodeOrName(code, name);
    return ok(foundFacilities);
  }

  private ResponseEntity<FacilityDto> ok(Facility facility) {
    return new ResponseEntity<>(toDto(facility), HttpStatus.OK);
  }

  private ResponseEntity<List<FacilityDto>> ok(Iterable<Facility> facilities) {
    return new ResponseEntity<>(toDto(facilities), HttpStatus.OK);
  }

  private FacilityDto toDto(Facility facility) {
    FacilityDto dto = new FacilityDto();
    facility.export(dto);

    return dto;
  }

  private List<FacilityDto> toDto(Iterable<Facility> facilities) {
    return StreamSupport
        .stream(facilities.spliterator(), false)
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  private List<ApprovedProductDto> toDto(Collection<FacilityTypeApprovedProduct> products) {
    List<ApprovedProductDto> productDtos = new ArrayList<>();
    for (FacilityTypeApprovedProduct product : products) {
      ApprovedProductDto productDto = new ApprovedProductDto();
      product.export(productDto);
      productDtos.add(productDto);
    }

    return productDtos;
  }

  private boolean addSupportedProgramsToFacility(Set<SupportedProgramDto> supportedProgramDtos,
                                                 Facility facility) {
    for (SupportedProgramDto supportedProgramDto : supportedProgramDtos) {
      Program program = programRepository.findByCode(Code.code(supportedProgramDto.getCode()));
      if (program == null) {
        LOGGER.debug("Program does not exist: " + supportedProgramDto.getCode());
        return false;
      }
      SupportedProgram supportedProgram = SupportedProgram.newSupportedProgram(facility,
          program, supportedProgramDto.isSupportActive(), supportedProgramDto.getZonedStartDate());
      facility.addSupportedProgram(supportedProgram);
    }

    return true;
  }
}
