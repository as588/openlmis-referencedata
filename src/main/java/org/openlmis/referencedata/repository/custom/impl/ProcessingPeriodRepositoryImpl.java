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

package org.openlmis.referencedata.repository.custom.impl;

import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.referencedata.repository.custom.ProcessingPeriodRepositoryCustom;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.List;

public class ProcessingPeriodRepositoryImpl implements ProcessingPeriodRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Finds Periods matching all of provided parameters.
   * @param processingSchedule processingSchedule of searched Periods.
   * @param toDate to which day shall Period start.
   * @return list of all Periods matching all of provided parameters.
   */
  public List<ProcessingPeriod> searchPeriods(
      ProcessingSchedule processingSchedule, LocalDate toDate) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<ProcessingPeriod> query = builder.createQuery(ProcessingPeriod.class);
    Root<ProcessingPeriod> root = query.from(ProcessingPeriod.class);
    Predicate predicate = builder.conjunction();
    if (processingSchedule != null) {
      predicate = builder.and(
              predicate,
              builder.equal(
                      root.get("processingSchedule"), processingSchedule));
    }
    if (toDate != null) {
      predicate = builder.and(
              predicate,
              builder.lessThanOrEqualTo(
                      root.get("startDate"), toDate));
    }
    query.where(predicate);
    query.orderBy(builder.asc(root.get("startDate")));

    return entityManager.createQuery(query).getResultList();
  }
}
