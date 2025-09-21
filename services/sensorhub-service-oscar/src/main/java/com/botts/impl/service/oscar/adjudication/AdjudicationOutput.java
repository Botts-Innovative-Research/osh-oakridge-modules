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

package com.botts.impl.service.oscar.adjudication;

import com.botts.impl.service.oscar.OSCARSystem;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;

import java.util.Arrays;

public class AdjudicationOutput extends AbstractSensorOutput<OSCARSystem> {

    public static final String NAME = "adjudication";
    public static final String LABEL = "Adjudication";
    public static final String DESCRIPTION = "";

    DataComponent recordStructure;
    DataEncoding recordEncoding;
    SWEHelper fac;

    public AdjudicationOutput(OSCARSystem parentSensor) {
        super(NAME, parentSensor);

        fac = new SWEHelper();

        this.recordStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("sampleTime", fac.createTime().asSamplingTimeIsoUTC())
                .addField("username", fac.createText()
                        .label("Username")
                        .definition(SWEHelper.getPropertyUri("Username"))
                        .build())
                .addField("feedback", fac.createText()
                        .label("Feedback")
                        .definition(SWEHelper.getPropertyUri("Feedback"))
                        .build())
                .addField("adjudicationCode", fac.createText()
                        .label("Adjudication Code")
                        .definition(SWEHelper.getPropertyUri("AdjudicationCode"))
                        .build())
                .addField("isotopes", fac.createText()
                        .label("Isotopes")
                        .definition(SWEHelper.getPropertyUri("Isotopes"))
                        .build())
                .addField("secondaryInspectionStatus", fac.createText()
                        .label("Secondary Inspection Status")
                        .definition(SWEHelper.getPropertyUri("SecondaryInspectionStatus"))
                        .build())
                .addField("filePaths", fac.createText()
                        .label("File Paths")
                        .definition(SWEHelper.getPropertyUri("FilePaths"))
                        .build())
                .addField("occupancyId", fac.createText()
                        .label("Occupancy ID")
                        .definition(SWEHelper.getPropertyUri("OccupancyID"))
                        .build())
                .addField("alarmingSystemUid", fac.createText()
                        .label("Alarming System UID")
                        .definition(SWEHelper.getPropertyUri("AlarmingSystemUid"))
                        .build())
                .addField("vehicleId", fac.createText()
                        .label("Vehicle ID")
                        .definition(SWEHelper.getPropertyUri("vehicleId"))
                        .build())
                .build();

        this.recordEncoding = new TextEncodingImpl();
    }

    public void setData(String username, String feedback, String adjudicationCode, String[] isotopes, String[] filePaths, int occupancyId, String alarmSystemUID, String vehicleID) {
        long timeMillis = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ? recordStructure.createDataBlock() : latestRecord.renew();

        dataBlock.setDoubleValue(0, timeMillis/1000d);
        dataBlock.setStringValue(1, username);
        dataBlock.setStringValue(2, feedback);
        dataBlock.setStringValue(3, adjudicationCode);
        dataBlock.setStringValue(4, Arrays.toString(isotopes));
        dataBlock.setStringValue(5, Arrays.toString(filePaths));
        dataBlock.setIntValue(5, occupancyId);
        dataBlock.setStringValue(6, alarmSystemUID);
        dataBlock.setStringValue(7, vehicleID);


        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }

    @Override
    public DataComponent getRecordDescription() {
        return recordStructure;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return recordEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }
}
