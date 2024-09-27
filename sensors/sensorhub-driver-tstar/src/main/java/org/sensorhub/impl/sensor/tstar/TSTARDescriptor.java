package org.sensorhub.impl.sensor.tstar;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


public class TSTARDescriptor extends JarModuleProvider implements IModuleProvider
{
    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return TSTARDriver.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return TSTARConfig.class;
    }
}