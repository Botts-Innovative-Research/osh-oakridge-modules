
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

package com.botts.impl.service.oscar.clientconfig;

import com.botts.impl.service.oscar.OSCARSystem;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class ClientConfigOutput extends AbstractSensorOutput<OSCARSystem> {

    public static final String NAME = "clientConfig";
    public static final String LABEL = "Client Config";
    public static final String DESCRIPTION = "Configuration of nodes for OSCAR-viewer connected to this node";

    DataComponent recordStructure;
    DataEncoding recordEncoding;
    SWEHelper fac;

    public ClientConfigOutput(OSCARSystem parentSensor) {
        super(NAME, parentSensor);

        fac = new SWEHelper();

        this.recordStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("sampleTime", fac.createTime().asSamplingTimeIsoUTC())
                .addField("user", fac.createText()
                        .label("User")
                        .definition(SWEHelper.getPropertyUri("User")))
                .addField("numNodes", fac.createCount()
                        .label("Number of Nodes")
                        .id("numNodes")
                        .build())
                .addField("nodesArray", fac.createArray()
                        .label("Nodes Array")
                        .description("List of Nodes")
                        .withVariableSize("numNodes")
                        .definition(SWEHelper.getPropertyUri("Nodes"))
                        .withElement("nodes", fac.createRecord()
                                .addField("name", fac.createText()
                                        .label("Name")
                                        .definition(SWEHelper.getPropertyUri("Name"))
                                )
                                .addField("address", fac.createText()
                                        .label("Address")
                                        .definition(SWEHelper.getPropertyUri("Address"))
                                )
                                .addField("port", fac.createText()
                                        .label("Port")
                                        .definition(SWEHelper.getPropertyUri("Port"))
                                )
                                .addField("oshPathRoot", fac.createText()
                                        .label("Osh Path")
                                        .definition(SWEHelper.getPropertyUri("OshPath"))
                                )
                                .addField("csAPIEndpoint", fac.createText()
                                        .label("CS API Endpoint")
                                        .definition(SWEHelper.getPropertyUri("CSAPIEndpoint"))
                                )
                                .addField("isSecure", fac.createBoolean()
                                        .label("Secure")
                                        .definition(SWEHelper.getPropertyUri("IsSecure"))
                                )
                                .addField("isDefaultNode", fac.createBoolean()
                                        .label("Default")
                                        .definition(SWEHelper.getPropertyUri("IsDefaultNode"))
                                )
                                .addField("username", fac.createText()
                                        .label("Username")
                                        .definition(SWEHelper.getPropertyUri("Username"))
                                )
                                .addField("password", fac.createText()
                                        .label("Password")
                                        .definition(SWEHelper.getPropertyUri("Password"))
                                )).build()
                ).build();

        this.recordEncoding = new TextEncodingImpl();
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
