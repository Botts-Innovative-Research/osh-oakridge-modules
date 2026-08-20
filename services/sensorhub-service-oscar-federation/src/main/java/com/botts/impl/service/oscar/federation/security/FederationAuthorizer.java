package com.botts.impl.service.oscar.federation.security;

import javax.servlet.http.HttpServletRequest;

public interface FederationAuthorizer
{
    boolean canManageNodes(HttpServletRequest request);
    boolean canAccessNode(HttpServletRequest request, String targetUid);
}
