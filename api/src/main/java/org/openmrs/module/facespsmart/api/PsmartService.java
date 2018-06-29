/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.facespsmart.api;

import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.facespsmart.PsmartStore;
import org.openmrs.module.facespsmart.api.db.PsmartDAO;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * This service exposes module's core functionality. It is a Spring managed bean which is configured in moduleApplicationContext.xml.
 * <p>
 * It can be accessed only via Context:<br>
 * <code>
 * Context.getService(PsmartService.class).someMethod();
 * </code>
 * 
 * @see org.openmrs.api.context.Context
 */
@Transactional
public interface PsmartService extends OpenmrsService {
     
	/*
	 * Set DAO
	 * 
	 */
    public void setPsmartDAO(PsmartDAO dao);

    public PsmartStore savePsmartStoreObject(PsmartStore psmartStore) throws APIException;

    public PsmartStore getPsmartStoreObject(Integer integer);

    public PsmartStore getPsmartStoreByUuid(String uuid);

    public PsmartStore updatePsmartStore(PsmartStore psmartStore);

    public List<PsmartStore> getAllPsmartStoreObjects();

    /**
     *
     * @param from date
     * @param to date
     * @param operation - created, collected
     * @return List<PsmartStore>
     */
    public List<PsmartStore> objectsProcessedSinceDate(Date from, Date to, String operation);
}