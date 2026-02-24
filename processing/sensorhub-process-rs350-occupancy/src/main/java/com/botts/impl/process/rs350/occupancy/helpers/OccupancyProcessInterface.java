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

package com.botts.impl.process.rs350.occupancy.helpers;

import com.botts.impl.process.rs350.occupancy.Rs350OccupancyProcessModule;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.event.IEventHandler;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.processing.ProcessingException;
import org.sensorhub.impl.event.BasicEventHandler;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.vast.process.DataQueue;
import org.vast.process.ProcessException;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Asserts;

public class OccupancyProcessInterface extends OccupancyOutput {
    final IProcessModule<?> parentProcess;
    final IEventHandler eventHandler;
    DataComponent outputDef;
    DataEncoding outputEncoding;
    DataBlock lastRecord;
    Occupancy lastOccupancy;
    long lastRecordTime = Long.MIN_VALUE;
    double avgSamplingPeriod = 1.0;
    int avgSampleCount = 0;

    /**
     * Output queue used to publish process outputs as data events
     */
    protected DataQueue outputQueue = new DataQueue()
    {
        @Override
        public synchronized void publishData()
        {
            if (!sourceComponent.hasData()) {
                return;
            }
            DataBlock data = sourceComponent.getData();
            Occupancy occupancy = Occupancy.toOccupancy(data);

            if (lastOccupancy == null || lastOccupancy.getSamplingTime() != occupancy.getSamplingTime()) {
                lastOccupancy = occupancy;
                // This handles publishing too
                setData(occupancy);
            }
        }
    };

    /**
     * Output interface to facilitate connection between process outputs and output queue
     * @param parentProcess OSH process module
     * @param outputDescriptor output to connect to data queue
     * @param encoding data encoding retrieved from data stream info
     * @throws ProcessingException if unable to connect output and process
     */
    public OccupancyProcessInterface(Rs350OccupancyProcessModule parentProcess, AbstractSWEIdentifiable outputDescriptor, DataEncoding encoding) throws ProcessingException
    {
        super(parentProcess);
        this.parentProcess = parentProcess;
        this.outputDef = SMLHelper.getIOComponent(outputDescriptor);
        if(encoding != null)
            this.outputEncoding = encoding;
        else
            this.outputEncoding = SMLHelper.getIOEncoding(outputDescriptor);

        this.eventHandler = new BasicEventHandler();

        try
        {
            DataComponent execOutput = parentProcess.wrapperProcess.getOutputComponent(outputDef.getName());
            parentProcess.wrapperProcess.connect(execOutput, outputQueue);
        }
        catch (ProcessException e)
        {
            throw new ProcessingException("Error while connecting output " + outputDef.getName(), e);
        }
    }

    @Override
    public IDataProducer getParentProducer()
    {
        return parentProcess;
    }


    @Override
    public String getName()
    {
        return outputDef.getName();
    }


    @Override
    public boolean isEnabled()
    {
        return true;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return outputDef;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return outputEncoding;
    }


    @Override
    public DataBlock getLatestRecord()
    {
        return lastRecord;
    }


    @Override
    public long getLatestRecordTime()
    {
        return lastRecordTime;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return avgSamplingPeriod;
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }

}
