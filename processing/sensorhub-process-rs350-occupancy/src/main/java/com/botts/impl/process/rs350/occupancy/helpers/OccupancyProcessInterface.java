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
import com.botts.impl.process.rs350.occupancy.Rs350OutputToOccupancy;
import com.botts.impl.utils.n42.RadAlarmCategoryCodeSimpleType;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.event.IEventHandler;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.processing.ProcessingException;
import org.sensorhub.impl.event.BasicEventHandler;
import org.sensorhub.impl.processing.SMLProcessImpl;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataArrayImpl;
import org.vast.process.DataQueue;
import org.vast.process.ProcessException;
import org.vast.sensorML.SMLHelper;

import java.util.ArrayList;
import java.util.List;

public class OccupancyProcessInterface extends OccupancyOutput<IDataProducer> {
    final IProcessModule<?> parentProcess;
    final IEventHandler eventHandler;
    static RADHelper radHelper = new RADHelper();
    static String OUTPUT_NAME = OccupancyOutput.NAME;
    DataComponent outputDef;
    DataEncoding outputEncoding;
    DataBlock lastRecord;
    Occupancy lastOccupancy;
    long lastRecordTime = Long.MIN_VALUE;
    double avgSamplingPeriod = 1.0;
    int avgSampleCount = 0;

    public static class OccupancyExtended extends Occupancy {
        private static final Logger logger = LoggerFactory.getLogger(OccupancyExtended.class);
        protected RadAlarmCategoryCodeSimpleType alarmCategory = RadAlarmCategoryCodeSimpleType.OTHER;

        public RadAlarmCategoryCodeSimpleType getAlarmCategory() { return alarmCategory; }

        public static class Builder extends Occupancy.Builder {

            public Builder() {
                instance = new OccupancyExtended();
            }

            @Override
            public OccupancyExtended build() {
                return (OccupancyExtended) instance;
            }

            public Occupancy.Builder alarmCategory(RadAlarmCategoryCodeSimpleType alarmCategory) {
                ((OccupancyExtended) instance).alarmCategory = alarmCategory;
                return this;
            }
        }

        // TODO This adds the alarm category code. Incorporate this into OccupancyOutput itself.
        public static DataBlock fromOccupancy(OccupancyExtended occupancy) {
            OccupancyProcessInterface output;
            try {
                output = new OccupancyProcessInterface(new SMLProcessImpl(), null, null);
            } catch (ProcessingException e) {
                logger.error("Could not generate extended occupancy output", e);
                return null;
            }
            DataComponent resultStructure = output.getRecordDescription();
            DataBlock dataBlock = resultStructure.createDataBlock();
            dataBlock.updateAtomCount();
            resultStructure.setData(dataBlock);

            int index = 0;

            dataBlock.setDoubleValue(index++, occupancy.getSamplingTime());
            dataBlock.setIntValue(index++, occupancy.getOccupancyCount());
            dataBlock.setDoubleValue(index++, occupancy.getStartTime());
            dataBlock.setDoubleValue(index++, occupancy.getEndTime());
            dataBlock.setDoubleValue(index++, occupancy.getNeutronBackground());
            dataBlock.setBooleanValue(index++, occupancy.hasGammaAlarm());
            dataBlock.setBooleanValue(index++, occupancy.hasNeutronAlarm());
            dataBlock.setIntValue(index++, occupancy.getMaxGammaCount());
            dataBlock.setIntValue(index++, occupancy.getMaxNeutronCount());

            int cmdIdsCount = occupancy.getAdjudicatedIds().size();
            dataBlock.setDoubleValue(index++, cmdIdsCount);

            var adjIdsArray = ((DataArrayImpl) resultStructure.getComponent("adjudicatedIds"));

            if (cmdIdsCount > 0) {
                adjIdsArray.updateSize();
                dataBlock.updateAtomCount();

                for (int i = 0; i < occupancy.getAdjudicatedIds().size(); i++) {
                    dataBlock.setStringValue(index++, occupancy.getAdjudicatedIds().get(i));
                }
            }

            int filePathsCount = occupancy.getVideoPaths().size();
            dataBlock.setDoubleValue(index++, filePathsCount);

            var filePathsArray = ((DataArrayImpl) resultStructure.getComponent("videoPaths"));

            if (filePathsCount > 0) {
                filePathsArray.updateSize();
                dataBlock.updateAtomCount();

                for (int i = 0; i < occupancy.getVideoPaths().size(); i++) {
                    dataBlock.setStringValue(index++, occupancy.getVideoPaths().get(i));
                }
            }

            // Add alarm category code
            // Currently using "OTHER" as the default for non-alarming.
            dataBlock.setStringValue(index, occupancy.getAlarmCategory().value());

            return dataBlock;
        }

        public static OccupancyExtended toOccupancy(DataBlock dataBlock) {
            int index = 0;

            var samplingTime = dataBlock.getDoubleValue(index++);
            var occupancyCount = dataBlock.getIntValue(index++);
            var startTime = dataBlock.getDoubleValue(index++);
            var endTime = dataBlock.getDoubleValue(index++);
            var neutronBackground = dataBlock.getDoubleValue(index++);
            var gammaAlarm = dataBlock.getBooleanValue(index++);
            var neutronAlarm = dataBlock.getBooleanValue(index++);
            var maxGammaCount = dataBlock.getIntValue(index++);
            var maxNeutronCount = dataBlock.getIntValue(index++);
            var cmdIdsCount = dataBlock.getIntValue(index++);

            List<String> cmdIds = new ArrayList<>();
            for (int i = 0; i < cmdIdsCount; i++)
                cmdIds.add(dataBlock.getStringValue(index++));

            var videoPathCount = dataBlock.getIntValue(index++);

            List<String> videoPaths = new ArrayList<>();
            for (int i = 0; i < videoPathCount; i++)
                videoPaths.add(dataBlock.getStringValue(index++));

            var catCode = dataBlock.getStringValue(index);

            return (OccupancyExtended) new OccupancyExtended.Builder()
                    .alarmCategory(RadAlarmCategoryCodeSimpleType.fromValue(catCode))
                    .samplingTime(samplingTime)
                    .occupancyCount(occupancyCount)
                    .startTime(startTime)
                    .endTime(endTime)
                    .neutronBackground(neutronBackground)
                    .maxGammaCount(maxGammaCount)
                    .maxNeutronCount(maxNeutronCount)
                    .gammaAlarm(gammaAlarm)
                    .neutronAlarm(neutronAlarm)
                    .adjudicatedIds(cmdIds)
                    .videoPaths(videoPaths)
                    .build();
        }
    }

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
                eventHandler.publish(new DataEvent(System.currentTimeMillis(), OccupancyProcessInterface.this, dataBlock));
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
    public OccupancyProcessInterface(IProcessModule<?> parentProcess, AbstractSWEIdentifiable outputDescriptor, DataEncoding encoding) throws ProcessingException
    {
        // TODO THIS OUTPUT ADDS AN ALARM CATEGORY CODE! ADD THIS CODE TO THE REGULAR OCCUPANCY OUTPUT!
        super(parentProcess);
        var catCode = radHelper.createAlarmCatCode();
        dataStruct.addField(catCode.getName(), catCode);
        this.parentProcess = parentProcess;
        this.eventHandler = new BasicEventHandler();

        if (outputDescriptor != null) {
            this.outputDef = SMLHelper.getIOComponent(outputDescriptor);
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
