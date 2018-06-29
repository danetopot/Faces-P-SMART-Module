package org.openmrs.module.facespsmart;

import org.openmrs.BaseOpenmrsData;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Created by rugute on 5/24/18.
 */
public class PsmartStore extends BaseOpenmrsData {

    private Integer id;
    private String uuid;
    private String shr;
    private Timestamp dateCreated;
    private String status;
    private Timestamp statusDate;
    private String addendum;

    public PsmartStore() {
        prePersist();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getShr() {
        return shr;
    }

    public void setShr(String shr) {
        this.shr = shr;
    }

    @Override
    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(Timestamp statusDate) {
        this.statusDate = statusDate;
    }

    public String getAddendum() {
        return addendum;
    }

    public void setAddendum(String addendum) {
        this.addendum = addendum;
    }

    public void prePersist(){

        if(null == getUuid())
            setUuid(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return "PsmartStore{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", shr='" + shr + '\'' +
                ", dateCreated=" + dateCreated +
                ", status='" + status + '\'' +
                ", statusDate=" + statusDate +
                ", addendum='" + addendum + '\'' +
                '}';
    }
}
