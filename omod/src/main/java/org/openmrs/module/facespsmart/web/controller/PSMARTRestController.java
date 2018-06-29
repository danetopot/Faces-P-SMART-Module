package org.openmrs.module.facespsmart.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.facespsmart.PsmartStore;
import org.openmrs.module.facespsmart.jsonvalidator.mapper.*;
import org.openmrs.module.facespsmart.jsonvalidator.utils.SHRUtils;
import org.openmrs.module.facespsmart.openmrsUtils.Utils;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by rugute on 5/24/18.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/smartcard")
public class PSMARTRestController  extends BaseRestController {

    protected final Log log = LogFactory.getLog(getClass());

    @RequestMapping(method = RequestMethod.POST, value = "/getshr")
    @ResponseBody
    public Object receiveSHR(HttpServletRequest request) {
        Integer patientID=null;
        String requestBody = null;
        MiddlewareRequest thisRequest = null;
        try {
            requestBody = SHRUtils.fetchRequestBody(request.getReader());//request.getParameter("encryptedSHR") != null? request.getParameter("encryptedSHR"): null;
        } catch (IOException e) {
            return new SimpleObject().add("ServerResponse", "Error extracting request body");
        }
        try {
            thisRequest = new com.fasterxml.jackson.databind.ObjectMapper().readValue(requestBody, MiddlewareRequest.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new SimpleObject().add("ServerResponse", "Error reading patient id: " + requestBody);
        }
        patientID=Integer.parseInt(thisRequest.getPatientID());
        if (patientID != 0) {
            OutgoingPatientSHR shr = new OutgoingPatientSHR(patientID);
            return shr.patientIdentification().toString();

        }
        return new SimpleObject().add("identification", "No patient id specified in the request: Got this: => " + request.getParameter("patientID"));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/processshr")
    @ResponseBody
    public Object prepareSHR(HttpServletRequest request) {

        String encryptedSHR=null;
        try {
            encryptedSHR = SHRUtils.fetchRequestBody(request.getReader());//request.getParameter("encryptedSHR") != null? request.getParameter("encryptedSHR"): null;
        } catch (IOException e) {
            //e.printStackTrace();
            return new SimpleObject().add("ServerResponse", "Error extracting request body");
        }

        IncomingPatientSHR shr = new IncomingPatientSHR(encryptedSHR);
        return shr.processIncomingSHR();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/getsmartcardeligiblelist")
    @ResponseBody
    public Object prepareEligibleList(HttpServletRequest request) {
        SmartCardEligibleList list = new SmartCardEligibleList();
        return list.getEligibleList().toString();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/getshrusingcardserial/{cardSerialNo}")
    @ResponseBody
    public Object getShrUsingCardSerial(HttpServletRequest request, @PathVariable("cardSerialNo") String cardSerialNo) {
        if(cardSerialNo != null) {
            OutgoingPatientSHR shr = new OutgoingPatientSHR(cardSerialNo);
            return shr.patientIdentification().toString();
        }
        return null;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/assigncardserialnumber")
    @ResponseBody
    public Object addSmartCardSerialToIdentifiers(HttpServletRequest request) {

        Integer patientID=null;
        String cardSerialNumber=null;
        String requestBody = null;
        MiddlewareRequest thisRequest = null;
        try {
            requestBody = SHRUtils.fetchRequestBody(request.getReader());//request.getParameter("encryptedSHR") != null? request.getParameter("encryptedSHR"): null;
        } catch (IOException e) {
            return new SimpleObject().add("ServerResponse", "Error extracting request body");
        }
        try {
            thisRequest = new com.fasterxml.jackson.databind.ObjectMapper().readValue(requestBody, MiddlewareRequest.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new SimpleObject().add("ServerResponse", "Error reading patient id: " + requestBody);
        }
        patientID=Integer.parseInt(thisRequest.getPatientID());
        cardSerialNumber = thisRequest.getCardSerialNumber();
        IncomingPatientSHR shr = new IncomingPatientSHR(patientID);
        return shr.assignCardSerialIdentifier(cardSerialNumber, null);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/checkexistingclient")
    @ResponseBody
    public Object clientExists(HttpServletRequest request) {

        Integer patientID=null;
        String cardSerialNumber=null;
        String requestBody = null;
        MiddlewareRequest thisRequest = null;
        try {
            requestBody = SHRUtils.fetchRequestBody(request.getReader());//request.getParameter("encryptedSHR") != null? request.getParameter("encryptedSHR"): null;
        } catch (IOException e) {
            return new SimpleObject().add("ServerResponse", "Error extracting request body");
        }

        IncomingPatientSHR shr = new IncomingPatientSHR(requestBody);
        return shr.patientExists();
    }

    /**
     * Handle authentication
     * @param request
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/authenticateuser")
    @ResponseBody
    public Object userAuthentication(HttpServletRequest request) {
        String requestBody = null;
        String userName=null;
        String pwd = null;
        MiddlewareRequest thisRequest = null;
        try {
            requestBody = SHRUtils.fetchRequestBody(request.getReader());//request.getParameter("encryptedSHR") != null? request.getParameter("encryptedSHR"): null;
        } catch (IOException e) {
            return new SimpleObject().add("ServerResponse", "Error extracting request body");
        }

        try {
            thisRequest = new com.fasterxml.jackson.databind.ObjectMapper().readValue(requestBody, MiddlewareRequest.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new SimpleObject().add("ServerResponse", "Error parsing request body: " + requestBody);
        }
        userName = thisRequest.getUserName();
        pwd = thisRequest.getPwd();

        return PsmartAuthentication.authenticateUser(userName.trim(), pwd.trim()).toString();
    }


    @RequestMapping(method = RequestMethod.GET, value = "/testfacilitymfl")
    @ResponseBody
    public Object testFacilityMfl(HttpServletRequest request) {

        return Utils.getDefaultLocationMflCode(Utils.getDefaultLocation());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/getlocationfrommfl")
    @ResponseBody
    public Object getLocationFromFacilityMfl(HttpServletRequest request) {

        return Utils.getLocationFromMFLCode("13738").getName();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/testpsmartstore")
    @ResponseBody
    public Object testpsmartstore(HttpServletRequest request) {
        PsmartStore test = new PsmartStore();
        test.setDateCreated(new Date());
        test.setShr("This is test SHR. I think this cannot work");
        test.setAddendum("this is addendum");
        test.setStatus("PENDING");
        test.setStatusDate(new Timestamp(new Date().getTime()));
        test.setDateCreated(new Timestamp(new Date().getTime()));
        OutgoingPatientSHR obj = new OutgoingPatientSHR();
        obj.saveRegistryEntry(test);

        return null;
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController#getNamespace()
     */

    @Override
    public String getNamespace() {
        return "v1/facespsmart";
    }

    private String getIncomingSHRSample () {
        return "{\n" +
                "  \"VERSION\": \"1.0.0\",\n" +
                "  \"PATIENT_IDENTIFICATION\": {\n" +
                "    \"EXTERNAL_PATIENT_ID\": {\n" +
                "      \"ID\": \"110ec58a-a0f2-4ac4-8393-c866d813b8d8\",\n" +
                "      \"IDENTIFIER_TYPE\": \"GODS_NUMBER\",\n" +
                "      \"ASSIGNING_AUTHORITY\": \"MPI\",\n" +
                "      \"ASSIGNING_FACILITY\": \"10829\"\n" +
                "    },\n" +
                "    \"INTERNAL_PATIENT_ID\": [\n" +
                "      {\n" +
                "        \"ID\": \"12345678-ADFGHJY-0987654-NHYI898\",\n" +
                "        \"IDENTIFIER_TYPE\": \"CARD_SERIAL_NUMBER\",\n" +
                "        \"ASSIGNING_AUTHORITY\": \"CARD_REGISTRY\",\n" +
                "        \"ASSIGNING_FACILITY\": \"10829\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"ID\": \"123456788\",\n" +
                "        \"IDENTIFIER_TYPE\": \"HEI_NUMBER\",\n" +
                "        \"ASSIGNING_AUTHORITY\": \"MCH\",\n" +
                "        \"ASSIGNING_FACILITY\": \"10829\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"ID\": \"1234567808\",\n" +
                "        \"IDENTIFIER_TYPE\": \"CCC_NUMBER\",\n" +
                "        \"ASSIGNING_AUTHORITY\": \"CCC\",\n" +
                "        \"ASSIGNING_FACILITY\": \"10829\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"ID\": \"00108\",\n" +
                "        \"IDENTIFIER_TYPE\": \"HTS_NUMBER\",\n" +
                "        \"ASSIGNING_AUTHORITY\": \"HTS\",\n" +
                "        \"ASSIGNING_FACILITY\": \"10829\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"ID\": \"ABC56778\",\n" +
                "        \"IDENTIFIER_TYPE\": \"ANC_NUMBER\",\n" +
                "        \"ASSIGNING_AUTHORITY\": \"ANC\",\n" +
                "        \"ASSIGNING_FACILITY\": \"10829\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"PATIENT_NAME\": {\n" +
                "      \"FIRST_NAME\": \"SERILA\",\n" +
                "      \"MIDDLE_NAME\": \"SARAH\",\n" +
                "      \"LAST_NAME\": \"SORROW\"\n" +
                "    },\n" +
                "    \"DATE_OF_BIRTH\": \"20111111\",\n" +
                "    \"DATE_OF_BIRTH_PRECISION\": \"EXACT\",\n" +
                "    \"SEX\": \"F\",\n" +
                "    \"DEATH_DATE\": \"\",\n" +
                "    \"DEATH_INDICATOR\": \"N\",\n" +
                "    \"PATIENT_ADDRESS\": {\n" +
                "      \"PHYSICAL_ADDRESS\": {\n" +
                "        \"VILLAGE\": \"KWAKIMANI\",\n" +
                "        \"WARD\": \"KIMANINI\",\n" +
                "        \"SUB_COUNTY\": \"KIAMBU EAST\",\n" +
                "        \"COUNTY\": \"KIAMBU\",\n" +
                "        \"NEAREST_LANDMARK\": \"KIAMBU EAST\"\n" +
                "      },\n" +
                "      \"POSTAL_ADDRESS\": \"789 KIAMBU\"\n" +
                "    },\n" +
                "    \"PHONE_NUMBER\": \"254720278655\",\n" +
                "    \"MARITAL_STATUS\": \"\",\n" +
                "    \"MOTHER_DETAILS\": {\n" +
                "      \"MOTHER_NAME\": {\n" +
                "        \"FIRST_NAME\": \"WAMUYU\",\n" +
                "        \"MIDDLE_NAME\": \"MARY\",\n" +
                "        \"LAST_NAME\": \"WAITHERA\"\n" +
                "      },\n" +
                "      \"MOTHER_IDENTIFIER\": [\n" +
                "        {\n" +
                "          \"ID\": \"1234567\",\n" +
                "          \"IDENTIFIER_TYPE\": \"NATIONAL_ID\",\n" +
                "          \"ASSIGNING_AUTHORITY\": \"GOK\",\n" +
                "          \"ASSIGNING_FACILITY\": \"\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"ID\": \"12345678\",\n" +
                "          \"IDENTIFIER_TYPE\": \"NHIF\",\n" +
                "          \"ASSIGNING_AUTHORITY\": \"NHIF\",\n" +
                "          \"ASSIGNING_FACILITY\": \"\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"ID\": \"12345-67890\",\n" +
                "          \"IDENTIFIER_TYPE\": \"CCC_NUMBER\",\n" +
                "          \"ASSIGNING_AUTHORITY\": \"CCC\",\n" +
                "          \"ASSIGNING_FACILITY\": \"10829\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"ID\": \"12345678\",\n" +
                "          \"IDENTIFIER_TYPE\": \"PMTCT_NUMBER\",\n" +
                "          \"ASSIGNING_AUTHORITY\": \"PMTCT\",\n" +
                "          \"ASSIGNING_FACILITY\": \"10829\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  },\n" +
                "  \"NEXT_OF_KIN\": [\n" +
                "    {\n" +
                "      \"NOK_NAME\": {\n" +
                "        \"FIRST_NAME\": \"WAIGURU\",\n" +
                "        \"MIDDLE_NAME\": \"KIMUTAI\",\n" +
                "        \"LAST_NAME\": \"WANJOKI\"\n" +
                "      },\n" +
                "      \"RELATIONSHIP\": \"**AS DEFINED IN GREENCARD\",\n" +
                "      \"ADDRESS\": \"4678 KIAMBU\",\n" +
                "      \"PHONE_NUMBER\": \"25489767899\",\n" +
                "      \"SEX\": \"F\",\n" +
                "      \"DATE_OF_BIRTH\": \"19871022\",\n" +
                "      \"CONTACT_ROLE\": \"T\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"HIV_TEST\": [\n" +
                "    {\n" +
                "      \"DATE\": \"20180101\",\n" +
                "      \"RESULT\": \"POSITIVE\",\n" +
                "      \"TYPE\": \"CONFIRMATORY\",\n" +
                "      \"FACILITY\": \"10829\",\n" +
                "      \"STRATEGY\": \"HP\",\n" +
                "      \"PROVIDER_DETAILS\": {\n" +
                "        \"NAME\": \"MATTHEW NJOROGE, MD\",\n" +
                "        \"ID\": \"12345-67890-abcde\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"IMMUNIZATION\": [\n" +
                "    {\n" +
                "      \"NAME\": \"OPV_AT_BIRTH\",\n" +
                "      \"DATE_ADMINISTERED\": \"20180101\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"CARD_DETAILS\": {\n" +
                "    \"STATUS\": \"ACTIVE/INACTIVE\",\n" +
                "    \"REASON\": \"LOST/DEATH/DAMAGED\",\n" +
                "    \"LAST_UPDATED\": \"20180101\",\n" +
                "    \"LAST_UPDATED_FACILITY\": \"10829\"\n" +
                "  }\n" +
                "}";
    }

}
