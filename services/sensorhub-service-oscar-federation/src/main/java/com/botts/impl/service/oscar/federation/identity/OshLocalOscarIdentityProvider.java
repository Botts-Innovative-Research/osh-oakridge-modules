package com.botts.impl.service.oscar.federation.identity;

import com.botts.impl.service.oscar.OSCARServiceModule;
import org.sensorhub.impl.module.ModuleRegistry;

public class OshLocalOscarIdentityProvider implements LocalOscarIdentityProvider
{
    private final ModuleRegistry registry;

    public OshLocalOscarIdentityProvider(ModuleRegistry registry)
    {
        this.registry = registry;
    }

    @Override
    public String getLocalOscarSystemUid()
    {
        OSCARServiceModule module = registry.getModuleByType(OSCARServiceModule.class);
        if (module == null || module.getOSCARSystem() == null)
            throw new IllegalStateException("OSCAR service module is not available");
        return module.getOSCARSystem().getUniqueIdentifier();
    }
}
