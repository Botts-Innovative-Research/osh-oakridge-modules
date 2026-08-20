package com.botts.impl.service.oscar.federation.identity;

/** Supplies the canonical UID of the OSCAR system hosted by this OSH node. */
public interface LocalOscarIdentityProvider
{
    String getLocalOscarSystemUid();
}
