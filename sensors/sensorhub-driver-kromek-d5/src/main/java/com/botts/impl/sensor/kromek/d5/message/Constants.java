package com.botts.impl.sensor.kromek.d5.message;

public class Constants {
    public static final int KROMEK_SERIAL_REPORTS_USE_COMPONENT = 1;

    public static final byte KROMEK_SERIAL_FRAMING_FRAME_BYTE = (byte) 0xC0; // Framing byte at the beginning / end of messages.
    public static final byte KROMEK_SERIAL_FRAMING_ESC_BYTE = (byte) 0xDB; // 0xDB byte between messages
    public static final byte KROMEK_SERIAL_FRAMING_ESC_FRAME_BYTE = (byte) 0xDC; // 0xC0 --> (0xDB 0xDC)
    public static final byte KROMEK_SERIAL_FRAMING_ESC_ESC_BYTE = (byte) 0xDD; // 0xDB --> (0xDB 0xDD)


    public static final int KROMEK_SERIAL_MESSAGE_CRC_POLYNOMIAL =  0x1021;
    public static final byte KROMEK_SERIAL_MESSAGE_CRC_INITIAL_VALUE = (byte) 0xFFFF;
    public static final int KROMEK_SERIAL_MESSAGE_CRC_BIT_WIDTH = 16;
    public static final boolean KROMEK_SERIAL_MESSAGE_CRC_WRITE_TRANSPOSE = false;
    public static final boolean KROMEK_SERIAL_MESSAGE_CRC_READ_TRANSPOSE = false;
    public static final boolean KROMEK_SERIAL_MESSAGE_CRC_COMPLEMENT_READ = false;

    public static final byte KROMEK_SERIAL_MESSAGE_MODE = (byte) 0x00;

    public static class KromekSerialMessageHeader {
        public short length; // Total length including KROMEK_SERIAL_MESSAGE_OVERHEAD
        public byte mode;
    }

    //(sizeof(KromekSerialMessageHeader) + sizeof(KromekSerialMessageCRC))
    public static final int KROMEK_SERIAL_MESSAGE_OVERHEAD = 5;
    public static final int KROMEK_SERIAL_REPORTS_HEADER_OVERHEAD = 2;

    // Define one of the following here
    public static final boolean KROMEK_SERIAL_REPORTS_BUILD_FOR_PRODUCT_D3S = false;
    public static final boolean KROMEK_SERIAL_REPORTS_BUILD_FOR_PRODUCT_D3M = false;
    public static final boolean KROMEK_SERIAL_REPORTS_BUILD_FOR_PRODUCT_D4 = false;
    public static final boolean KROMEK_SERIAL_REPORTS_BUILD_FOR_PRODUCT_D5 = true;
    public static final boolean KROMEK_SERIAL_REPORTS_BUILD_FOR_PRODUCT_UNIBASE = false;

    // Report Type Bitmask
    public static final byte KROMEK_SERIAL_REPORTS_I0_BITMASK = (byte) 0x80;

    // Component IDs
    public static final byte KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_0 = (byte) 0x01;
    public static final byte KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_1 = (byte) 0x02;
    public static final byte KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_2 = (byte) 0x03;
    public static final byte KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_3 = (byte) 0x04;
    public static final byte KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_4 = (byte) 0x05;
    public static final byte KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_5 = (byte) 0x06;

    public static final byte KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD = (byte) 0x07;
    public static final byte KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD_EXT = (byte) 0x08;

    public static final byte KROMEK_SERIAL_COMPONENT_CLLB = KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_0;
    public static final byte KROMEK_SERIAL_COMPONENT_GAMMA = KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_0;
    public static final byte KROMEK_SERIAL_COMPONENT_NEUTRON = KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_1;
    public static final byte KROMEK_SERIAL_COMPONENT_DOSE = KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_2;
    public static final byte KROMEK_SERIAL_COMPONENT_DOSE_2 = KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_3;
    public static final byte KROMEK_SERIAL_COMPONENT_ALPHA = KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_4;
    public static final byte KROMEK_SERIAL_COMPONENT_BETA = KROMEK_SERIAL_COMPONENT_ANALOGUE_CHANNEL_5;

