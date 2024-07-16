package com.botts.impl.sensor.rapiscan.eml;

import com.botts.impl.sensor.rapiscan.MessageHandler;
import com.botts.impl.sensor.rapiscan.eml.types.AlgorithmType;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import org.sensorhub.impl.utils.rad.RADHelper;

import java.util.Arrays;
import java.util.EnumSet;

public class EMLHelper extends RADHelper {

    //TODO ???
    // Helper functions for EMLOutput_OLD
    public Text createResult() {
        return createText()
                .name("result")
                .label("Result")
                .definition(RADHelper.getRadUri("result"))
                .build();
    }

}
