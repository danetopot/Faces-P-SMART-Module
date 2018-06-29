package org.openmrs.module.facespsmart.jsonvalidator.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.facespsmart.metadata.SmartCardMetadata;
import org.openmrs.module.facespsmart.openmrsUtils.Utils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by rugute on 5/25/18.
 */
public class SmartCardEligibleList
    {
        String IMMUNIZATION_FORM_UUID = "712a82d1-2a0b-4bcb-ae68-6cf1134c45d9";
        String HTS_INITIAL_TEST_FORM_UUID = "7c89a130-88ec-4e1d-b3b2-afa4ef8642a4";
        String HTS_CONFIRMATORY_TEST_FORM_UUID = "008cfdfc-135c-495c-b717-f078f6b585c1";

    private JsonNodeFactory getJsonNodeFactory () {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        return factory;
    }

    public ArrayNode getEligibleList() {

        Form HTS_INITIAL_FORM = Context.getFormService().getFormByUuid(HTS_INITIAL_TEST_FORM_UUID);
        Form HTS_CONFIRMATORY_FORM = Context.getFormService().getFormByUuid(HTS_CONFIRMATORY_TEST_FORM_UUID);
        Form IMMUNIZATION_FORM =  Context.getFormService().getFormByUuid(IMMUNIZATION_FORM_UUID);
        List<Encounter> allEncounters = Utils.getEncounters(null, Arrays.asList(HTS_CONFIRMATORY_FORM, HTS_INITIAL_FORM, IMMUNIZATION_FORM));
        PatientIdentifierType SMART_CARD_SERIAL_NUMBER_TYPE = Context.getPatientService().getPatientIdentifierTypeByUuid(SmartCardMetadata._PatientIdentifierType.SMART_CARD_SERIAL_NUMBER);

        ArrayNode node = getJsonNodeFactory().arrayNode();
        Set<Patient> patientList = new HashSet<Patient>();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        int counter = 0;
        for(Encounter encounter: allEncounters) {
            if(counter == 10) {
                break;
            }
            Patient patient = encounter.getPatient();
            List<PatientIdentifier> identifiers = patient.getPatientIdentifiers(SMART_CARD_SERIAL_NUMBER_TYPE);
            ObjectNode patientNode = getJsonNodeFactory().objectNode();
            if(!patientList.contains(patient) && identifiers.size() == 0 && df.format(encounter.getEncounterDatetime()).equals(df.format(new Date())) && patient.getAge() < 15) {
                patientNode.put("PATIENTID", patient.getPatientId());
                patientNode.put("FIRSTNAME", patient.getGivenName());
                patientNode.put("MIDDLENAME", patient.getMiddleName());
                patientNode.put("LASTNAME", patient.getFamilyName());
                patientNode.put("AGE", patient.getAge());
                patientNode.put("GENDER", patient.getGender());
                patientList.add(patient);
                node.add(patientNode);
                counter++;

            }


        }
        /*ObjectNode wrapper = getJsonNodeFactory().objectNode();
        wrapper.put("ELIGIBLE_LIST", node);*/
        return node;
    }
}
