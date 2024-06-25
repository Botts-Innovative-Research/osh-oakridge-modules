package com.botts.impl.sensor.aspect.enums;

public enum Outputs {
    None(0x0000),
    Alarm(0x0001),
    Video(0x0002),
    YellowLED(0x0004),
    FastFlashingLED(0x0008),
    SlowFlashingLED(0x0010),
    Busy(0x0020),
    Adaptation(0x0040),
    Bit7(0x0080),
    EntryGreen(0x0100),
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

