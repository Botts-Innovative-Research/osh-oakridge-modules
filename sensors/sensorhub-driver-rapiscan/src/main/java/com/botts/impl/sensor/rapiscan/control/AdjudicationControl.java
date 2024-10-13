package com.botts.impl.sensor.rapiscan.control;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

public class AdjudicationControl extends AbstractSensorControl<RapiscanSensor> {
    private static final String SENSOR_CONTROL_NAME = "adjudicationControl";
    private static final String SENSOR_CONTROL_LABEL = "Adjudication Control";
    DataRecord commandData;
    IdEncoder obsEncoder;
    public AdjudicationControl(RapiscanSensor parentSensor) {
        super(SENSOR_CONTROL_NAME, parentSensor);
    }

    public void init() {
        SWEHelper fac = new SWEHelper();
        obsEncoder = getParentProducer().getParentHub().getIdEncoders().getObsIdEncoder();

        commandData = fac.createRecord()
                .name(getName())
                .label(SENSOR_CONTROL_LABEL)
                .addField("observationId", fac.createText()
                        .label("Observation ID")
                        .build())
                .addField("setAdjudicated", fac.createBoolean()
                        .label("Set Adjudicated")
                        .build())
                .build();
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandData;
    }
    @Override
    protected boolean execCommand(DataBlock cmdData) {
        DataRecord commandMsg = commandData.copy();
        commandMsg.setData(cmdData);

        String observationId = commandMsg.getData().getStringValue(0);
        boolean setAdjudicated = commandMsg.getData().getBooleanValue(1);

        if(observationId == null || observationId.equals("null"))
            return false;

        BigId internalObsId = obsEncoder.decodeID(observationId);

        var prevObs = getParentProducer().getParentHub().getDatabaseRegistry()
                .getFederatedDatabase()
                .getObservationStore().get(internalObsId);

        prevObs.getResult().setBooleanValue(9, setAdjudicated);

        String systemUID = getParentProducer().getParentSystemUID() != null ?
                getParentProducer().getParentSystemUID() :
                getParentProducer().getUniqueIdentifier();
        var obsDb = getParentProducer().getParentHub().getSystemDriverRegistry().getDatabase(systemUID);

        obsDb.getObservationStore().put(internalObsId, prevObs);

        return prevObs.getResult().getBooleanValue(9) == setAdjudicated;
    }
}
