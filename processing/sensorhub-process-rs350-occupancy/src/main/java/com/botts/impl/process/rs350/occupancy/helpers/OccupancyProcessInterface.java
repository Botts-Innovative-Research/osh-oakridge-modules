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
import org.sensorhub.impl.utils.rad.model.OccupancyExtended;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.vast.process.DataQueue;
import org.vast.process.ProcessException;
import org.vast.sensorML.SMLHelper;

public class OccupancyProcessInterface extends OccupancyOutput<IDataProducer> {
    final IProcessModule<?> parentProcess;
    final IEventHandler eventHandler;
    static String OUTPUT_NAME = OccupancyOutput.NAME;
    DataComponent outputDef;
    DataEncoding outputEncoding;
    DataBlock lastRecord;
    Occupancy lastOccupancy;
    long lastRecordTime = Long.MIN_VALUE;
    double avgSamplingPeriod = 1.0;
    volatile boolean doPublish = false;
    int avgSampleCount = 0;

    public void doPublish() {
        doPublish = true;
    }

    /**
     * Output queue used to publish process outputs as data events. Reads incoming
     * datablocks as {@link OccupancyExtended} so the RS350-specific
     * {@code alarmCategoryCode} field is preserved end-to-end.
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
            OccupancyExtended occupancy = OccupancyExtended.toOccupancy(data);

            if (lastOccupancy == null || lastOccupancy.getSamplingTime() != occupancy.getSamplingTime()) {
                lastOccupancy = occupancy;
                // This handles publishing too (parent setData → augmentPublish,
                // which we override below to serialize extended datablocks).
                setData(occupancy);
                eventHandler.publish(new DataEvent(System.currentTimeMillis(), OccupancyProcessInterface.this, dataBlock));
                doPublish = false;
            }
        }
    };

    /**
     * Output interface to facilitate connection between process outputs and output queue.
     * <p>
     * Overrides the inherited {@link OccupancyOutput#dataStruct} with the extended
     * record schema (base occupancy fields + {@code alarmCategoryCode}) so that both
     * the published datablock and the registered datastream schema carry the alarm
     * category string reported by the RS350 driver.
     *
     * @param parentProcess OSH process module
     * @param outputDescriptor output to connect to data queue
     * @param encoding data encoding retrieved from data stream info
     * @throws ProcessingException if unable to connect output and process
     */
    public OccupancyProcessInterface(IProcessModule<?> parentProcess, AbstractSWEIdentifiable outputDescriptor, DataEncoding encoding) throws ProcessingException
    {
        super(parentProcess);
        // Replace the base (16-field) record with the extended (17-field) record so
        // the augmentPublish path serializes datablocks that match the schema
        // persisted for the datastream. See OccupancyExtended.createExtendedRecordStructure.
        this.dataStruct = OccupancyExtended.createExtendedRecordStructure();
        this.parentProcess = parentProcess;
        this.eventHandler = new BasicEventHandler();

        if (outputDescriptor != null) {
            this.outputDef = SMLHelper.getIOComponent(outputDescriptor);
            lastOccupancy = OccupancyExtended.toOccupancy(outputDef.createDataBlock());
            if (encoding != null)
                this.outputEncoding = encoding;
            else
                this.outputEncoding = SMLHelper.getIOEncoding(outputDescriptor);


            try {
                if (parentProcess instanceof Rs350OccupancyProcessModule rs350Module) {
                    DataComponent execOutput = rs350Module.wrapperProcess.getOutputComponent(outputDef.getName());
                    rs350Module.wrapperProcess.connect(execOutput, outputQueue);
                }
            } catch (ProcessException e) {
                throw new ProcessingException("Error while connecting output " + outputDef.getName(), e);
            }
        }
    }

    /**
     * Override the parent's publish path so datablocks are serialized via
     * {@link OccupancyExtended#fromOccupancy(OccupancyExtended)} — the base
     * {@link OccupancyOutput#augmentPublish(Occupancy)} uses
     * {@link Occupancy#fromOccupancy(Occupancy)} which produces a base-schema
     * datablock that's missing the trailing {@code alarmCategoryCode} field and
     * therefore does not match the datastream schema registered for the RS350
     * occupancy output.
     */
    @Override
    protected void augmentPublish(Occupancy occupancy) {
        augmentOccupancy(occupancy);

        if (occupancy instanceof OccupancyExtended extended) {
            dataBlock = OccupancyExtended.fromOccupancy(extended);
        } else {
            // Wrap any plain Occupancy as an extended one with an empty category so
            // the datablock still matches the extended schema.
            OccupancyExtended wrapper = (OccupancyExtended) new OccupancyExtended.Builder()
                    .alarmCategory("")
                    .samplingTime(occupancy.getSamplingTime())
                    .occupancyCount(occupancy.getOccupancyCount())
                    .startTime(occupancy.getStartTime())
                    .endTime(occupancy.getEndTime())
                    .neutronBackground(occupancy.getNeutronBackground())
                    .gammaAlarm(occupancy.hasGammaAlarm())
                    .neutronAlarm(occupancy.hasNeutronAlarm())
                    .maxGammaCount(occupancy.getMaxGammaCount())
                    .maxNeutronCount(occupancy.getMaxNeutronCount())
                    .adjudicatedIds(occupancy.getAdjudicatedIds())
                    .videoPaths(occupancy.getVideoPaths())
                    .webIdObsIds(occupancy.getWebIdObsIds())
                    .build();
            dataBlock = OccupancyExtended.fromOccupancy(wrapper);
        }

        dataStruct.setData(dataBlock);
        latestRecord = dataBlock;

        long now = System.currentTimeMillis();
        updateSamplingPeriod(now);
        latestRecordTime = now;

        eventHandler.publish(new DataEvent(now, this, dataBlock));
    }

    @Override
    public IDataProducer getParentProducer()
    {
        return parentProcess;
    }


    @Override
    public String getName()
    {
        return OUTPUT_NAME;
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
