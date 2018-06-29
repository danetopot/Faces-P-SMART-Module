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
package org.openmrs.module.facespsmart.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.facespsmart.PsmartStore;
import org.openmrs.module.facespsmart.api.PsmartService;
import org.openmrs.module.facespsmart.api.db.PsmartDAO;

import java.util.Date;
import java.util.List;

/**
 * It is a default implementation of {@link PsmartService}.
 */
public class PsmartServiceImpl extends BaseOpenmrsService implements PsmartService {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private PsmartDAO dao;
	
	/**
     * @param dao the dao to set
     */
    public void setDao(PsmartDAO dao) {
	    this.dao = dao;
    }
    
    /**
     * @return the dao
     */
    public PsmartDAO getDao() {
	    return dao;
    }
    @Override
    public void setPsmartDAO(PsmartDAO dao) {

    }

    @Override
    public PsmartStore savePsmartStoreObject(PsmartStore psmartStore) {
        return dao.savePsmartStoreObject(psmartStore);
    }

    @Override
    public PsmartStore getPsmartStoreObject(Integer integer) {
        return null;
    }

    @Override
    public PsmartStore getPsmartStoreByUuid(String uuid) {
        return null;
    }

    @Override
    public PsmartStore updatePsmartStore(PsmartStore psmartStore) {
        return null;
    }

    @Override
    public List<PsmartStore> getAllPsmartStoreObjects() {
        return null;
    }

    @Override
    public List<PsmartStore> objectsProcessedSinceDate(Date from, Date to, String operation) {
        return null;
    }
}