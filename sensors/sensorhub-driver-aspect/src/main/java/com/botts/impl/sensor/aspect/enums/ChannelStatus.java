package com.botts.impl.sensor.aspect.enums;

public enum ChannelStatus {
    None(0x0),
    Alarm1(0x1),
    Alarm2(0x2),
    Bit2(0x4),
    Bit3(0x8),
    LowCount(0x10),
    HighCount(0x20),
    Overload(0x40),
    Ready(0x80),
    N0(0x100),
    N1(0x200),
    N2(0x400),
    N3(0x800),
    N4(0x1000),
    N5(0x2000),
    N6(0x4000),
    N7(0x8000);

    private final int value;

    ChannelStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
