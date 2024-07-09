/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.rapiscan;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.processing.AbstractProcessProvider;
import org.sensorhub.process.rapiscan.helpers.RapiscanProcessConfig;
import org.sensorhub.process.rapiscan.helpers.RapiscanProcessModule;

public class ProcessDescriptors extends AbstractProcessProvider
{
    
    public ProcessDescriptors()
    {
        addImpl(AlarmRecorder.INFO);
    }


    @Override
    public String getModuleName() {
        return "Occupancy Alarm Database Process";
    }

    @Override
    public String getModuleDescription()
    {
        return "Processing module configured for storing occupancy data from an OSH database";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return RapiscanProcessModule.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return RapiscanProcessConfig.class;
    }

}
