package com.botts.impl.service.oscar.federation.security;

import javax.servlet.http.HttpServletRequest;

/** Uses the existing OSH HTTP realm for gateway and administrator authorization. */
public class AuthenticatedUserAuthorizer implements FederationAuthorizer
{
    private final String administratorRole;

    public AuthenticatedUserAuthorizer(String administratorRole)
    {
        this.administratorRole = administratorRole;
    }

    @Override
    public boolean canManageNodes(HttpServletRequest request)
    {
        return request.getRemoteUser() != null && request.isUserInRole(administratorRole);
    }

    @Override
    public boolean canAccessNode(HttpServletRequest request, String targetUid)
    {
        return request.getRemoteUser() != null;
    }
}
