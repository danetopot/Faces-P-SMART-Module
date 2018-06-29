package org.openmrs.module.facespsmart.api;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.facespsmart.jsonvalidator.mapper.OutgoingPatientSHR;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Created by rugute on 5/24/18.
 */
public class OutgoingPatientSHRTest extends BaseModuleContextSensitiveTest {

    @Before
    public void setup() throws Exception {
        executeDataSet("dataset/test-dataset.xml");
    }

    @Test
    public void shouldCheckService() {
        Assert.assertNotNull(Context.getService(PsmartService.class));
    }
    @Ignore
    @Test
    public void shouldReturnPatientSHR() {

        OutgoingPatientSHR shr = new OutgoingPatientSHR(315230);
        Assert.assertNotNull(shr.patientIdentification());
    }
}
