/*******************************************************************************

  The contents of this file are subject to the Mozilla Public License, v. 2.0.
  If a copy of the MPL was not distributed with this file, You can obtain one
  at http://mozilla.org/MPL/2.0/.

  Software distributed under the License is distributed on an "AS IS" basis,
  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
  for the specific language governing rights and limitations under the License.

  The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
  Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.process.d5.occupancy;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.processing.AbstractProcessProvider;

public class ProcessDescriptors extends AbstractProcessProvider
{
    
    public ProcessDescriptors()
    {
        addImpl(D5OutputToOccupancy.INFO);
    }


    @Override
    public String getModuleName() {
        return "D5 Occupancy Data Process";
    }

    @Override
    public String getModuleDescription()
    {
        return "Processing module configured for storing occupancy data from an OSH database";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return D5OccupancyProcessModule.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return D5OccupancyProcessConfig.class;
    }

}
