package org.openmrs.module.facespsmart.jsonvalidator.mapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.facespsmart.jsonvalidator.utils.SHRUtils;
import org.openmrs.module.facespsmart.metadata.SmartCardMetadata;
import org.openmrs.module.facespsmart.openmrsUtils.Utils;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by rugute on 5/24/18.
 */
public class IncomingPatientSHR {

       private Patient patient;
        private PersonService personService;
        private PatientService patientService;
        private ObsService obsService;
        private ConceptService conceptService;
        private AdministrationService administrationService;
        private EncounterService encounterService;
        private String incomingSHR;

        String TELEPHONE_CONTACT = "86e70608-1486-4098-88e7-4324faf722f7";
        String CIVIL_STATUS_CONCEPT = "1054AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        String PSMART_ENCOUNTER_TYPE_UUID = "f0c8d011-1a9e-4251-9648-4c720fc117c7";
        String HEI_UNIQUE_NUMBER = "ff3ddecf-ab5a-102d-be97-85aedb3d9f67";
        String NATIONAL_ID = "f5d7f029-b5d7-47e3-a292-64ceff17c2ae";
        String UNIQUE_PATIENT_NUMBER = "e2871922-024c-48a7-9803-854b97da1c2a";
        String ANC_NUMBER = "8197AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        String HTS_CONFIRMATORY_TEST_FORM_UUID = "008cfdfc-135c-495c-b717-f078f6b585c1";
        String HTS_INITIAL_TEST_FORM_UUID = "7c89a130-88ec-4e1d-b3b2-afa4ef8642a4";
        String IMMUNIZATION_FORM_UUID = "712a82d1-2a0b-4bcb-ae68-6cf1134c45d9";

        protected Log logger = LogFactory.getLog(getClass());


        public IncomingPatientSHR(String shr) {

            this.patientService = Context.getPatientService();
            this.personService = Context.getPersonService();
            this.obsService = Context.getObsService();
            this.administrationService = Context.getAdministrationService();
            this.conceptService = Context.getConceptService();
            this.encounterService = Context.getEncounterService();
            this.incomingSHR = shr;
        }

        public IncomingPatientSHR(Integer patientID) {

            this.patientService = Context.getPatientService();
            this.personService = Context.getPersonService();
            this.obsService = Context.getObsService();
            this.administrationService = Context.getAdministrationService();
            this.conceptService = Context.getConceptService();
            this.encounterService = Context.getEncounterService();
            this.patient = patientService.getPatient(patientID);
        }

        public String processIncomingSHR() {

            //Patient existingPatient = checkIfPatientExists();
            String msg = "";
            Patient patient = checkIfPatientExists();
            if (patient != null) {
                this.patient = patient;
            } else {
                createOrUpdatePatient();
            }

            savePersonAddresses();
            savePersonAttributes();
            addOtherPatientIdentifiers();


            try {
                patientService.savePatient(this.patient);

                try {
                    saveHivTestData();
                    try {
                        saveImmunizationData();
                        return "Successfully processed P-Smart data";
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "There was an error processing immunization data";
                    }

                    //checkinPatient();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    msg = "There was an error processing P-Smart HIV Test Data";
                }

            } catch (Exception e) {
                e.printStackTrace();
                msg = "There was an error processing patient SHR";
            }

            //}

            return msg;
        }

        public String patientExists() {
            return checkIfPatientExists() != null ? checkIfPatientExists().getGivenName().concat(" ").concat(checkIfPatientExists().getFamilyName()) : "Client doesn't exist in the system";
        }

        private void checkinPatient() {
            Visit newVisit = new Visit();
            newVisit.setPatient(patient);
            newVisit.setStartDatetime(new Date());
            newVisit.setVisitType(MetadataUtils.existing(VisitType.class, SmartCardMetadata._VisitType.OUTPATIENT));
            Context.getVisitService().saveVisit(newVisit);

        }

        public String assignCardSerialIdentifier(String identifier, String encryptedSHR) {
            PatientIdentifierType SMART_CARD_SERIAL_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(SmartCardMetadata._PatientIdentifierType.SMART_CARD_SERIAL_NUMBER);

            if (identifier != null) {

                // check if no other patient has same identifier
                List<Patient> patientsAssignedId = patientService.getPatients(null, identifier.trim(), Arrays.asList(SMART_CARD_SERIAL_NUMBER_TYPE), false);
                if (patientsAssignedId.size() > 0) {
                    return "Identifier already assigned";
                }

                // check if patient already has the identifier
                List<PatientIdentifier> existingIdentifiers = patient.getPatientIdentifiers(SMART_CARD_SERIAL_NUMBER_TYPE);

                boolean found = false;
                for (PatientIdentifier id : existingIdentifiers) {
                    if (id.getIdentifier().equals(identifier.trim())) {
                        found = true;
                        return "Client already assigned the card serial";
                    }
                }


                if (!found) {
                    PatientIdentifier patientIdentifier = new PatientIdentifier();
                    patientIdentifier.setIdentifierType(SMART_CARD_SERIAL_NUMBER_TYPE);
                    patientIdentifier.setLocation(Utils.getDefaultLocation());
                    patientIdentifier.setIdentifier(identifier.trim());
                    patient.addIdentifier(patientIdentifier);
                    patientService.savePatient(patient);
                    OutgoingPatientSHR shr = new OutgoingPatientSHR(patient.getPatientId());
                    return shr.patientIdentification().toString();
                }
            }
            return "No identifier provided";
        }

