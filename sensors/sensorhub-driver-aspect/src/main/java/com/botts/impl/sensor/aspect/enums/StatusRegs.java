package com.botts.impl.sensor.aspect.enums;

public class StatusRegs {
    public short DetectorFlags1;
    public short DetectorFlags2;
    public short DetectorFlags3;
    public short DetectorCount1;
    public short DetectorCount2;
    public short DetectorCount3;
    public short DetectorCount4;
    public short DetectorCount5;
    public short DetectorCount6;
    public short DetectorCount7;
    public short DetectorCount8;
    public int InternalTime;
    public Inputs Inputs;
    public ChannelStatus GammaStatus;
    public int GammaCount;
    public float GammaBackground;
    public float GammaVar;
    public ChannelStatus NeutronStatus;
    public int NeutronCount;
    public float NeutronBackground;
    public float NeutronVar;
    public short ObjectCounter;
    public short ObjectMark;
    public short ObjectSpeed;
    public Outputs Outputs;

    StatusRegs() {
    }
}
