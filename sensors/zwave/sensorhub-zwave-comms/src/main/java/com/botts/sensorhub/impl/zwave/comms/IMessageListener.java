/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023-2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

public interface IMessageListener {

    void onNewDataPacket(String id, String message);
}
