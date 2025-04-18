package com.botts.impl.sensor.aspect.registers;

import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.aspect.enums.ChannelStatus;
import com.botts.impl.sensor.aspect.enums.Inputs;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.SysexMessage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

public class MonitorRegisters {
    private static final Logger log = LoggerFactory.getLogger(MonitorRegisters.class);
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
    private int objectCount;
    private int objectMark;
    private int objectSpeed;
    private int outputSignals;

    public MonitorRegisters(TCPMasterConnection tcpMasterConnection, int ref, int count) {
        this.tcpMasterConnection = tcpMasterConnection;
        this.ref = ref;
        this.count = count;
    }

    public void readRegisters(int unitID) throws ModbusException {
        try{
            if(!tcpMasterConnection.isConnected()){
                throw new IllegalStateException("TCP connection was not established!");
            }

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
            objectCount = response.getRegisterValue(17);
            objectMark = response.getRegisterValue(18);
            objectSpeed = response.getRegisterValue(19);
            outputSignals = response.getRegisterValue(20);

            log.debug("Registers read successfully: ", this.toString());
        }catch(ModbusException e){
            log.debug("Error reading Modbus Registers: ", e);
        }catch(Exception e){
            log.debug("Error: ",e);
        }

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
                ", objectNumber=" + getObjectCounter() +
                ", wheelsNumber=" + getObjectMark() +
                ", velocity=" + getObjectSpeed() +
                ", outputSignals=0x" + Integer.toHexString(getOutputSignals()) +
                '}';
    }

    /**
     * Converts the two registers into a single 32-bit integer.
     */
    private int convertRegistersToInteger(int lowRegister, int highRegister) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short) lowRegister);
        bb.putShort((short) highRegister);
//        bb.flip();
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
//        bb.flip();
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

    public double getGammaChannelVariance() {
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
    public int getObjectCounter() {
        return objectCount;
    }

    public int getObjectMark() {
        return objectMark;
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
