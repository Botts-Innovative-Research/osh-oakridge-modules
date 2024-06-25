package com.botts.impl.sensor.aspect.enums;

public enum Inputs {
    None(0x0),
    Occup0(0x1),
    Occup1(0x2),
    Occup2(0x4),
    Occup3(0x8),
    Button(0x0010),
    Sensor(0x0020),
    Dir01(0x0040),
    Dir10(0x0080),
    PowerFail(0x0100),
    LowBattery(0x0200),
    Open(0x0400),
    Error(0x0800),
    Bit12(0x1000),
    Bit13(0x2000),
    Bit14(0x4000),
    Reset(0x8000),
    Occup(0x000F), // = Occup3 | Occup2 | Occup1 | Occup
    DirUnknown(0x00C0), //= dir10 | dir01
    DirMask(0x00C0), // = dir unknown
    Warn(0x8500), //= reset | open | powerfail
    Fail(0x0A00); //= error | low battery

    private final int value;

    Inputs(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

