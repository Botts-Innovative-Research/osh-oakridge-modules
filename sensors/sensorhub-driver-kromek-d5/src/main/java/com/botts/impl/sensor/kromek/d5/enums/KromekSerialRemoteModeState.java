/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (c) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */

package com.botts.impl.sensor.kromek.d5.enums;

public enum KromekSerialRemoteModeState {
    KROMEK_SERIAL_REMOTE_CONTROL_STATE_COMPLETE,
    KROMEK_SERIAL_REMOTE_CONTROL_STATE_CONFIRMATION_IN_PROGRESS,
    KROMEK_SERIAL_REMOTE_CONTROL_STATE_BACKGROUND_IN_PROGRESS,
    KROMEK_SERIAL_REMOTE_CONTROL_STATE_ERROR,
    KROMEK_SERIAL_REMOTE_CONTROL_STATE_INVALID_COMMAND,
    KROMEK_SERIAL_REMOTE_CONTROL_STATE_D5_NOT_READY,
    KROMEK_SERIAL_REMOTE_CONTROL_STATE_INITIALISING
}
