
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
import org.sensorhub.impl.service.consys.*;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.vast.data.DataArrayImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;

public class AdjudicationOutput extends AbstractSensorOutput<LaneSystem> {

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
        recordStructure.setData(dataBlock);
//        dataBlock = Adjudication.fromAdjudication(adjudication);

        int index = 0;
        dataBlock.setDoubleValue(index++, timeMillis/1000d);

        var id = getParentProducer().getSecurityHandler().getCurrentUser().getId();

        getParentProducer().getParentHub().getSecurityManager().getUserRegistry().get(id);


        String username = null;
//        var csApi = getParentProducer().getParentHub().getModuleRegistry().getModuleByType(ConSysApiService.class);
//        var csApiServlet = csApi.getServlet();
//        var apiSecurity = csApiServlet.getSecurityHandler();
//        var user = apiSecurity.getCurrentUser().getId();

        dataBlock.setStringValue(index++, username == null ? "" : username);
        dataBlock.setStringValue(index++, adjudication.getFeedback());
        dataBlock.setStringValue(index++, adjudication.getAdjudicationCode().toString());

        int isotopeCount =  adjudication.getIsotopes().size();
        dataBlock.setIntValue(index++, isotopeCount);

        var isotopesArray =((DataArrayImpl) recordStructure.getComponent("isotopes"));
        isotopesArray.updateSize();
        dataBlock.updateAtomCount();

        for (int i = 0; i < isotopeCount; i++)
            dataBlock.setStringValue(index++, adjudication.getIsotopes().get(i));

        dataBlock.setStringValue(index++, adjudication.getSecondaryInspectionStatus().toString());

        int filePathCount = adjudication.getFilePaths().size();
        dataBlock.setIntValue(index++, filePathCount);

        var filePathsArray =((DataArrayImpl) recordStructure.getComponent("filePaths"));
        filePathsArray.updateSize();
        dataBlock.updateAtomCount();

        for (int i = 0; i < filePathCount; i++)
            dataBlock.setStringValue(index++, adjudication.getFilePaths().get(i));


        dataBlock.setStringValue(index++, adjudication.getOccupancyId());
        dataBlock.setStringValue(index, adjudication.getVehicleId());

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
