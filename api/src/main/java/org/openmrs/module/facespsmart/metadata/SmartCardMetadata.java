package org.openmrs.module.facespsmart.metadata;

import org.openmrs.PatientIdentifierType;
import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.encounterType;
import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.form;
import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.patientIdentifierType;

/**
 * Created by rugute on 5/24/18.
 */
public class SmartCardMetadata extends AbstractMetadataBundle {

    public static final String MODULE_ID = "facespsmart";

    public static final class _PatientIdentifierType {
        public static final String SMART_CARD_SERIAL_NUMBER = "56b05b52-89b0-45e1-a9a9-711a7663c38a";
        public static final String HTS_NUMBER = "8d793bee-c2cc-11de-8d13-0010c6dffd0f";
        public static final String GODS_NUMBER = "71309902-e3b6-4d70-b931-cc48205dedf4";
    }

    public static final class _Form {
        public static final String PSMART_HIV_TEST = "7c89a130-88ec-4e1d-b3b2-afa4ef8642a4";
        public static final String PSMART_IMMUNIZATION = "712a82d1-2a0b-4bcb-ae68-6cf1134c45d9";
    }

    public static final class _VisitType {
        public static final String OUTPATIENT = "ee11898b-5397-4358-bebc-07b48e10e9a7";
    }

    /**
     * stored data read from smart card. this is separate so that reports in the system are not affected
     */
    public static final class _EncounterType {
        public static final String EXTERNAL_PSMART_DATA = "66646baa-7eeb-456a-91ab-3b7220d66da3";
    }

    @Override
    public void install() throws Exception {
        install(patientIdentifierType("Smart Card Serial Number", "P-SMART Serial Number", null, null,
                null, PatientIdentifierType.LocationBehavior.NOT_USED, false, _PatientIdentifierType.SMART_CARD_SERIAL_NUMBER));

        install(patientIdentifierType("HTS Number", "Number assigned to clients when tested for HIV", null, null,
                null, PatientIdentifierType.LocationBehavior.NOT_USED, false, _PatientIdentifierType.HTS_NUMBER));

        install(patientIdentifierType("GODS Number", "Number assigned by MPI", null, null,
                null, PatientIdentifierType.LocationBehavior.NOT_USED, false, _PatientIdentifierType.GODS_NUMBER));
        install(encounterType("External P-Smart", "Holds data read from smart card and  belong to other facilities/systems", _EncounterType.EXTERNAL_PSMART_DATA));

        install(form("P-Smart HIV Test Form", "Holds HTS data read from smart card", _EncounterType.EXTERNAL_PSMART_DATA, "1", _Form.PSMART_HIV_TEST));
        install(form("P-Smart Immunization Form", "Holds Immunization data read from smart card", _EncounterType.EXTERNAL_PSMART_DATA, "1", _Form.PSMART_IMMUNIZATION));


    }
}
