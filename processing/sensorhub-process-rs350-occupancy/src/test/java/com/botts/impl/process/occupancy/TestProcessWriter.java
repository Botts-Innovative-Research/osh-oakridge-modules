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

package com.botts.impl.process.occupancy;

import org.junit.Test;
import com.botts.impl.process.occupancy.helpers.ProcessHelper;
import org.vast.swe.SWEHelper;

public class TestProcessWriter {

    public TestProcessWriter() throws Exception {
        testGamepadPtzProcess();
    }

    @Test
    public void testGamepadPtzProcess() throws Exception
    {
        ProcessHelper processHelper = new ProcessHelper();
        SWEHelper fac = new SWEHelper();

        OccupancyDataRecorder process = new OccupancyDataRecorder();

        processHelper.addOutputList(process.getOutputList());

        processHelper.addDataSource("source0", "urn:osh:sensor:rapiscansensor001");

        process.getParameterList().getComponent(0).getData().setStringValue("29f2b677-95b1-4499-8e5b-459839ec3eb6");

        processHelper.addProcess("process0", process);

        processHelper.addConnection("components/source0/outputs/Occupancy/"
                ,"components/process0/inputs/occupancy");

        processHelper.addConnection("components/process0/outputs/neutronEntry",
                "outputs/neutronEntry");
        processHelper.addConnection("components/process0/outputs/gammaEntry",
                "outputs/gammaEntry");
        processHelper.addConnection("components/process0/outputs/video1",
                "outputs/video1");

        processHelper.writeXML(System.out);
    }

}