        public Patient checkIfPatientExists() {

            PatientIdentifierType HEI_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(HEI_UNIQUE_NUMBER);
            PatientIdentifierType CCC_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(UNIQUE_PATIENT_NUMBER);
            PatientIdentifierType NATIONAL_ID_TYPE = patientService.getPatientIdentifierTypeByUuid(NATIONAL_ID);
            PatientIdentifierType SMART_CARD_SERIAL_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(SmartCardMetadata._PatientIdentifierType.SMART_CARD_SERIAL_NUMBER);
            PatientIdentifierType HTS_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(SmartCardMetadata._PatientIdentifierType.HTS_NUMBER);
            PatientIdentifierType GODS_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(SmartCardMetadata._PatientIdentifierType.GODS_NUMBER);

            String shrGodsNumber = SHRUtils.getSHR(incomingSHR).pATIENT_IDENTIFICATION.eXTERNAL_PATIENT_ID.iD;
            if(shrGodsNumber != null && !shrGodsNumber.isEmpty()) {
                List<Patient> patientsAssignedGodsNumber = patientService.getPatients(null, shrGodsNumber.trim(), Arrays.asList(GODS_NUMBER_TYPE), false);
                if (patientsAssignedGodsNumber.size() > 0) {
                    return patientsAssignedGodsNumber.get(0);
                }
            }
            for (int x = 0; x < SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID.length; x++) {

                String idType = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID[x].iDENTIFIER_TYPE;
                PatientIdentifierType identifierType = null;

                String identifier = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID[x].iD;

                if (idType.equals("ANC_NUMBER")) {
                    // get patient with the identifier

                    List<Obs> obs = obsService.getObservations(
                            null,
                            null,
                            Arrays.asList(conceptService.getConceptByUuid(ANC_NUMBER)),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false
                    );
                    for (Obs ancNo : obs) {
                        if (ancNo.getValueText().equals(identifier.trim()))
                            return (Patient) ancNo.getPerson();
                    }

                } else {
                    if (idType.equals("HEI_NUMBER")) {
                        identifierType = HEI_NUMBER_TYPE;
                    } else if (idType.equals("CCC_NUMBER")) {
                        identifierType = CCC_NUMBER_TYPE;
                    } else if (idType.equals("NATIONAL_ID")) {
                        identifierType = NATIONAL_ID_TYPE;
                    } else if (idType.equals("CARD_SERIAL_NUMBER")) {
                        identifierType = SMART_CARD_SERIAL_NUMBER_TYPE;
                    } else if (idType.equals("HTS_NUMBER")) {
                        identifierType = HTS_NUMBER_TYPE;
                    }

                    if(identifierType != null && identifier != null) {
                        List<Patient> patientsAlreadyAssigned = patientService.getPatients(null, identifier.trim(), Arrays.asList(identifierType), false);
                        if (patientsAlreadyAssigned.size() > 0) {
                            return patientsAlreadyAssigned.get(0);
                        }
                    }
                }

            }


            return null;
        }

        public boolean checkIfPatientHasIdentifier(Patient patient, PatientIdentifierType identifierType, String identifier) {

            List<Patient> patientsWithIdentifierList = patientService.getPatients(null, identifier.trim(), Arrays.asList(identifierType), false);
            if (patientsWithIdentifierList.size() > 0) {
                return patientsWithIdentifierList.get(0).equals(patient);
            }

            return false;
        }

