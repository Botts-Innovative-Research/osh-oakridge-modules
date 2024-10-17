package com.botts.impl.sensor.aspect.registers;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

public class DeviceDescriptionRegisters {
    private static final int REF = 0;
    private static final int COUNT = 8;

    private final TCPMasterConnection tcpMasterConnection;
    private int signature;
    private int numberOfRegisters;
    private int monitorRegistersIdentifier;
    private int monitorRegistersBaseAddress;
    private int monitorRegistersNumberOfRegisters;

    public DeviceDescriptionRegisters(TCPMasterConnection tcpMasterConnection) {
        this.tcpMasterConnection = tcpMasterConnection;
    }

    public void readRegisters(int unitID) throws ModbusException {
        ReadMultipleRegistersRequest readMultipleRegistersRequest = new ReadMultipleRegistersRequest(REF, COUNT);
        readMultipleRegistersRequest.setUnitID(unitID);

        ModbusTCPTransaction modbusTCPTransaction = new ModbusTCPTransaction(tcpMasterConnection);
        modbusTCPTransaction.setRequest(readMultipleRegistersRequest);
        modbusTCPTransaction.execute();

        ReadMultipleRegistersResponse response = (ReadMultipleRegistersResponse) modbusTCPTransaction.getResponse();
        signature = response.getRegisterValue(0);
        numberOfRegisters = response.getRegisterValue(1);
        monitorRegistersIdentifier = response.getRegisterValue(2);
        monitorRegistersBaseAddress = response.getRegisterValue(3);
        monitorRegistersNumberOfRegisters = response.getRegisterValue(4);
    }

    public String toString() {
        return DeviceDescriptionRegisters.class.getSimpleName() + "{" +
                "signature=0x" + Integer.toHexString(getSignature()) +
                ", numberOfRegisters=" + getNumberOfRegisters() +
                ", monitorRegistersIdentifier=" + getMonitorRegistersIdentifier() +
                ", monitorRegistersBaseAddress=" + getMonitorRegistersBaseAddress() +
                ", monitorRegistersNumberOfRegisters=" + getMonitorRegistersNumberOfRegisters() +
                '}';
    }

    public int getSignature() {
        return signature;
    }

    public int getNumberOfRegisters() {
        return numberOfRegisters;
    }

    public int getMonitorRegistersIdentifier() {
        return monitorRegistersIdentifier;
    }

    public int getMonitorRegistersBaseAddress() {
        return monitorRegistersBaseAddress;
    }

    public int getMonitorRegistersNumberOfRegisters() {
        return monitorRegistersNumberOfRegisters;
    }
}