    //#warning these need to be changed once settings app is updated, should be 32 & 63
    public static final int KROMEK_SERIAL_MAX_WIFI_SSID_LENGTH = 32;
    public static final int KROMEK_SERIAL_MAX_WIFI_PASSWORD_LENGTH = 63;

    public static final int KROMEK_SERIAL_MAX_UNIT_ID_LENGTH = 12;

    // ACKNOWLEDGE Specific Report IDs
    public static final byte KROMEK_SERIAL_REPORTS_IN_ACK_ID = (byte) 0xfe;

    public static final byte KROMEK_SERIAL_REPORTS_ACK_REPORT_ID_ERROR = (byte) 0xff;

    // Detector Specific OUT Report IDs
    public static final byte KROMEK_SERIAL_REPORTS_OUT_GAIN_ID = (byte) 0x02;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_BIAS_UNCOMPENSATED_ID = (byte) 0x07;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_LLD_CHANNEL_ID = (byte) 0x09;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_SOFTWARE_LLD_ENABLED_ID = (byte) 0x0C;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_SOFTWARE_LLD_CHANNEL_ID = (byte) 0x12;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_ENERGY_CALIBRATION_PARAMETERS_ID = (byte) 0x15;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_COARSE_GAIN_ID = (byte) 0x1E;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_SPECTRUM_BIT_SIZE_ID = (byte) 0x20;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_ENERGY_CALIBRATION_RESULT_INDEXED_ID = (byte) 0x24;

    // Detector Specific IN Report IDs
    public static final byte KROMEK_SERIAL_REPORTS_IN_GAIN_ID = (byte) 0x82;
    public static final byte KROMEK_SERIAL_REPORTS_IN_BIAS_UNCOMPENSATED_ID = (byte) 0x87;
    public static final byte KROMEK_SERIAL_REPORTS_IN_LLD_CHANNEL_ID = (byte) 0x89;
    public static final byte KROMEK_SERIAL_REPORTS_IN_BIAS_ACTUAL_ID = (byte) 0x8B;
    public static final byte KROMEK_SERIAL_REPORTS_IN_SOFTWARE_LLD_ENABLED_ID = (byte) 0x8C;
    public static final byte KROMEK_SERIAL_REPORTS_IN_SOFTWARE_LLD_CHANNEL_ID = (byte) 0x92;
    public static final byte KROMEK_SERIAL_REPORTS_IN_ENERGY_CALIBRATION_PARAMETERS_ID = (byte) 0x95;
    public static final byte KROMEK_SERIAL_REPORTS_IN_COARSE_GAIN_ID = (byte) 0x9E;
    public static final byte KROMEK_SERIAL_REPORTS_IN_SPECTRUM_BIT_SIZE_ID = (byte) 0xA0;
    public static final byte KROMEK_SERIAL_REPORTS_IN_ENERGY_CALIBRATION_RESULT_INDEXED_ID = (byte) 0xA4;

