package org.openmrs.module.facespsmart.jsonvalidator.mapper;

import org.openmrs.Concept;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by rugute on 5/24/18.
 */
public class SmartCardHivTest {
    private Concept result;
    private String facility;
    private Concept strategy;
    private Date dateTested;
    private String type;
    private String providerName;
    private String providerId;

    public SmartCardHivTest(Concept result, String facility, Concept strategy, Date dateTested, String type, String providerName, String providerId) {
        this.result = result;
        this.facility = facility;
        this.strategy = strategy;
        this.dateTested = dateTested;
        this.type = type;
        this.providerName = providerName;
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Concept getResult() {
        return result;
    }

    public void setResult(Concept result) {
        this.result = result;
    }

    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public Concept getStrategy() {
        return strategy;
    }

    public void setStrategy(Concept strategy) {
        this.strategy = strategy;
    }

    public Date getDateTested() {
        return dateTested;
    }

    public void setDateTested(Date dateTested) {
        this.dateTested = dateTested;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean equals(Object obj) {

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof SmartCardHivTest)) return false;
        SmartCardHivTest o = (SmartCardHivTest) obj;
        return o.getFacility().equals(this.getFacility())
                && df.format(o.getDateTested()).equals(df.format(this.getDateTested()))
                && o.getResult().equals(this.getResult())
                && o.getStrategy().equals(this.getStrategy())
                && o.getType().equals(this.getType())
                && o.getProviderId().equals(this.getProviderId());
    }
}
