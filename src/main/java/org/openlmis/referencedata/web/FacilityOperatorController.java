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

import org.openlmis.referencedata.domain.FacilityOperator;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.repository.FacilityOperatorRepository;
import org.openlmis.referencedata.util.messagekeys.FacilityOperatorMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@Controller
@Transactional
public class FacilityOperatorController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(FacilityOperatorController.class);

  @Autowired
  private FacilityOperatorRepository facilityOperatorRepository;

  /**
   * Allows creating new facilityOperators.
   *
   * @param facilityOperator A facilityOperator bound to the request body.
   * @return the created facilityOperator.
   */
  @RequestMapping(value = "/facilityOperators", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public FacilityOperator createFacilityOperator(
      @RequestBody FacilityOperator facilityOperator) {
    rightService.checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT);
    LOGGER.debug("Creating new facility operator");
    // Ignore provided id
    facilityOperator.setId(null);
    facilityOperatorRepository.save(facilityOperator);
    return facilityOperator;
  }

  /**
   * Get all facilityOperators.
   *
   * @return facilityOperators.
   */
  @RequestMapping(value = "/facilityOperators", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Iterable<FacilityOperator> getAllFacilityOperators() {
    rightService.checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT);
    Iterable<FacilityOperator> facilityOperators = facilityOperatorRepository.findAll();
    if (facilityOperators == null) {
      throw new NotFoundException(FacilityOperatorMessageKeys.ERROR_NOT_FOUND);
    } else {
      return facilityOperators;
    }
  }

  /**
   * Allows updating facilityOperator.
   *
   * @param facilityOperator   A facilityOperator bound to the request body.
   * @param facilityOperatorId UUID of facilityOperator which we want to update.
   * @return the updated facilityOperator.
   */
  @RequestMapping(value = "/facilityOperators/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public FacilityOperator updateFacilityOperator(
      @RequestBody FacilityOperator facilityOperator, @PathVariable("id") UUID facilityOperatorId) {
    rightService.checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT);
    LOGGER.debug("Updating facility operator");
    facilityOperatorRepository.save(facilityOperator);
    return facilityOperator;
  }

  /**
   * Get chosen facilityOperator.
   *
   * @param facilityOperatorId UUID of facilityOperator whose we want to get.
   * @return the facilityOperator.
   */
  @RequestMapping(value = "/facilityOperators/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public FacilityOperator getFacilityOperators(
      @PathVariable("id") UUID facilityOperatorId) {
    rightService.checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT);
    FacilityOperator facilityOperator = facilityOperatorRepository.findOne(facilityOperatorId);
    if (facilityOperator == null) {
      throw new NotFoundException(FacilityOperatorMessageKeys.ERROR_NOT_FOUND);
    } else {
      return facilityOperator;
    }
  }

  /**
   * Allows deleting facilityOperator.
   *
   * @param facilityOperatorId UUID of facilityOperator whose we want to delete.
   */
  @RequestMapping(value = "/facilityOperators/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteFacilityOperators(@PathVariable("id") UUID facilityOperatorId) {
    rightService.checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT);
    FacilityOperator facilityOperator = facilityOperatorRepository.findOne(facilityOperatorId);
    if (facilityOperator == null) {
      throw new NotFoundException(FacilityOperatorMessageKeys.ERROR_NOT_FOUND);
    } else {
      facilityOperatorRepository.delete(facilityOperator);
    }
  }
}
