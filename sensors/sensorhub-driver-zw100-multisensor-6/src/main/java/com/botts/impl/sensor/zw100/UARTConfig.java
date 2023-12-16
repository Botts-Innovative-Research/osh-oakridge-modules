package com.botts.impl.sensor.zw100;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sensorhub.impl.sensor.AbstractSensorOutput;


public class UARTConfig extends AbstractSensorOutput<ZW100Sensor>{

        @Override
        public DataComponent getRecordDescription() {
                return null;
        }

        @Override
        public DataEncoding getRecommendedEncoding() {
                return null;
        }

        @Override
        public double getAverageSamplingPeriod() {
                return 0;
        }

        public enum Parity { PARITY_EVEN, PARITY_MARK, PARITY_NONE, PARITY_ODD, PARITY_SPACE };

        public String portName = "/dev/ttyS4";
        public int baudRate = 115200;

        public byte dataBits = 8;

        public byte stopBits = 1;

        public Parity parity = Parity.PARITY_NONE;

        public int receiveTimeout = -1;

        public int receiveThreshold = 1;

        private static final Logger logger = LoggerFactory.getLogger(TemperatureOutput.class);
        private static final String SENSOR_OUTPUT_NAME = "Temperature";

        UARTConfig(ZW100Sensor parentSensor) {

                super(SENSOR_OUTPUT_NAME, parentSensor);

                logger.debug("Output created");
        }
    }



