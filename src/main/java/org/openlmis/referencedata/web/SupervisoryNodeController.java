/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.referencedata.web;

import static java.util.stream.Collectors.toSet;
import static org.openlmis.referencedata.domain.RightName.SUPERVISORY_NODES_MANAGE;

import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.dto.SupervisoryNodeDto;
import org.openlmis.referencedata.dto.UserDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.repository.RightRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.repository.UserRepository;
import org.openlmis.referencedata.service.RequisitionGroupProgramScheduleService;
import org.openlmis.referencedata.service.RightService;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.openlmis.referencedata.util.messagekeys.RightMessageKeys;
import org.openlmis.referencedata.util.messagekeys.SupervisoryNodeMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
@Transactional
public class SupervisoryNodeController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SupervisoryNodeController.class);

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Autowired
  private RequisitionGroupProgramScheduleService requisitionGroupProgramScheduleService;

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private ProgramRepository programRepository;
  
  @Autowired
  private RightRepository rightRepository;
  
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RightService rightService;

  /**
   * Allows creating new supervisoryNode. If the id is specified, it will be ignored.
   *
   * @param supervisoryNodeDto A supervisoryNodeDto bound to the request body.
   * @return the created supervisoryNode.
   */
  @RequestMapping(value = "/supervisoryNodes", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public SupervisoryNodeDto createSupervisoryNode(
      @RequestBody SupervisoryNodeDto supervisoryNodeDto) {
    rightService.checkAdminRight(SUPERVISORY_NODES_MANAGE);

    LOGGER.debug("Creating new supervisoryNode");
    supervisoryNodeDto.setId(null);
    SupervisoryNode supervisoryNode = SupervisoryNode.newSupervisoryNode(supervisoryNodeDto);
    supervisoryNodeRepository.save(supervisoryNode);
    LOGGER.debug("Created new supervisoryNode with id: " + supervisoryNode.getId());
    return exportToDto(supervisoryNode);
  }

  /**
   * Get all supervisoryNodes.
   *
   * @return the SupervisoryNodeDtos.
   */
  @RequestMapping(value = "/supervisoryNodes", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<SupervisoryNodeDto> getAllSupervisoryNodes() {
    rightService.checkAdminRight(SUPERVISORY_NODES_MANAGE);

    Iterable<SupervisoryNode> supervisoryNodes = supervisoryNodeRepository.findAll();
    List<SupervisoryNodeDto> supervisoryNodeDtos = new ArrayList<>();

    for (SupervisoryNode supervisoryNode : supervisoryNodes) {
      supervisoryNodeDtos.add(exportToDto(supervisoryNode));
    }

    return supervisoryNodeDtos;
  }

  /**
   * Get chosen supervisoryNode.
   *
   * @param supervisoryNodeId UUID of the supervisoryNode whose we want to get.
   * @return the SupervisoryNode.
   */
  @RequestMapping(value = "/supervisoryNodes/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public SupervisoryNodeDto getSupervisoryNode(
      @PathVariable("id") UUID supervisoryNodeId) {
    rightService.checkAdminRight(SUPERVISORY_NODES_MANAGE);

    SupervisoryNode supervisoryNode = supervisoryNodeRepository.findOne(supervisoryNodeId);
    if (supervisoryNode == null) {
      throw new NotFoundException(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);
    } else {
      return exportToDto(supervisoryNode);
    }
  }

  /**
   * Allows updating supervisoryNode.
   *
   * @param supervisoryNodeDto A supervisoryNodeDto bound to the request body.
   * @param supervisoryNodeId UUID of the supervisoryNode which we want to update.
   * @return the updated supervisoryNode.
   */
  @RequestMapping(value = "/supervisoryNodes/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public SupervisoryNodeDto updateSupervisoryNode(
      @RequestBody SupervisoryNodeDto supervisoryNodeDto,
      @PathVariable("id") UUID supervisoryNodeId) {
    rightService.checkAdminRight(SUPERVISORY_NODES_MANAGE);
    LOGGER.debug("Updating supervisoryNode with id: " + supervisoryNodeId);

    SupervisoryNode supervisoryNodeToUpdate =
        supervisoryNodeRepository.findOne(supervisoryNodeId);

    if (supervisoryNodeToUpdate == null) {
      supervisoryNodeToUpdate = new SupervisoryNode();
    }

    supervisoryNodeToUpdate.updateFrom(SupervisoryNode.newSupervisoryNode(supervisoryNodeDto));
    supervisoryNodeRepository.save(supervisoryNodeToUpdate);

    LOGGER.debug("Updated supervisoryNode with id: " + supervisoryNodeId);
    return exportToDto(supervisoryNodeToUpdate);
  }

  /**
   * Allows deleting supervisoryNode.
   *
   * @param supervisoryNodeId UUID of supervisoryNode whose we want to delete.
   */
  @RequestMapping(value = "/supervisoryNodes/{id}", method = RequestMethod.DELETE)
  public ResponseEntity deleteSupervisoryNode(@PathVariable("id") UUID supervisoryNodeId) {
    rightService.checkAdminRight(SUPERVISORY_NODES_MANAGE);

    SupervisoryNode supervisoryNode = supervisoryNodeRepository.findOne(supervisoryNodeId);
    if (supervisoryNode == null) {
      throw new NotFoundException(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);
    } else {
      supervisoryNodeRepository.delete(supervisoryNode);
      return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
  }

  /**
   * Find supervising users by right and program.
   *
   * @param rightId UUID of right that user has.
   * @param programId UUID of program.
   * @return the found users.
   */
  @RequestMapping(value = "/supervisoryNodes/{id}/supervisingUsers", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Set<UserDto> findSupervisingUsers(
      @PathVariable("id") UUID supervisoryNodeId,
      @RequestParam("rightId") UUID rightId,
      @RequestParam("programId") UUID programId) {
    rightService.checkAdminRight(RightName.USERS_MANAGE_RIGHT);

    SupervisoryNode supervisoryNode = supervisoryNodeRepository.findOne(supervisoryNodeId);
    Right right = rightRepository.findOne(rightId);
    Program program = programRepository.findOne(programId);

    if (supervisoryNode == null) {
      throw new NotFoundException(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);
    }

    if (right == null) {
      throw new ValidationMessageException(RightMessageKeys.ERROR_NOT_FOUND);
    }

    if (program == null) {
      throw new ValidationMessageException(ProgramMessageKeys.ERROR_NOT_FOUND);
    }
    
    Set<User> supervisingUsers = userRepository.findSupervisingUsersBy(right, supervisoryNode,
        program);

    return supervisingUsers.stream().map(this::exportToDto).collect(toSet());
  }

  /**
   * Searching for supervisoryNode with given parameters.
   *
   * @param programId UUID of RequisitionGroup program by which we want to search.
   * @param facilityId UUID of RequisitionGroup facility by which we want to search.
   * @return the found supervisoryNode.
   */
  @RequestMapping(value = "/supervisoryNodes/search", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<SupervisoryNodeDto> findByRequisitionGroupFacilityAndProgram(
      @RequestParam("programId") UUID programId, @RequestParam("facilityId") UUID facilityId) {
    rightService.checkAdminRight(SUPERVISORY_NODES_MANAGE);

    Facility facility = facilityRepository.findOne(facilityId);
    Program program = programRepository.findOne(programId);

    if (program == null) {
      throw new ValidationMessageException(ProgramMessageKeys.ERROR_NOT_FOUND);
    }

    if (facility == null) {
      throw new ValidationMessageException(FacilityMessageKeys.ERROR_NOT_FOUND);
    }

    RequisitionGroupProgramSchedule foundGroup = requisitionGroupProgramScheduleService
            .searchRequisitionGroupProgramSchedule(program, facility);

    if (foundGroup == null) {
      throw new NotFoundException(new Message(
          SupervisoryNodeMessageKeys.ERROR_NOT_FOUND_WITH_PROGRAM_AND_FACILITY,
          programId, facilityId));
    }

    SupervisoryNode result = foundGroup.getRequisitionGroup().getSupervisoryNode();
    return Collections.singletonList(exportToDto(result));
  }

  private SupervisoryNodeDto exportToDto(SupervisoryNode supervisoryNode) {
    SupervisoryNodeDto supervisoryNodeDto = null;

    if (supervisoryNode != null) {
      supervisoryNodeDto = new SupervisoryNodeDto();
      supervisoryNode.export(supervisoryNodeDto);
    }

    return supervisoryNodeDto;
  }

  private UserDto exportToDto(User user) {
    UserDto userDto = null;

    if (user != null) {
      userDto = new UserDto();
      user.export(userDto);
    }

    return userDto;
  }
}
