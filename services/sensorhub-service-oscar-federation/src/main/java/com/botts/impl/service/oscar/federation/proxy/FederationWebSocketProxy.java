package com.botts.impl.service.oscar.federation.proxy;

import com.botts.impl.service.oscar.federation.model.RemoteNodeConnection;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;

public interface FederationWebSocketProxy
{
    Object createMqttBridge(RemoteNodeConnection node,
            ServletUpgradeRequest request, ServletUpgradeResponse response);
}
