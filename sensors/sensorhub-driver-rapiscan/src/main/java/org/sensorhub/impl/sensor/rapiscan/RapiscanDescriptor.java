/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.rapiscan;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

public class RapiscanDescriptor extends JarModuleProvider implements IModuleProvider {

    @Override
    public Class<? extends IModule<?>> getModuleClass() {
        return RapiscanSensor.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return RapiscanConfig.class;
    }
}
