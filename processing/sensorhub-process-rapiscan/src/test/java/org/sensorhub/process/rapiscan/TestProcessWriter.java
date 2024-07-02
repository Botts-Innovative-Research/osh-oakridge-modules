package org.sensorhub.process.rapiscan;

import org.junit.Test;
import org.sensorhub.process.rapiscan.helpers.ProcessHelper;
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

        AlarmRecorder process = new AlarmRecorder();

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
