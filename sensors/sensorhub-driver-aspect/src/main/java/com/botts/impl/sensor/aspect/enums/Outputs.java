package com.botts.impl.sensor.aspect.enums;

public enum Outputs {
    None(0x0),
    Alarm(0x1),
    Video(0x2),
    YellowLED(0x4),
    FastFlashingLED(0x8),
    SlowFlashingLED(0x10),
    Busy(0x20),
    Adaptation(0x40),
    Bit7(0x80),
    EntryGreen(0x100),
    ExitGreen(0x200),
    ExitArrow(0x400),
    Out8(0x800),
    Bit12(0x1000),
    Bit13(0x2000),
    Bit14(0x4000),
    Bit15(0x8000);

    private final int value;

    Outputs(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

