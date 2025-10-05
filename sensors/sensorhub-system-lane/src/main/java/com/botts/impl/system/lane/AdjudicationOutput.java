
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

package com.botts.impl.system.lane;


import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;

import java.util.Arrays;

public class AdjudicationOutput extends AbstractSensorOutput<LaneSystem> {


    // TODO: Should this be resolved via an ontology repo?
//    {code: 0, label: "", group: ""},
//    {code: 1, label: "Code 1: Contraband Found", group: "Real Alarm"},
//    {code: 2, label: "Code 2: Other", group: "Real Alarm"},
//    {code: 3, label: "Code 3: Medical Isotope Found", group: "Innocent Alarm"},
//    {code: 4, label: "Code 4: NORM Found", group: "Innocent Alarm"},
//    {code: 5, label: "Code 5: Declared Shipment of Radioactive Material", group: "Innocent Alarm"},
//    {code: 6, label: "Code 6: Physical Inspection Negative", group: "False Alarm"},
//    {code: 7, label: "Code 7: RIID/ASP Indicates Background Only", group: "False Alarm"},
//    {code: 8, label: "Code 8: Other", group: "False Alarm"},
//    {code: 9, label: "Code 9: Authorized Test, Maintenance, or Training Activity", group: "Test/Maintenance"},
//    {code: 10, label: "Code 10: Unauthorized Activity", group: "Tamper/Fault"},
//    {code: 11, label: "Code 11: Other", group: "Other"}

    public static final String NAME = "adjudicationOutput";
    public static final String LABEL = "Adjudication Output";
    public static final String DESCRIPTION = "Historical adjudications associated to occupancies under this lane";

    DataComponent recordStructure;
    DataEncoding recordEncoding;
    RADHelper fac;

    public AdjudicationOutput(LaneSystem parentSensor) {
        super(NAME, parentSensor);

        fac = new RADHelper();

        this.recordStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .definition(RADHelper.getRadUri("AdjudicationOutput"))
                .addField("sampleTime", fac.createTime()
                        .asSamplingTimeIsoUTC())
                .addField("username", fac.createText()
                        .label("Username")
                        .definition(SWEHelper.getPropertyUri("Username"))
                        .build())
                .addAllFields(fac.createAdjudicationRecord())
                .build();

        this.recordEncoding = new TextEncodingImpl();
    }

    public void setData(Adjudication adjudication) {
        long timeMillis = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ? recordStructure.createDataBlock() : latestRecord.renew();

        dataBlock.setDoubleValue(0, timeMillis/1000d);
        // TODO: Check this gets accurate username
        String username = getParentProducer().getSecurityHandler().getCurrentUser().getName();
        dataBlock.setStringValue(1, username);

        dataBlock.setStringValue(2, adjudication.getFeedback());
        dataBlock.setIntValue(3, adjudication.getAdjudicationCode());
        dataBlock.setStringValue(4, adjudication.getIsotopes());
        dataBlock.setStringValue(5, adjudication.getSecondaryInspectionStatus());
        dataBlock.setStringValue(5, adjudication.getFilePaths());
        dataBlock.setStringValue(6, adjudication.getOccupancyId());
        dataBlock.setStringValue(7, adjudication.getVehicleId());

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
