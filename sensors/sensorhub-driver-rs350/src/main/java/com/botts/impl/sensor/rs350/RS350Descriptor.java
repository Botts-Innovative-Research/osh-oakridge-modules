/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

public class RS350Descriptor extends JarModuleProvider implements IModuleProvider {

    @Override
    public Class<? extends IModule<?>> getModuleClass(){return RS350Sensor.class;}

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {return RS350Config.class;}
}
