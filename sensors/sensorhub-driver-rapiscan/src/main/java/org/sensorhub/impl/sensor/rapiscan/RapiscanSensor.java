package org.sensorhub.impl.sensor.rapiscan;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RapiscanSensor extends AbstractSensorModule<RapiscanConfig>{

    private static final Logger logger = LoggerFactory.getLogger(RapiscanSensor.class);

    ICommProvider<?> commProvider;

    InputStream msgIn;

    CsvMsgPrinter csvMsgPrinter = new CsvMsgPrinter();

    public RapiscanSensor() {
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:rapiscan", config.serialNumber);
        generateXmlID("Rapiscan", config.serialNumber);

    }

    @Override
    protected void doStart() throws SensorHubException {

        // init comm provider
        if (commProvider == null) {

            // we need to recreate comm provider here because it can be changed by UI
            try {

                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                var moduleReg = getParentHub().getModuleRegistry();

                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);

                commProvider.start();

            } catch (Exception e) {

                commProvider = null;

                throw e;
            }
        }

        // connect to data stream
        try {

            msgIn = new BufferedInputStream(commProvider.getInputStream());

            csvMsgPrinter.printMessages(msgIn);

        } catch (IOException e) {

            throw new SensorException("Error while initializing communications ", e);
        }
    }



        @Override
    public boolean isConnected() {
        return false;
    }
}