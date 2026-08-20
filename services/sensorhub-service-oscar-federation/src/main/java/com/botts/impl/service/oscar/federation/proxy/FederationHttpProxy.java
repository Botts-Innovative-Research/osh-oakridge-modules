package com.botts.impl.service.oscar.federation.proxy;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.botts.impl.service.oscar.federation.model.RemoteNodeConnection;

public interface FederationHttpProxy
{
    void proxy(RemoteNodeConnection node, String upstreamPath,
            HttpServletRequest request, HttpServletResponse response) throws IOException;
}
