package org.openmrs.module.facespsmart.jsonvalidator.mapper;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.facespsmart.openmrsUtils.Utils;

/**
 * Created by rugute on 5/25/18.
 */
public class PsmartAuthentication {


    public static ObjectNode authenticateUser(String userName, String pwd) {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode node = factory.objectNode();
        try {
            Context.authenticate(userName, pwd);
            User authenticatedUser = Context.getAuthenticatedUser();
            if (authenticatedUser != null) {

                node.put("STATUS", "true");
                node.put("DISPLAYNAME", authenticatedUser.getDisplayString());
                node.put("FACILITY", Utils.getDefaultLocationMflCode(Utils.getDefaultLocation()));
            }

        } catch (Exception e) {
            node.put("STATUS", "false");
            node.put("DISPLAYNAME", "");
            node.put("FACILITY", Utils.getDefaultLocationMflCode(Utils.getDefaultLocation()));
        }
        return node;
    }
}
