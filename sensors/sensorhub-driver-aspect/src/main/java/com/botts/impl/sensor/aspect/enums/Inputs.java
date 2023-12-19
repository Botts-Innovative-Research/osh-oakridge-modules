package com.botts.impl.sensor.aspect.enums;

public enum Inputs {
    None(0x0),
    Occup0(0x1),
    Occup1(0x2),
    Occup2(0x4),
    Occup3(0x8),
    Button(0x10),
    Sensor(0x20),
    Dir01(0x40),
    Dir10(0x80),
    PowerFail(0x100),
    LowBattery(0x200),
    Open(0x400),
    Error(0x800),
    Bit12(0x1000),
    Bit13(0x2000),
    Bit14(0x4000),
    Reset(0x8000),
    Occup(0xF),
    DirUnknown(0xC0),
    DirMask(0xC0),
    Warn(0x8500),
    Fail(0xA00);

    private final int value;

    Inputs(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