    // DeviceLevel OUT Report IDs
    public static final byte KROMEK_SERIAL_REPORTS_OUT_TOTAL_COUNTS_NS_ENABLED_ID = (byte) 0x01;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_OTG_MODE_ID = (byte) 0x46;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_OTA_COMMAND_ID = (byte) 0x49;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_OTA_DATA_ID = (byte) 0x4C;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_ETHERNET_CONFIG_ID = (byte) 0x4D;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_COMPRESSION_ENABLED_ID = (byte) 0x4F;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_BLUETOOTH_CONFIG_ID = (byte) 0x50;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_RADIATION_THRESHOLD_INDEXED_ID = (byte) 0x51;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_RESET_ACCUMULATED_DOSE_ID = (byte) 0x52;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_UI_SCREEN_CONFIG_ID = (byte) 0x54;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_UI_COMPONENTS_ENABLED_ID = (byte) 0x55;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_LOGGING_MODE_ID = (byte) 0x56;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_LOGGING_SESSION_DATA_ID = (byte) 0x57;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_LOGGING_LINK_LOSS_CONFIG_ID = (byte) 0x58;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_LOGGING_NEXT_SESSION_ID = (byte) 0x59;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_LOGGING_CLEAR_DATA_ID = (byte) 0x5B;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_LOGGING_RESTART_SESSION_ID = (byte) 0x5D;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_AUTOPOWER_ID = (byte) 0x5F;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_MAINTAINER_DEVICE_CONFIG_INDEXED_ID = (byte) 0x63;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_USER_DEVICE_CONFIG_INDEXED_ID = (byte) 0x64;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_TRANSPORT_PROTOCOL_ID = (byte) 0x65;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_UTC_TIME_ID = (byte) 0x69;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_REMOTE_ISOTOPE_CONFIRMATION_ID = (byte) 0x6B;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_REMOTE_BACKGROUND_COLLECTION_ID = (byte) 0x6D;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_WIFI_AP_CONFIG_ID = (byte) 0x70;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_UI_BT_ENABLE_ID = (byte) 0x71;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_UI_ALERT_CONFIG_ID = (byte) 0x72;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_UI_CONFIRMATION_MODE_ID = (byte) 0x73;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_UI_SEARCH_ID_ENABLE_ID = (byte) 0x74;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_UI_ISOTOPE_ENABLE_ID = (byte) 0x75;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_UI_PIN_CODE_ID = (byte) 0x76;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_FORCE_BATTERY_SELECT_ID = (byte) 0x79;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_UNIT_ID_ID = (byte) 0x7C;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_ALARM_AUTO_SILENCE_ID = (byte) 0x7D;