        private void createOrUpdatePatient() {

            String fName = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.pATIENT_NAME.fIRST_NAME;
            String mName = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.pATIENT_NAME.mIDDLE_NAME;
            String lName = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.pATIENT_NAME.lAST_NAME;
            String dobString = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.dATE_OF_BIRTH;
            String dobPrecision = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.dATE_OF_BIRTH_PRECISION;
            Date dob = null;
            try {
                dob = new SimpleDateFormat("yyyyMMdd").parse(dobString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String gender = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.sEX;

            this.patient = new Patient();
            this.patient.setGender(gender);
            this.patient.addName(new PersonName(fName, mName, lName));
            if (dob != null) {
                this.patient.setBirthdate(dob);
            }

            if (dobPrecision != null && dobPrecision.equals("ESTIMATED")) {
                this.patient.setBirthdateEstimated(true);
            } else if (dobPrecision != null && dobPrecision.equals("EXACT")) {
                this.patient.setBirthdateEstimated(false);
            }

        }

        private void addOpenMRSIdentifier() {
            PatientIdentifier openMRSID = generateOpenMRSID();
            patient.addIdentifier(openMRSID);
        }

        private void addOtherPatientIdentifiers() {

            PatientIdentifierType HEI_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(HEI_UNIQUE_NUMBER);
            PatientIdentifierType CCC_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(UNIQUE_PATIENT_NUMBER);
            PatientIdentifierType NATIONAL_ID_TYPE = patientService.getPatientIdentifierTypeByUuid(NATIONAL_ID);
            PatientIdentifierType SMART_CARD_SERIAL_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(SmartCardMetadata._PatientIdentifierType.SMART_CARD_SERIAL_NUMBER);
            PatientIdentifierType HTS_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(SmartCardMetadata._PatientIdentifierType.HTS_NUMBER);
            PatientIdentifierType GODS_NUMBER_TYPE = patientService.getPatientIdentifierTypeByUuid(SmartCardMetadata._PatientIdentifierType.GODS_NUMBER);

            // extract GOD's Number
            String shrGodsNumber = SHRUtils.getSHR(incomingSHR).pATIENT_IDENTIFICATION.eXTERNAL_PATIENT_ID.iD;
            if (shrGodsNumber != null) {
                if(!checkIfPatientHasIdentifier(this.patient, GODS_NUMBER_TYPE, shrGodsNumber.trim())) {
                    String godsNumberAssigningFacility = SHRUtils.getSHR(incomingSHR).pATIENT_IDENTIFICATION.eXTERNAL_PATIENT_ID.aSSIGNING_FACILITY;
                    PatientIdentifier godsNumber = new PatientIdentifier();
                    godsNumber.setIdentifierType(GODS_NUMBER_TYPE);
                    godsNumber.setIdentifier(shrGodsNumber);
                    godsNumber.setLocation(Utils.getLocationFromMFLCode(godsNumberAssigningFacility) != null? Utils.getLocationFromMFLCode(godsNumberAssigningFacility) : Utils.getDefaultLocation());
                    patient.addIdentifier(godsNumber);
                }

            }

            // OpenMRS ID
            PatientIdentifierType openmrsIDType = Context.getPatientService().getPatientIdentifierTypeByUuid("8d793bee-c2cc-11de-8d13-0010c6dffd0f");
            PatientIdentifier existingIdentifier = patient.getPatientIdentifier(openmrsIDType);

            if(existingIdentifier != null) {

            } else {
                addOpenMRSIdentifier();
            }

            // process internal identifiers

            for (int x = 0; x < SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID.length; x++) {

                String idType = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID[x].iDENTIFIER_TYPE;
                PatientIdentifierType identifierType = null;
                String identifier = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID[x].iD;

                if (idType.equals("ANC_NUMBER")) {
                    // first save patient
               /* patientService.savePatient(this.patient);
                Obs ancNumberObs = new Obs();
                ancNumberObs.setConcept(conceptService.getConceptByUuid(ANC_NUMBER));
                ancNumberObs.setValueText(identifier);
                ancNumberObs.setPerson(this.patient);
                ancNumberObs.setObsDatetime(new Date());
                obsService.saveObs(ancNumberObs, null);*/

                } else {
                    if (idType.equals("HEI_NUMBER")) {
                        identifierType = HEI_NUMBER_TYPE;
                    } else if (idType.equals("CCC_NUMBER")) {
                        identifierType = CCC_NUMBER_TYPE;
                    } else if (idType.equals("NATIONAL_ID")) {
                        identifierType = NATIONAL_ID_TYPE;
                    } else if (idType.equals("CARD_SERIAL_NUMBER")) {
                        identifierType = SMART_CARD_SERIAL_NUMBER_TYPE;
                    } else if (idType.equals("HTS_NUMBER")) {
                        identifierType = HTS_NUMBER_TYPE;
                    } else {
                        continue;
                    }

                    if(!checkIfPatientHasIdentifier(this.patient, identifierType, identifier)) {
                        PatientIdentifier patientIdentifier = new PatientIdentifier();
                        String assigningFacility = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID[x].aSSIGNING_FACILITY;
                        patientIdentifier.setIdentifierType(identifierType);
                        patientIdentifier.setIdentifier(identifier);
                        patientIdentifier.setLocation(Utils.getDefaultLocation());
                        patientIdentifier.setLocation(Utils.getLocationFromMFLCode(assigningFacility) != null? Utils.getLocationFromMFLCode(assigningFacility) : Utils.getDefaultLocation());

                        //identifierSet.add(patientIdentifier);
                        patient.addIdentifier(patientIdentifier);

                    }
                }

            }
            Iterator<PatientIdentifier> pIdentifiers = patient.getIdentifiers().iterator();
            PatientIdentifier currentIdentifier = null;
            PatientIdentifier preferredIdentifier = null;
            while (pIdentifiers.hasNext()) {
                currentIdentifier = pIdentifiers.next();
                if (currentIdentifier.isPreferred()) {
                    if (preferredIdentifier != null) { // if there's a preferred address already exists, make it preferred=false
                        preferredIdentifier.setPreferred(false);
                    }
                    preferredIdentifier = currentIdentifier;
                }
            }
            if ((preferredIdentifier == null) && (currentIdentifier != null)) { // No preferred identifier. Make the last identifier entry as preferred.
                currentIdentifier.setPreferred(true);
            }


        }

        Concept testTypeConverter(String key) {
            Map<String, Concept> testTypeList = new HashMap<String, Concept>();
            testTypeList.put("SCREENING", conceptService.getConcept(6875));
            testTypeList.put("CONFIRMATORY", conceptService.getConcept(7778));
            return testTypeList.get(key);

        }

        String testTypeToStringConverter(Concept key) {
            Map<Concept, String> testTypeList = new HashMap<Concept, String>();
            testTypeList.put(conceptService.getConcept(6875),"SCREENING");
            testTypeList.put(conceptService.getConcept(7778), "CONFIRMATORY");
            return testTypeList.get(key);

        }

        Concept hivStatusConverter(String key) {
            Map<String, Concept> hivStatusList = new HashMap<String, Concept>();
            hivStatusList.put("POSITIVE", conceptService.getConcept(703));
            hivStatusList.put("NEGATIVE", conceptService.getConcept(664));
            hivStatusList.put("INCONCLUSIVE", conceptService.getConcept(1138));
            return hivStatusList.get(key);
        }

        Concept testStrategyConverter(String key) {
            Map<String, Concept> hivTestStrategyList = new HashMap<String, Concept>();
            hivTestStrategyList.put("HP", conceptService.getConcept(7925));
            hivTestStrategyList.put("NP", conceptService.getConcept(7926));
            hivTestStrategyList.put("VI", conceptService.getConcept(7927));
            hivTestStrategyList.put("VS", conceptService.getConcept(7928));
            hivTestStrategyList.put("HB", conceptService.getConcept(7929));
            hivTestStrategyList.put("MO", conceptService.getConcept(7930));
            return hivTestStrategyList.get(key);
        }

        private void savePersonAttributes() {
            String tELEPHONE = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.pHONE_NUMBER;
            PersonAttributeType type = personService.getPersonAttributeTypeByUuid(TELEPHONE_CONTACT);
            if (tELEPHONE != null) {
                PersonAttribute attribute = new PersonAttribute(type, tELEPHONE);

                try {
                    Object hydratedObject = attribute.getHydratedObject();
                    if (hydratedObject == null || "".equals(hydratedObject.toString())) {
                        // if null is returned, the value should be blanked out
                        attribute.setValue("");
                    } else if (hydratedObject instanceof Attributable) {
                        attribute.setValue(((Attributable) hydratedObject).serialize());
                    } else if (!hydratedObject.getClass().getName().equals(type.getFormat())) {
                        // if the classes doesn't match the format, the hydration failed somehow
                        // TODO change the PersonAttribute.getHydratedObject() to not swallow all errors?
                        throw new APIException();
                    }
                }
                catch (APIException e) {
                    //.warn("Got an invalid value: " + value + " while setting personAttributeType id #" + paramName, e);
                    // setting the value to empty so that the user can reset the value to something else
                    attribute.setValue("");
                }
                patient.addAttribute(attribute);
            }

        }

        private void savePersonAddresses() {
            /**
             * county: personAddress.country
             * sub-county: personAddress.stateProvince
             * ward: personAddress.address4
             * landmark: personAddress.address2
             * postal address: personAddress.address1
             */

            String postaladdress = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.pATIENT_ADDRESS.pOSTAL_ADDRESS;
            String vILLAGE = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.pATIENT_ADDRESS.pHYSICAL_ADDRESS.vILLAGE;
            String wARD = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.pATIENT_ADDRESS.pHYSICAL_ADDRESS.wARD;
            String sUBCOUNTY = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.pATIENT_ADDRESS.pHYSICAL_ADDRESS.sUB_COUNTY;
            String cOUNTY = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.pATIENT_ADDRESS.pHYSICAL_ADDRESS.cOUNTY;
            String nEAREST_LANDMARK = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.pATIENT_ADDRESS.pHYSICAL_ADDRESS.nEAREST_LANDMARK;

            Set<PersonAddress> patientAddress = patient.getAddresses();
            //Set<PersonAddress> patientAddress = new TreeSet<PersonAddress>();
            if(patientAddress.size() > 0) {
                for (PersonAddress address : patientAddress) {
                    if (cOUNTY != null) {
                        address.setCountry(cOUNTY);
                    }
                    if (sUBCOUNTY != null) {
                        address.setStateProvince(sUBCOUNTY);
                    }
                    if (wARD != null) {
                        address.setAddress4(wARD);
                    }
                    if (nEAREST_LANDMARK != null) {
                        address.setAddress2(nEAREST_LANDMARK);
                    }
                    if (vILLAGE != null) {
                        address.setAddress2(vILLAGE);
                    }
                    if (postaladdress != null) {
                        address.setAddress1(postaladdress);
                    }
                    patient.addAddress(address);
                }
            } else {
                PersonAddress pa = new PersonAddress();
                if (cOUNTY != null) {
                    pa.setCountry(cOUNTY);
                }
                if (sUBCOUNTY != null) {
                    pa.setStateProvince(sUBCOUNTY);
                }
                if (wARD != null) {
                    pa.setAddress4(wARD);
                }
                if (nEAREST_LANDMARK != null) {
                    pa.setAddress2(nEAREST_LANDMARK);
                }
                if (vILLAGE != null) {
                    pa.setAddress2(vILLAGE);
                }
                if (postaladdress != null) {
                    pa.setAddress1(postaladdress);
                }
                patient.addAddress(pa);
            }

        }


        private void saveHivTestData() {



            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
            Set<SmartCardHivTest> incomingTests = new HashSet<SmartCardHivTest>();
            Set<SmartCardHivTest> existingTests = new HashSet<SmartCardHivTest>(getHivTests());

            if(SHRUtils.getSHR(this.incomingSHR).hIV_TEST != null) {
                for (int i = 0; i < SHRUtils.getSHR(this.incomingSHR).hIV_TEST.length; i++) {

                    String dateStr = SHRUtils.getSHR(this.incomingSHR).hIV_TEST[i].dATE;
                    String result = SHRUtils.getSHR(this.incomingSHR).hIV_TEST[i].rESULT;
                    String type = SHRUtils.getSHR(this.incomingSHR).hIV_TEST[i].tYPE;
                    String facility = SHRUtils.getSHR(this.incomingSHR).hIV_TEST[i].fACILITY;
                    String strategy = SHRUtils.getSHR(this.incomingSHR).hIV_TEST[i].sTRATEGY;
                    String providerDetails = SHRUtils.getSHR(this.incomingSHR).hIV_TEST[i].pROVIDER_DETAILS.nAME;
                    String providerId = SHRUtils.getSHR(this.incomingSHR).hIV_TEST[i].pROVIDER_DETAILS.iD;

                    Date date = null;

                    try {
                        date = new SimpleDateFormat("yyyyMMdd").parse(dateStr);
                    } catch (ParseException ex) {

                        ex.printStackTrace();
                    }
                    // skip all tests done in the facility
                    if (Integer.valueOf(facility) == Integer.valueOf(Utils.getDefaultLocationMflCode(Utils.getDefaultLocation()))) {//temp value for this facility
                        continue;
                    }

                    // drop any entry with missing information
                    if (hivStatusConverter(result.trim()) != null && testStrategyConverter(strategy.trim()) != null && date != null
                            && facility != null && providerDetails != null && providerId != null && testTypeConverter(type.trim()) != null) {
                        incomingTests.add(new SmartCardHivTest(hivStatusConverter(result.trim()),
                                facility.trim(),
                                testStrategyConverter(strategy.trim()), date, type.trim(), providerDetails, providerId));
                    }
                }
            }
            Iterator<SmartCardHivTest> ite = incomingTests.iterator();
            while(ite.hasNext()) {
                SmartCardHivTest value = ite.next();
                for(SmartCardHivTest db : existingTests) {
                    if(db.equals(value)) {
                        ite.remove();
                        break;
                    }
                }
            }

            for(SmartCardHivTest thisTest : incomingTests) {

                Encounter enc = new Encounter();
                Location location = Utils.getDefaultLocation();
                enc.setLocation(location);
                enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid(SmartCardMetadata._EncounterType.EXTERNAL_PSMART_DATA));
                enc.setEncounterDatetime(thisTest.getDateTested());
                enc.setPatient(patient);
                enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
                enc.setForm(Context.getFormService().getFormByUuid(SmartCardMetadata._Form.PSMART_HIV_TEST));


                // build observations
                setEncounterObs(enc, thisTest);
            }

            patientService.savePatient(patient);
        }

        private void setEncounterObs(Encounter enc, SmartCardHivTest hivTest) {

            Integer finalHivTestResultConcept = 1544;
            Integer testTypeConcept = 6709;
            Integer testStrategyConcept = 8145;
            Integer healthProviderConcept = 1473;
            Integer healthFacilityNameConcept = 8156;
            Integer healthProviderIdentifierConcept = 8157;
            // test result
            Obs o = new Obs();
            o.setConcept(conceptService.getConcept(finalHivTestResultConcept));
            o.setDateCreated(new Date());
            o.setCreator(Context.getUserService().getUser(1));
            o.setLocation(enc.getLocation());
            o.setObsDatetime(enc.getEncounterDatetime());
            o.setPerson(this.patient);
            o.setValueCoded(hivTest.getResult());

            // test type
            Obs o1 = new Obs();
            o1.setConcept(conceptService.getConcept(testTypeConcept));
            o1.setDateCreated(new Date());
            o1.setCreator(Context.getUserService().getUser(1));
            o1.setLocation(enc.getLocation());
            o1.setObsDatetime(enc.getEncounterDatetime());
            o1.setPerson(this.patient);
            o1.setValueCoded(testTypeConverter(hivTest.getType().trim()));

            // test strategy
            Obs o2 = new Obs();
            o2.setConcept(conceptService.getConcept(testStrategyConcept));
            o2.setDateCreated(new Date());
            o2.setCreator(Context.getUserService().getUser(1));
            o2.setLocation(enc.getLocation());
            o2.setObsDatetime(enc.getEncounterDatetime());
            o2.setPerson(this.patient);
            o2.setValueCoded(hivTest.getStrategy());

            // test provider
            // only do this if provider details is not null

            Obs o3 = new Obs();
            o3.setConcept(conceptService.getConcept(healthProviderConcept));
            o3.setDateCreated(new Date());
            o3.setCreator(Context.getUserService().getUser(1));
            o3.setLocation(enc.getLocation());
            o3.setObsDatetime(enc.getEncounterDatetime());
            o3.setPerson(this.patient);
            o3.setValueText(hivTest.getProviderName().trim());

            // test provider id
            Obs o5 = new Obs();
            o5.setConcept(conceptService.getConcept(healthProviderIdentifierConcept));
            o5.setDateCreated(new Date());
            o5.setCreator(Context.getUserService().getUser(1));
            o5.setLocation(enc.getLocation());
            o5.setObsDatetime(enc.getEncounterDatetime());
            o5.setPerson(this.patient);
            o5.setValueText(hivTest.getProviderId().trim());

            // test facility
            Obs o4 = new Obs();
            o4.setConcept(conceptService.getConcept(healthFacilityNameConcept));
            o4.setDateCreated(new Date());
            o4.setCreator(Context.getUserService().getUser(1));
            o4.setLocation(enc.getLocation());
            o4.setObsDatetime(enc.getEncounterDatetime());
            o4.setPerson(this.patient);
            o4.setValueText(hivTest.getFacility().trim());


            enc.addObs(o);
            enc.addObs(o1);
            enc.addObs(o2);
            enc.addObs(o3);
            enc.addObs(o4);
            enc.addObs(o5);
            encounterService.saveEncounter(enc);
        }
        private void saveImmunizationData() {
            Set<ImmunizationWrapper> immunizationData = new HashSet<ImmunizationWrapper>(processImmunizationDataFromSHR());
            Set<ImmunizationWrapper> existingImmunizationData = new HashSet<ImmunizationWrapper>(getAllImmunizationDataFromDb());

            if(immunizationData.size() > 0) {
                Iterator<ImmunizationWrapper> ite = immunizationData.iterator();
                while (ite.hasNext()) {
                    ImmunizationWrapper value = ite.next();
                    for (ImmunizationWrapper db : existingImmunizationData) {
                        if (db.equals(value)) {
                            ite.remove();
                            break;
                        }
                    }
                }
            }
            if(immunizationData.size() > 0) {
                saveImmunizationData(immunizationData);
            }
        }

        private void saveImmunizationData(Set<ImmunizationWrapper> data) {

            EncounterType pSmartDataEncType = encounterService.getEncounterTypeByUuid(SmartCardMetadata._EncounterType.EXTERNAL_PSMART_DATA);
            Form pSmartImmunizationForm = Context.getFormService().getFormByUuid(SmartCardMetadata._Form.PSMART_IMMUNIZATION);

            // organize data according to date
            Map<Date, List<ImmunizationWrapper>> organizedImmunizations = new HashMap<Date, List<ImmunizationWrapper>>();
            for (ImmunizationWrapper immunization : data) {
                Date vaccineDate = immunization.getVaccineDate();
                if (!organizedImmunizations.containsKey(vaccineDate)) {
                    organizedImmunizations.put(vaccineDate, new ArrayList<ImmunizationWrapper>());

                }
                organizedImmunizations.get(vaccineDate).add(immunization);
            }

            // loop through different dates

            for (Map.Entry<Date, List<ImmunizationWrapper>> entry : organizedImmunizations.entrySet()) {

                Date key = entry.getKey();
                List<ImmunizationWrapper> immunizationList = entry.getValue();

                // build encounter
                Encounter enc = new Encounter();
                Location location = Utils.getDefaultLocation();
                enc.setLocation(location);
                enc.setEncounterType(pSmartDataEncType);
                enc.setEncounterDatetime(key);
                enc.setPatient(patient);
                enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
                enc.setForm(pSmartImmunizationForm);

                // build obs and add to encounter
                for (ImmunizationWrapper iEntry : immunizationList) {
                    Set<Obs> obs = createImmunizationObs(iEntry, enc);
                    enc.setObs(obs);
                }

                encounterService.saveEncounter(enc);

            }
            patientService.savePatient(patient);

        }

        private Set<Obs> createImmunizationObs(ImmunizationWrapper entry, Encounter encounter) {

            Concept groupingConcept = conceptService.getConcept(1421);
            Concept vaccineConcept = conceptService.getConcept(984);
            Concept sequenceNumber = conceptService.getConcept(1418);
            Set<Obs> immunizationObs = new HashSet<Obs>();

            Obs obsGroup = new Obs();
            obsGroup.setConcept(groupingConcept);
            obsGroup.setObsDatetime(entry.getVaccineDate());
            obsGroup.setPerson(patient);
            obsGroup.setEncounter(encounter);

            Obs immunization = new Obs();
            immunization.setConcept(vaccineConcept);
            immunization.setValueCoded(entry.getVaccine());
            immunization.setObsDatetime(entry.getVaccineDate());
            immunization.setPerson(patient);
            immunization.setObsGroup(obsGroup);
            immunization.setEncounter(encounter);

            immunizationObs.addAll(Arrays.asList(obsGroup, immunization));

            if (entry.getSequenceNumber() != null) {
                Obs immunizationSequenceNumber = new Obs();
                immunizationSequenceNumber.setConcept(sequenceNumber);
                immunizationSequenceNumber.setValueNumeric(Double.valueOf(entry.getSequenceNumber()));
                immunizationSequenceNumber.setPerson(patient);
                immunizationSequenceNumber.setObsGroup(obsGroup);
                immunizationSequenceNumber.setObsDatetime(entry.getVaccineDate());
                immunizationSequenceNumber.setEncounter(encounter);
                immunizationObs.add(immunizationSequenceNumber);

            }


            return immunizationObs;
        }

        private List<ImmunizationWrapper> getAllImmunizationDataFromDb() {

            Concept groupingConcept = conceptService.getConcept(1421);
            Concept	vaccineConcept = conceptService.getConcept(984);
            Concept sequenceNumber = conceptService.getConcept(1418);
            Form pSmartImmunizationForm = Context.getFormService().getFormByUuid(SmartCardMetadata._Form.PSMART_IMMUNIZATION);


            // get immunizations from immunization form
            List<Encounter> immunizationEncounters = encounterService.getEncounters(
                    patient,
                    null,
                    null,
                    null,
                    Arrays.asList(Context.getFormService().getFormByUuid(IMMUNIZATION_FORM_UUID), pSmartImmunizationForm),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            List<ImmunizationWrapper> immunizationList = new ArrayList<ImmunizationWrapper>();
            // extract blocks of vaccines organized by grouping concept
            for(Encounter encounter : immunizationEncounters) {
                List<Obs> obs = obsService.getObservations(
                        Arrays.asList(Context.getPersonService().getPerson(patient.getPersonId())),
                        Arrays.asList(encounter),
                        Arrays.asList(groupingConcept),
                        null,
                        null,
                        null,
                        Arrays.asList("obsId"),
                        null,
                        null,
                        null,
                        null,
                        false
                );
                // Iterate through groups
                for(Obs group : obs) {
                    ImmunizationWrapper groupWrapper;
                    Concept vaccine = null;
                    Integer sequence = null;
                    Date vaccineDate = obs.get(0).getObsDatetime();
                    Set<Obs> members = group.getGroupMembers();
                    // iterate through obs for a particular group
                    for (Obs memberObs : members) {
                        if (memberObs.getConcept().equals(vaccineConcept) ) {
                            vaccine = memberObs.getValueCoded();
                        } else if (memberObs.getConcept().equals(sequenceNumber)) {
                            sequence = memberObs.getValueNumeric() != null? memberObs.getValueNumeric().intValue() : sequence;
                        }
                    }
                    immunizationList.add(new ImmunizationWrapper(vaccine, sequence, vaccineDate));


                }
            }


            return immunizationList;
        }

        private List<ImmunizationWrapper> processImmunizationDataFromSHR() {

            Concept BCG = conceptService.getConcept(7983);
            Concept OPV = conceptService.getConcept(6869);
            Concept IPV = conceptService.getConcept(8107);
            Concept DPT = conceptService.getConcept(781);
            Concept PCV = conceptService.getConcept(7990);
            Concept ROTA = conceptService.getConcept(6932);
            Concept MEASLESorRUBELLA = conceptService.getConcept(7321);
            Concept MEASLES = conceptService.getConcept(63);
            Concept YELLOW_FEVER = conceptService.getConcept(5864);

            List<ImmunizationWrapper> shrData = new ArrayList<ImmunizationWrapper>();
            if(SHRUtils.getSHR(this.incomingSHR).iMMUNIZATION != null) {
                for (int i = 0; i < SHRUtils.getSHR(this.incomingSHR).iMMUNIZATION.length; i++) {

                    String name = SHRUtils.getSHR(this.incomingSHR).iMMUNIZATION[i].nAME;
                    String dateAministered = SHRUtils.getSHR(this.incomingSHR).iMMUNIZATION[i].dATE_ADMINISTERED;
                    Date date = null;
                    try {
                        date = new SimpleDateFormat("yyyyMMdd").parse(dateAministered);
                    } catch (ParseException ex) {

                        ex.printStackTrace();
                    }
                    ImmunizationWrapper entry = new ImmunizationWrapper();

                    if (name.trim().equals("BCG")) {
                        entry.setVaccine(BCG);
                        entry.setSequenceNumber(null);
                    } else if (name.trim().equals("OPV_AT_BIRTH")) {
                        entry.setVaccine(OPV);
                        entry.setSequenceNumber(0);
                    } else if (name.trim().equals("OPV1")) {
                        entry.setVaccine(OPV);
                        entry.setSequenceNumber(1);
                    } else if (name.trim().equals("OPV2")) {
                        entry.setVaccine(OPV);
                        entry.setSequenceNumber(2);
                    } else if (name.trim().equals("OPV3")) {
                        entry.setVaccine(OPV);
                        entry.setSequenceNumber(3);
                    } else if (name.trim().equals("PCV10-1")) {
                        entry.setVaccine(PCV);
                        entry.setSequenceNumber(1);
                    } else if (name.trim().equals("PCV10-2")) {
                        entry.setVaccine(PCV);
                        entry.setSequenceNumber(2);
                    } else if (name.trim().equals("PCV10-3")) {
                        entry.setVaccine(PCV);
                        entry.setSequenceNumber(3);
                    } else if (name.trim().equals("ROTA1")) {
                        entry.setVaccine(ROTA);
                        entry.setSequenceNumber(1);
                    } else if (name.trim().equals("ROTA2")) {
                        entry.setVaccine(ROTA);
                        entry.setSequenceNumber(2);
                    } else if (name.trim().equals("MEASLES6")) {
                        entry.setVaccine(MEASLES);
                        entry.setSequenceNumber(1);
                    } else if (name.trim().equals("MEASLES9")) {
                        entry.setVaccine(MEASLESorRUBELLA);
                        entry.setSequenceNumber(1);
                    } else if (name.trim().equals("MEASLES18")) {
                        entry.setVaccine(MEASLESorRUBELLA);
                        entry.setSequenceNumber(2);
                    }
                    entry.setVaccineDate(date);
                    if (entry.getVaccine() != null && entry.getVaccineDate() != null) {
                        shrData.add(entry);
                    }
                }
            }
            return shrData;
        }

        /**
         * saves the first next of kin details. The system does not support multiple
         */
        private void saveNextOfKinDetails() {

            String NEXT_OF_KIN_ADDRESS = "b5c2765a-73c9-439e-92be-3b42724f02c6";
            String NEXT_OF_KIN_CONTACT = "5c9f67cb-d133-45a6-a573-512b71b625a0";
            String NEXT_OF_KIN_NAME = "29674853-1805-486c-a183-0b82ebb9ece3";
            String NEXT_OF_KIN_RELATIONSHIP = "e83bf759-9ecf-4597-acb6-6ae92844c6f0";
            Set<PersonAttribute> attributes = new TreeSet<PersonAttribute>();
            if (SHRUtils.getSHR(this.incomingSHR).nEXT_OF_KIN != null && SHRUtils.getSHR(this.incomingSHR).nEXT_OF_KIN.length > 0) {
                PersonAttributeType nextOfKinNameAttrType = personService.getPersonAttributeTypeByUuid(NEXT_OF_KIN_NAME);
                PersonAttributeType nextOfKinAddressAttrType = personService.getPersonAttributeTypeByUuid(NEXT_OF_KIN_ADDRESS);
                PersonAttributeType nextOfKinPhoneContactAttrType = personService.getPersonAttributeTypeByUuid(NEXT_OF_KIN_CONTACT);
                PersonAttributeType nextOfKinRelationshipAttrType = personService.getPersonAttributeTypeByUuid(NEXT_OF_KIN_RELATIONSHIP);

                String nextOfKinName = SHRUtils.getSHR(this.incomingSHR).nEXT_OF_KIN[0].nOK_NAME.fIRST_NAME.concat(
                        SHRUtils.getSHR(this.incomingSHR).nEXT_OF_KIN[0].nOK_NAME.mIDDLE_NAME != "" ? SHRUtils.getSHR(this.incomingSHR).nEXT_OF_KIN[0].nOK_NAME.mIDDLE_NAME : ""
                ).concat(
                        SHRUtils.getSHR(this.incomingSHR).nEXT_OF_KIN[0].nOK_NAME.lAST_NAME != "" ? SHRUtils.getSHR(this.incomingSHR).nEXT_OF_KIN[0].nOK_NAME.lAST_NAME : ""
                );

                String nextOfKinAddress = SHRUtils.getSHR(this.incomingSHR).nEXT_OF_KIN[0].aDDRESS;
                String nextOfKinPhoneContact = SHRUtils.getSHR(this.incomingSHR).nEXT_OF_KIN[0].pHONE_NUMBER;
                String nextOfKinRelationship = SHRUtils.getSHR(this.incomingSHR).nEXT_OF_KIN[0].rELATIONSHIP;

                if (nextOfKinName != null) {
                    PersonAttribute kinName = new PersonAttribute();
                    kinName.setAttributeType(nextOfKinNameAttrType);
                    kinName.setValue(nextOfKinName.trim());
                    attributes.add(kinName);
                }

                if (nextOfKinAddress != null) {
                    PersonAttribute kinAddress = new PersonAttribute();
                    kinAddress.setAttributeType(nextOfKinAddressAttrType);
                    kinAddress.setValue(nextOfKinAddress.trim());
                    attributes.add(kinAddress);
                }

                if (nextOfKinPhoneContact != null) {
                    PersonAttribute kinPhoneContact = new PersonAttribute();
                    kinPhoneContact.setAttributeType(nextOfKinPhoneContactAttrType);
                    kinPhoneContact.setValue(nextOfKinPhoneContact.trim());
                    attributes.add(kinPhoneContact);
                }

                if (nextOfKinRelationship != null) {
                    PersonAttribute kinRelationship = new PersonAttribute();
                    kinRelationship.setAttributeType(nextOfKinRelationshipAttrType);
                    kinRelationship.setValue(nextOfKinRelationship.trim());
                    attributes.add(kinRelationship);
                }
                patient.setAttributes(attributes);
            }


        }

        private void saveMotherDetails() {

        }

        private void saveObsData() {

            String cIVIL_STATUS = SHRUtils.getSHR(this.incomingSHR).pATIENT_IDENTIFICATION.mARITAL_STATUS;
            if (cIVIL_STATUS != null) {

            }
        }

        /**
         * Can't save patients unless they have required OpenMRS IDs
         */
        private PatientIdentifier generateOpenMRSID() {
            PatientIdentifierType openmrsIDType = Context.getPatientService().getPatientIdentifierTypeByUuid("8d793bee-c2cc-11de-8d13-0010c6dffd0f");
            String generated = Context.getService(IdentifierSourceService.class).generateIdentifier(openmrsIDType, "OpenMRS Identification Number");
        /*PatientIdentifier existingIdentifier = patient.getPatientIdentifier(openmrsIDType);
        if(existingIdentifier != null) {
            logger.info("Identifier exists");
            System.out.println("Identifier exists");
            return existingIdentifier;
        }*/
            PatientIdentifier identifier = new PatientIdentifier(generated, openmrsIDType, Utils.getDefaultLocation());
            logger.info("New identifier generated");
            System.out.println("New identifier generated");
            return identifier;
        }

        private List<SmartCardHivTest> getHivTests() {

            // test concepts
            Concept finalHivTestResultConcept = conceptService.getConcept(1544);
            Concept	testTypeConcept = conceptService.getConcept(6709);
            Concept testStrategyConcept = conceptService.getConcept(8145);
            Concept testFacilityCodeConcept = conceptService.getConcept(8156);
            Concept healthProviderConcept = conceptService.getConcept(1473);
            Concept healthProviderIdentifierConcept = conceptService.getConcept(8157);



            Form HTS_INITIAL_FORM = Context.getFormService().getFormByUuid(HTS_INITIAL_TEST_FORM_UUID);
            Form HTS_CONFIRMATORY_FORM = Context.getFormService().getFormByUuid(HTS_CONFIRMATORY_TEST_FORM_UUID);

            EncounterType smartCardHTSEntry = Context.getEncounterService().getEncounterTypeByUuid(SmartCardMetadata._EncounterType.EXTERNAL_PSMART_DATA);
            Form SMART_CARD_HTS_FORM = Context.getFormService().getFormByUuid(SmartCardMetadata._Form.PSMART_HIV_TEST);


            List<Encounter> htsEncounters = Utils.getEncounters(patient, Arrays.asList(HTS_CONFIRMATORY_FORM, HTS_INITIAL_FORM));
            List<Encounter> processedIncomingTests = Utils.getEncounters(patient, Arrays.asList(SMART_CARD_HTS_FORM));

            List<SmartCardHivTest> testList = new ArrayList<SmartCardHivTest>();
            // loop through encounters and extract hiv test information
            for(Encounter encounter : htsEncounters) {
                List<Obs> obs = Utils.getEncounterObservationsForQuestions(patient, encounter, Arrays.asList(finalHivTestResultConcept, testTypeConcept, testStrategyConcept));
                testList.add(extractHivTestInformation(obs));
            }

            // append processed tests from card
            for(Encounter encounter : processedIncomingTests) {
                List<Obs> obs = Utils.getEncounterObservationsForQuestions(patient, encounter, Arrays.asList(finalHivTestResultConcept, testTypeConcept, testStrategyConcept, testFacilityCodeConcept, healthProviderConcept, healthProviderIdentifierConcept));
                testList.add(extractHivTestInformation(obs));
            }

            return testList;
        }

        private SmartCardHivTest extractHivTestInformation (List<Obs> obsList) {

            Integer finalHivTestResultConcept = 1544;
            Integer	testTypeConcept = 6709;
            Integer testStrategyConcept = 8145;
            Integer testFacilityCodeConcept = 8156;
            Integer healthProviderConcept = 1473;
            Integer healthProviderIdentifierConcept = 8157;

            Date testDate= obsList.get(0).getObsDatetime();
            User provider = obsList.get(0).getCreator();
            Concept testResult = null;
            String testType = null;
            String testFacility = null;
            Concept testStrategy = null;
            String providerName = null;
            String providerId = null;

            for(Obs obs:obsList) {

                if(obs.getEncounter().getForm().getUuid().equals(HTS_CONFIRMATORY_TEST_FORM_UUID)) {
                    testType = "CONFIRMATORY";
                } else if(obs.getEncounter().getForm().getUuid().equals(HTS_INITIAL_TEST_FORM_UUID)) {
                    testType = "SCREENING";
                }

                if (obs.getConcept().getConceptId().equals(testTypeConcept)) {
                    testType = testTypeToStringConverter(obs.getValueCoded());
                }

                if (obs.getConcept().getConceptId().equals(finalHivTestResultConcept) ) {
                    testResult = obs.getValueCoded();
                } else if (obs.getConcept().getConceptId().equals(testStrategyConcept) ) {
                    testStrategy = obs.getValueCoded();
                } else if(obs.getConcept().getConceptId().equals(testFacilityCodeConcept)) {
                    testFacility = obs.getValueText();
                } else if (obs.getConcept().getConceptId().equals(healthProviderConcept) ) {
                    providerName = obs.getValueText();
                } else if(obs.getConcept().getConceptId().equals(healthProviderIdentifierConcept)) {
                    providerId = obs.getValueText();
                }
            }
            return new SmartCardHivTest(testResult, testFacility, testStrategy, testDate, testType, providerName, providerId);

        }

}
