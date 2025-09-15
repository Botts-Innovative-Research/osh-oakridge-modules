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

package com.botts.impl.service.oscar.siteinfo;

import com.botts.impl.service.oscar.OSCARSystem;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class SiteInfoOutput extends AbstractSensorOutput<OSCARSystem> {

    public static final String NAME = "siteInfo";
    public static final String LABEL = "Site Info";
    public static final String DESCRIPTION = "Important information about this OSCAR node's site";

    DataComponent recordStructure;
    DataEncoding recordEncoding;
    GeoPosHelper fac;

    public SiteInfoOutput(OSCARSystem parentSensor) {
        super(NAME, parentSensor);

        fac = new GeoPosHelper();

        this.recordStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("sampleTime", fac.createTime().asSamplingTimeIsoUTC())
                .addField("siteDiagramPath", fac.createText()
                        .description("Path of site diagram image"))
                .addField("siteBoundingBox", fac.createRecord()
                        .description("Geographic bounding box coordinates of site diagram")
                        .addField("lowerLeftBound", fac.createLocationVectorLatLon())
                        .addField("upperRightBound", fac.createLocationVectorLatLon()))
                .build();

        this.recordEncoding = new TextEncodingImpl();
    }

    public void setData(String siteDiagramPath, double[] lowerLeftBound, double[] upperRightBound) {

        long timeMillis = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ? recordStructure.createDataBlock() : latestRecord.renew();

        dataBlock.setDoubleValue(0, timeMillis/1000d);
        dataBlock.setStringValue(1, siteDiagramPath);
        dataBlock.setDoubleValue(2, lowerLeftBound[0]);
        dataBlock.setDoubleValue(3, lowerLeftBound[1]);
        dataBlock.setDoubleValue(4, upperRightBound[0]);
        dataBlock.setDoubleValue(5, upperRightBound[1]);

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
