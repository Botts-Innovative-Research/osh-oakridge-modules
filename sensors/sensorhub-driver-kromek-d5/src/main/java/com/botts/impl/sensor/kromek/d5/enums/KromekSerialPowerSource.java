package com.botts.impl.sensor.kromek.d5.enums;

public enum KromekSerialPowerSource {
    KROMEK_POWER_SOURCE_UNKNOWN,
    KROMEK_POWER_SOURCE_BATTERY,
    KROMEK_POWER_SOURCE_USB,
    KROMEK_POWER_SOURCE_QI,
    KROMEK_POWER_SOURCE_ETHERNET,
    KROMEK_POWER_SOURCE_BATTERY_BACKUP_AA;

    public static KromekSerialPowerSource fromByte(byte b) {
        switch (b) {
            case 0:
            default:
                return KROMEK_POWER_SOURCE_UNKNOWN;
            case 1:
                return KROMEK_POWER_SOURCE_BATTERY;
            case 2:
                return KROMEK_POWER_SOURCE_USB;
            case 3:
                return KROMEK_POWER_SOURCE_QI;
            case 4:
                return KROMEK_POWER_SOURCE_ETHERNET;
            case 5:
                return KROMEK_POWER_SOURCE_BATTERY_BACKUP_AA;
        }
    }
}