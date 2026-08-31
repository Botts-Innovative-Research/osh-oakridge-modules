/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.mpegts;

/**
 * Declares how a {@link DataBufferListener} wants demuxed packets delivered to it.
 * <p>
 * A listener declares exactly one mode, which the {@link MpegTsProcessor} uses to decide
 * which packets it receives. This replaces the previous approach of tagging listeners with
 * marker subclasses and filtering them by {@link Class}.
 *
 * @see MpegTsProcessor#addVideoDataBufferListener(DataBufferListener)
 */
public enum DeliveryMode {

    /**
     * The listener only ever receives packets as they are demuxed, and never receives a replay
     * of the pre-roll frame buffer. This is the default, and the right choice for anything that
     * must stay in step with wall-clock time (live video output, HLS).
     */
    LIVE_ONLY,

    /**
     * The listener receives a replay of the pre-roll frame buffer before it starts receiving live
     * packets. While a replay is in flight the listener receives nothing live, because those
     * packets are still queued and will reach it through the replay; once the buffer drains it
     * receives live packets like everyone else.
     * <p>
     * This is the right choice for recording to a file, where the goal is to capture the seconds
     * leading up to the record command rather than to stay current.
     */
    BUFFERED
}