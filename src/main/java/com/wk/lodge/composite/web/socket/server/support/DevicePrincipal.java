
package com.wk.lodge.composite.web.socket.server.support;

import java.security.Principal;

public class DevicePrincipal implements Principal {

    public String name;

    public DevicePrincipal(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}
