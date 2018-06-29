package org.openmrs.module.facespsmart.jsonvalidator.mapper;

import org.openmrs.Concept;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by rugute on 5/24/18.
 */
public class ImmunizationWrapper {
    private Concept vaccine;
    private Integer sequenceNumber;
    private Date vaccineDate;

    public ImmunizationWrapper() {
    }

    public ImmunizationWrapper(Concept vaccine, Integer sequenceNumber, Date vaccineDate) {
        this.vaccine = vaccine;
        this.sequenceNumber = sequenceNumber;
        this.vaccineDate = vaccineDate;
    }

    public Concept getVaccine() {
        return vaccine;
    }

    public void setVaccine(Concept vaccine) {
        this.vaccine = vaccine;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Date getVaccineDate() {
        return vaccineDate;
    }

    public void setVaccineDate(Date vaccineDate) {
        this.vaccineDate = vaccineDate;
    }

    public boolean equals(Object obj) {

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof ImmunizationWrapper)) return false;
        ImmunizationWrapper o = (ImmunizationWrapper) obj;
        return o.getVaccine().equals(this.getVaccine()) && df.format(o.getVaccineDate()).equals(df.format(this.getVaccineDate())) && o.getSequenceNumber() == this.getSequenceNumber();
    }

}
