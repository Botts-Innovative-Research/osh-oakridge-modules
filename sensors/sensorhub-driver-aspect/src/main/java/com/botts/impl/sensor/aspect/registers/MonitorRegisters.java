package com.botts.impl.sensor.aspect.registers;

import com.botts.impl.sensor.aspect.enums.ChannelStatus;
import com.botts.impl.sensor.aspect.enums.Inputs;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MonitorRegisters {
    private final TCPMasterConnection tcpMasterConnection;
    private final int ref;
    private final int count;

    private int timeElapsed;
    private int inputSignals;
    private int gammaChannelStatus;
    private int gammaChannelCount;
    private float gammaChannelBackground;
    private float gammaChannelVariance;
    private int neutronChannelStatus;
    private int neutronChannelCount;
    private float neutronChannelBackground;
    private float neutronChannelVariance;
    private int numberOfObject;
    private int numberOfWheelPair;
    private int objectSpeed;
    private int outputSignals;

    public MonitorRegisters(TCPMasterConnection tcpMasterConnection, int ref, int count) {
        this.tcpMasterConnection = tcpMasterConnection;
        this.ref = ref;
        this.count = count;
    }

    public void readRegisters(int unitID) throws ModbusException {
        ReadMultipleRegistersRequest readMultipleRegistersRequest = new ReadMultipleRegistersRequest(ref, count);
        readMultipleRegistersRequest.setUnitID(unitID);

        ModbusTCPTransaction modbusTCPTransaction = new ModbusTCPTransaction(tcpMasterConnection);
        modbusTCPTransaction.setRequest(readMultipleRegistersRequest);
        modbusTCPTransaction.execute();

        ReadMultipleRegistersResponse response = (ReadMultipleRegistersResponse) modbusTCPTransaction.getResponse();

        timeElapsed = convertRegistersToInteger(response.getRegisterValue(0), response.getRegisterValue(1));
        inputSignals = response.getRegisterValue(2);
        gammaChannelStatus = response.getRegisterValue(3);
        gammaChannelCount = convertRegistersToInteger(response.getRegisterValue(4), response.getRegisterValue(5));
        gammaChannelBackground = convertRegistersToFloat(response.getRegisterValue(6), response.getRegisterValue(7));
        gammaChannelVariance = convertRegistersToFloat(response.getRegisterValue(8), response.getRegisterValue(9));
        neutronChannelStatus = response.getRegisterValue(10);
        neutronChannelCount = convertRegistersToInteger(response.getRegisterValue(11), response.getRegisterValue(12));
        neutronChannelBackground = convertRegistersToFloat(response.getRegisterValue(13), response.getRegisterValue(14));
        neutronChannelVariance = convertRegistersToFloat(response.getRegisterValue(15), response.getRegisterValue(16));
        numberOfObject = response.getRegisterValue(17);
        numberOfWheelPair = response.getRegisterValue(18);
        objectSpeed = response.getRegisterValue(19);
        outputSignals = response.getRegisterValue(20);
    }

    public String toString() {
        return MonitorRegisters.class.getSimpleName() + "{" +
                "timeElapsed=" + getTimeElapsed() +
                ", inputSignals=0x" + Integer.toHexString(getInputSignals()) +
                ", gammaChannelStatus=" + getGammaChannelStatus() +
                ", gammaChannelCount=" + getGammaChannelCount() +
                ", gammaChannelBackground=" + getGammaChannelBackground() +
                ", gammaChannelVariance=" + getGammaChannelVariance() +
                ", neutronChannelStatus=" + getNeutronChannelStatus() +
                ", neutronChannelCount=" + getNeutronChannelCount() +
                ", neutronChannelBackground=" + getNeutronChannelBackground() +
                ", neutronChannelVariance=" + getNeutronChannelVariance() +
                ", numberOfObject=" + getNumberOfObject() +
                ", numberOfWheelPair=" + getNumberOfWheelPair() +
                ", objectSpeed=" + getObjectSpeed() +
                ", outputSignals=0x" + Integer.toHexString(getOutputSignals()) +
                '}';
    }

    /**
     * Converts the two registers into a single 32-bit integer.
     */
    private int convertRegistersToInteger(int lowRegister, int highRegister) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short) lowRegister);
        bb.putShort((short) highRegister);
        return bb.getInt(0);
    }

    /**
     * Converts the two registers into a single 32-bit float.
     */
    private float convertRegistersToFloat(int lowRegister, int highRegister) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short) lowRegister);
        bb.putShort((short) highRegister);
        return bb.getFloat(0);
    }

    public int getTimeElapsed() {
        return timeElapsed;
    }

    public int getInputSignals() {
        return inputSignals;
    }

    public int getGammaChannelStatus() {
        return gammaChannelStatus;
    }

    public int getGammaChannelCount() {
        return gammaChannelCount;
    }

    public float getGammaChannelBackground() {
        return gammaChannelBackground;
    }

    public float getGammaChannelVariance() {
        return gammaChannelVariance;
    }

    public int getNeutronChannelStatus() {
        return neutronChannelStatus;
    }

    public int getNeutronChannelCount() {
        return neutronChannelCount;
    }

    public float getNeutronChannelBackground() {
        return neutronChannelBackground;
    }

    public float getNeutronChannelVariance() {
        return neutronChannelVariance;
    }

    public int getNumberOfObject() {
        return numberOfObject;
    }

    public int getNumberOfWheelPair() {
        return numberOfWheelPair;
    }

    public int getObjectSpeed() {
        return objectSpeed;
    }

    public int getOutputSignals() {
        return outputSignals;
    }

    public String getGammaAlarmState() {
        if ((getGammaChannelStatus() & ChannelStatus.Alarm1.getValue()) == ChannelStatus.Alarm1.getValue()
                || (getGammaChannelStatus() & ChannelStatus.Alarm2.getValue()) == ChannelStatus.Alarm2.getValue()) {
            return "Alarm";
        } else if ((getGammaChannelStatus() & ChannelStatus.LowCount.getValue()) == ChannelStatus.LowCount.getValue()) {
            return "Fault - Gamma Low";
        } else if ((getGammaChannelStatus() & ChannelStatus.HighCount.getValue()) == ChannelStatus.HighCount.getValue()) {
            return "Fault - Gamma High";
        } else {
            return "Background";
        }
    }

    public String getNeutronAlarmState() {
        if ((getNeutronChannelStatus() & ChannelStatus.Alarm1.getValue()) == ChannelStatus.Alarm1.getValue()
                || (getNeutronChannelStatus() & ChannelStatus.Alarm2.getValue()) == ChannelStatus.Alarm2.getValue()) {
            return "Alarm";
        } else if ((getNeutronChannelStatus() & ChannelStatus.LowCount.getValue()) == ChannelStatus.LowCount.getValue()) {
            return "Fault - Neutron Low";
        } else if ((getNeutronChannelStatus() & ChannelStatus.HighCount.getValue()) == ChannelStatus.HighCount.getValue()) {
            return "Fault - Neutron High";
        } else {
            return "Background";
        }
    }

    public boolean isGammaAlarm() {
        return (getGammaChannelStatus() & ChannelStatus.Alarm1.getValue()) == ChannelStatus.Alarm1.getValue()
                || (getGammaChannelStatus() & ChannelStatus.Alarm2.getValue()) == ChannelStatus.Alarm2.getValue();
    }

    public boolean isNeutronAlarm() {
        return (getNeutronChannelStatus() & ChannelStatus.Alarm1.getValue()) == ChannelStatus.Alarm1.getValue()
                || (getNeutronChannelStatus() & ChannelStatus.Alarm2.getValue()) == ChannelStatus.Alarm2.getValue();
    }

    public boolean isOccupied() {
        return (getInputSignals() & Inputs.Occup0.getValue()) == Inputs.Occup0.getValue()
                || (getInputSignals() & Inputs.Occup1.getValue()) == Inputs.Occup1.getValue()
                || (getInputSignals() & Inputs.Occup2.getValue()) == Inputs.Occup2.getValue()
                || (getInputSignals() & Inputs.Occup3.getValue()) == Inputs.Occup3.getValue();
    }
}
