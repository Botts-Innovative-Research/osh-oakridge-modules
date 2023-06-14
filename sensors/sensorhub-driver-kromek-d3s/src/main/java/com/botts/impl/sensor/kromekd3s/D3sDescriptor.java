package com.botts.impl.sensor.kromekd3s;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

public class D3sDescriptor extends JarModuleProvider implements IModuleProvider {

    @Override
    public Class<? extends IModule<?>> getModuleClass(){return D3sSensor.class;}

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {return D3sConfig.class;}
}
