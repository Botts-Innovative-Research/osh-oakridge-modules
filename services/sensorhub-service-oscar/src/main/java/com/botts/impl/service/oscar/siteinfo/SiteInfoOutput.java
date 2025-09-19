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
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEConstants;
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
    }

    public void init(){
        fac = new GeoPosHelper();


        this.recordStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("sampleTime", fac.createTime().asSamplingTimeIsoUTC())
                .addField("siteDiagramPath", fac.createText()
                        .definition(SWEHelper.getPropertyUri("SiteDiagramPath"))
                        .label("Site Diagram Path")
                        .description("Path of site diagram image"))
                .addField("siteBoundingBox", fac.createRecord()
                        .label("Bounding box")
                        .definition(SWEHelper.getPropertyUri("SiteBoundingBox"))
                        .description("Geographic bounding box coordinates of site diagram")
                        .addField("lowerLeftBound", fac.createVector()
                                .addCoordinate("lat", fac.createQuantity()
                                        .definition(SWEHelper.getPropertyUri("GeodeticLatitude"))
                                        .refFrame(SWEConstants.REF_FRAME_4326)
                                        .label("Geodetic Latitude")
                                        .axisId("Lat")
                                        .uomCode("deg")
                                        .refFrame(null))
                                .addCoordinate("lon", fac.createQuantity()
                                        .definition(SWEHelper.getPropertyUri("Longitude"))
                                        .refFrame(SWEConstants.REF_FRAME_4326)
                                        .label("Geodetic Latitude")
                                        .axisId("Lat")
                                        .uomCode("deg")
                                        .refFrame(null))
                                .refFrame(SWEConstants.REF_FRAME_4326)
                                .label("Lower Left Bound")
                                .definition(SWEHelper.getPropertyUri("LowerLeftBound"))
                                .build())
                        .addField("upperRightBound", fac.createVector()
                                .addCoordinate("lat", fac.createQuantity()
                                        .definition(SWEHelper.getPropertyUri("GeodeticLatitude"))
                                        .refFrame(SWEConstants.REF_FRAME_4326)
                                        .label("Geodetic Latitude")
                                        .axisId("Lat")
                                        .uomCode("deg")
                                        .refFrame(null))
                                .addCoordinate("lon", fac.createQuantity()
                                        .definition(SWEHelper.getPropertyUri("Longitude"))
                                        .refFrame(SWEConstants.REF_FRAME_4326)
                                        .label("Geodetic Latitude")
                                        .axisId("Lat")
                                        .uomCode("deg")
                                        .refFrame(null))
                                .refFrame(SWEConstants.REF_FRAME_4326)
                                .label("Upper Right Bound")
                                .definition(SWEHelper.getPropertyUri("UpperRightBound"))
                                .build())
                ).build();

        this.recordEncoding = new TextEncodingImpl();
    }
    public void setData(String siteDiagramPath, SiteDiagramConfig.LatLonLocation lowerLeftBound, SiteDiagramConfig.LatLonLocation upperRightBound) {

        long timeMillis = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ? recordStructure.createDataBlock() : latestRecord.renew();

        dataBlock.setDoubleValue(0, timeMillis/1000d);
        dataBlock.setStringValue(1, siteDiagramPath);
        dataBlock.setDoubleValue(2, lowerLeftBound.lon);
        dataBlock.setDoubleValue(3, lowerLeftBound.lat);
        dataBlock.setDoubleValue(4, upperRightBound.lon);
        dataBlock.setDoubleValue(5, upperRightBound.lat);

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
