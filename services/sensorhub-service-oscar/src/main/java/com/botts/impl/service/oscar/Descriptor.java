package com.botts.impl.service.oscar;

import org.sensorhub.api.module.IModuleBase;
import org.sensorhub.api.module.ModuleConfigBase;
import org.sensorhub.impl.module.JarModuleProvider;

public class Descriptor extends JarModuleProvider {

    @Override
    public Class<? extends IModuleBase<?>> getModuleClass() {
        return OSCARServiceModule.class;
    }

    @Override
    public Class<? extends ModuleConfigBase> getModuleConfigClass() {
        return OSCARServiceConfig.class;
    }

}