    // Device Level IN Report IDs
    public static final byte KROMEK_SERIAL_REPORTS_IN_TOTAL_COUNTS_NS_ENABLED_ID = (byte) 0x81;
    public static final byte KROMEK_SERIAL_REPORTS_IN_ERROR_ID = (byte) 0xC0; // 7th board generated (not requestable)
    public static final byte KROMEK_SERIAL_REPORTS_IN_SPECTRUM_ID = (byte) 0xC1;
    public static final byte KROMEK_SERIAL_REPORTS_IN_RADIOMETRICS_V1_ID = (byte) 0xC2;
    public static final byte KROMEK_SERIAL_REPORTS_IN_COUNT_ID = (byte) 0xC3;
    public static final byte KROMEK_SERIAL_REPORTS_IN_START_ID = (byte) 0xC4; // DEPRECATED
    public static final byte KROMEK_SERIAL_REPORTS_IN_STATUS_ID = (byte) 0xC5;
    public static final byte KROMEK_SERIAL_REPORTS_IN_OTG_MODE_ID = (byte) 0xC6;
    public static final byte KROMEK_SERIAL_REPORTS_IN_ABOUT_ID = (byte) 0xC7;
    public static final byte KROMEK_SERIAL_REPORTS_IN_DEVICE_INFO_ID = (byte) 0xC8;
    public static final byte KROMEK_SERIAL_REPORTS_IN_OTA_PROPERTIES_ID = (byte) 0xCA;
    public static final byte KROMEK_SERIAL_REPORTS_IN_OTA_STATUS_ID = (byte) 0xCB;
    public static final byte KROMEK_SERIAL_REPORTS_IN_OTA_DATA_ID = (byte) 0xCC; // NOT IMPLEMENTED
    public static final byte KROMEK_SERIAL_REPORTS_IN_ETHERNET_CONFIG_ID = (byte) 0xCD;
    public static final byte KROMEK_SERIAL_REPORTS_IN_SPECTRUM_EXTENDED_ID = (byte) 0xCE;
    public static final byte KROMEK_SERIAL_REPORTS_IN_COMPRESSION_ENABLED_ID = (byte) 0xCF;
    public static final byte KROMEK_SERIAL_REPORTS_IN_BLUETOOTH_CONFIG_ID = (byte) 0xD0;
    public static final byte KROMEK_SERIAL_REPORTS_IN_RADIATION_THRESHOLD_INDEXED_ID = (byte) 0xD1;
    public static final byte KROMEK_SERIAL_REPORTS_IN_RESET_ACCUMULATED_DOSE_ID = (byte) 0xD2;
    public static final byte KROMEK_SERIAL_REPORTS_IN_DOSE_INFO_ID = (byte) 0xD3;
    public static final byte KROMEK_SERIAL_REPORTS_IN_UI_SCREEN_CONFIG_ID = (byte) 0xD4;
    public static final byte KROMEK_SERIAL_REPORTS_IN_UI_COMPONENTS_ENABLED_ID = (byte) 0xD5;
    public static final byte KROMEK_SERIAL_REPORTS_IN_LOGGING_MODE_ID = (byte) 0xD6;
    public static final byte KROMEK_SERIAL_REPORTS_IN_LOGGING_SESSION_DATA_ID = (byte) 0xD7;
    public static final byte KROMEK_SERIAL_REPORTS_IN_LOGGING_LINK_LOSS_CONFIG_ID = (byte) 0xD8;
    public static final byte KROMEK_SERIAL_REPORTS_IN_LOGGING_NEXT_RECORD_ID = (byte) 0xDA;
    public static final byte KROMEK_SERIAL_REPORTS_IN_LOGGING_STATUS_ID = (byte) 0xDE;
    public static final byte KROMEK_SERIAL_REPORTS_IN_AUTOPOWER_ID = (byte) 0xDF;
    public static final byte KROMEK_SERIAL_REPORTS_IN_HEALTH_STATUS_ID = (byte) 0xE0;
    public static final byte KROMEK_SERIAL_REPORTS_IN_ENERGY_SPECTRUM_ID = (byte) 0xE1;
    public static final byte KROMEK_SERIAL_REPORTS_IN_MAINTAINER_DEVICE_CONFIG_INDEXED_ID = (byte) 0xE3;
    public static final byte KROMEK_SERIAL_REPORTS_IN_USER_DEVICE_CONFIG_INDEXED_ID = (byte) 0xE4;
    public static final byte KROMEK_SERIAL_REPORTS_IN_TRANSPORT_PROTOCOL_ID = (byte) 0xE5;
    public static final byte KROMEK_SERIAL_REPORTS_IN_UTC_TIME_ID = (byte) 0xE9;
    public static final byte KROMEK_SERIAL_REPORTS_IN_REMOTE_ISOTOPE_CONFIRMATION_ID = (byte) 0xEB;
    public static final byte KROMEK_SERIAL_REPORTS_IN_REMOTE_ISOTOPE_CONFIRMATION_STATUS_ID = (byte) 0xEC;
    public static final byte KROMEK_SERIAL_REPORTS_IN_REMOTE_BACKGROUND_COLLECTION_ID = (byte) 0xED;
    public static final byte KROMEK_SERIAL_REPORTS_IN_REMOTE_BACKGROUND_COLLECTION_STATUS_ID = (byte) 0xEE;
    public static final byte KROMEK_SERIAL_REPORTS_IN_WIFI_AP_CONFIG_ID = (byte) 0xF0;
    public static final byte KROMEK_SERIAL_REPORTS_IN_UI_BT_ENABLE_ID = (byte) 0xF1;
    public static final byte KROMEK_SERIAL_REPORTS_IN_UI_ALERT_CONFIG_ID = (byte) 0xF2;
    public static final byte KROMEK_SERIAL_REPORTS_IN_UI_CONFIRMATION_MODE_ID = (byte) 0xF3;
    public static final byte KROMEK_SERIAL_REPORTS_IN_UI_SEARCH_ID_ENABLE_ID = (byte) 0xF4;
    public static final byte KROMEK_SERIAL_REPORTS_IN_UI_ISOTOPE_ENABLE_ID = (byte) 0xF5;
    public static final byte KROMEK_SERIAL_REPORTS_IN_UI_PIN_CODE_ID = (byte) 0xF6;
    public static final byte KROMEK_SERIAL_REPORTS_IN_FORCE_BATTERY_SELECT_ID = (byte) 0xF9;
    public static final byte KROMEK_SERIAL_REPORTS_IN_POWER_INFO_ID = (byte) 0xFA;
    public static final byte KROMEK_SERIAL_REPORTS_IN_REMOTE_EXT_ISOTOPE_CONFIRMATION_STATUS_ID = (byte) 0xFB;
    public static final byte KROMEK_SERIAL_REPORTS_IN_UNIT_ID_ID = (byte) 0xFC;
    public static final byte KROMEK_SERIAL_REPORTS_IN_ALARM_AUTO_SILENCE_ID = (byte) 0xFD;

