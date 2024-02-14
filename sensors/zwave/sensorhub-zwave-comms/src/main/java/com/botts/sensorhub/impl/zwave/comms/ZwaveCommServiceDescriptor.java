/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * Module descriptor, allows OpenSensorHub to identify and load the module.
 */
public class ZwaveCommServiceDescriptor extends JarModuleProvider implements IModuleProvider
{
    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return ZwaveCommService.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return ZwaveCommServiceConfig.class;
    }

}