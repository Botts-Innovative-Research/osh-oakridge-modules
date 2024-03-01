/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023-2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;

import java.util.ArrayList;
import java.util.Map;

public interface IMessageListener {

    void onNewDataPacket(int id, ZWaveEvent message);
}
