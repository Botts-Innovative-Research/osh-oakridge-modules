package com.botts.impl.service.dailyfile;

import org.sensorhub.api.module.IModuleBase;
import org.sensorhub.api.module.ModuleConfigBase;
import org.sensorhub.impl.module.JarModuleProvider;

/** Makes the daily-file journal available to the OSH module registry and Admin UI. */
public class Descriptor extends JarModuleProvider {

    @Override
    public Class<? extends IModuleBase<?>> getModuleClass() {
        return DailyFileJournalService.class;
    }

    @Override
    public Class<? extends ModuleConfigBase> getModuleConfigClass() {
        return DailyFileJournalServiceConfig.class;
    }
}