    //KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD_EXT specific IN reports
    public static final byte KROMEK_SERIAL_REPORTS_OUT_ALPHABETA_UI_CONFIG_REPORT = (byte) 0x01;
    //public static final byte KROMEK_SERIAL_REPORTS_OUT_RADIOMETRIC_STATUS_REPORT = (byte) 0x02; //NOT USED, IN only
    //public static final byte KROMEK_SERIAL_REPORTS_OUT_ALPHABETA_RADIOMETRICS_ID = (byte) 0x03; //NOT USED, IN only
    public static final byte KROMEK_SERIAL_REPORTS_OUT_SETTINGS_ITEM_PIN_ENABLE_ID = (byte) 0x04;
    public static final byte KROMEK_SERIAL_REPORTS_OUT_DOSE_RATE_AVERAGING_PERIOD_ID = (byte) 0x05;

    //KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD_EXT specific OUT reports
    public static final byte KROMEK_SERIAL_REPORTS_IN_ALPHABETA_UI_CONFIG_REPORT = (byte) 0x81;
    public static final byte KROMEK_SERIAL_REPORTS_IN_RADIOMETRIC_STATUS_REPORT = (byte) 0x82;
    public static final byte KROMEK_SERIAL_REPORTS_IN_ALPHABETA_RADIOMETRICS_ID = (byte) 0x83;
    public static final byte KROMEK_SERIAL_REPORTS_IN_SETTINGS_ITEM_PIN_ENABLE_ID = (byte) 0x84;
    public static final byte KROMEK_SERIAL_REPORTS_IN_DOSE_RATE_AVERAGING_PERIOD_ID = (byte) 0x85;

    public static final int KROMEK_SERIAL_REPORTS_ERROR_STRING_SIZE = 50;
    public static final int KROMEK_SERIAL_REPORTS_SERIALNUMBER_SIZE = 50;
    public static final int KROMEK_SERIAL_REPORTS_PRODUCTNAME_SIZE = 50;
    public static final int KROMEK_SERIAL_REPORTS_HEALTH_STATUS_DATA_SIZE = 32;

    public static final int KROMEK_SERIAL_REPORTS_IN_SPECTRUM_MAX_BINS = 4096;
    public static final int KROMEK_SERIAL_REPORTS_IN_SPECTRUM_ENERGY_MAX_BINS = 1024;

//    // Use this for clarity in code. All IN requests from the host are simply a header
//    public static final byte  KromekSerialReportRequest = KromekSerialReportHeader;
//    public static final byte  KromekSerialReportRequestWithIndex = KromekSerialReportHeaderWithIndex;


}
