package org.openmrs.module.facespsmart;

import org.openmrs.BaseOpenmrsObject;

import java.io.Serializable;

/**
 * Created by rugute on 5/24/18.
 */
public class EndPoint extends BaseOpenmrsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

}