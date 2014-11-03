
package com.wk.lodge.composite.web.socket.server.support;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

public class CompositeHandshakeHandler extends DefaultHandshakeHandler {

    public CompositeHandshakeHandler() {
        super();
    }

    public CompositeHandshakeHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
        super(requestUpgradeStrategy);
    }

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Principal principal = new DevicePrincipal(UUID.randomUUID().toString());
        return principal;
    }
}
