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
 * A simple data structure to hold data buffers with their timestamp computed from the TransportStream
 *
 * @author Nick Garay
 * @since Apr. 1, 2020
 */
public class DataBufferRecord implements Cloneable {

    /** Matches FFmpeg's AV_NOPTS_VALUE without coupling this data carrier to a native pointer. */
    public static final long NO_TIMESTAMP = Long.MIN_VALUE;

    /**
     * The timestamp associated for the data
     */
    double presentationTimeStamp;

    boolean keyFrame;

    /** Original packet timing, kept in the source stream time base for lossless remuxing. */
    long packetPts = NO_TIMESTAMP;
    long packetDts = NO_TIMESTAMP;
    long packetDuration;
    int timeBaseNumerator;
    int timeBaseDenominator;

    /**
     * The data buffer to be stored
     */
    byte[] dataBuffer;

    /**
     * Constructor
     *
     * @param presentationTimeStamp  The presentationTimeStamp associated for the data
     * @param dataBuffer The data buffer to be stored
     */
    public DataBufferRecord(double presentationTimeStamp, byte[] dataBuffer) {

        this.presentationTimeStamp = presentationTimeStamp;
        this.dataBuffer = dataBuffer;
        keyFrame = false;
    }

    public DataBufferRecord(double presentationTimeStamp, byte[] dataBuffer, boolean isKeyFrame) {

        this.presentationTimeStamp = presentationTimeStamp;
        this.dataBuffer = dataBuffer;
        keyFrame = isKeyFrame;
    }

    public DataBufferRecord(double presentationTimeStamp, byte[] dataBuffer, boolean isKeyFrame,
                            long packetPts, long packetDts, long packetDuration,
                            int timeBaseNumerator, int timeBaseDenominator) {
        this.presentationTimeStamp = presentationTimeStamp;
        this.dataBuffer = dataBuffer;
        this.keyFrame = isKeyFrame;
        this.packetPts = packetPts;
        this.packetDts = packetDts;
        this.packetDuration = packetDuration;
        this.timeBaseNumerator = timeBaseNumerator;
        this.timeBaseDenominator = timeBaseDenominator;
    }

    /**
     * Returns the timestamp associated with this data record
     *
     * @return timestamp value
     */
    public double getPresentationTimestamp() {
        return presentationTimeStamp;
    }

    /**
     * Returns the data buffer associated with this data record as a byte array
     *
     * @return the data byte array
     */
    public byte[] getDataBuffer() {
        return dataBuffer;
    }

    public boolean isKeyFrame() { return keyFrame; }

    public boolean hasPacketTiming() {
        return timeBaseNumerator > 0 && timeBaseDenominator > 0
                && (packetPts != NO_TIMESTAMP || packetDts != NO_TIMESTAMP);
    }

    public long getPacketPts() { return packetPts; }

    public long getPacketDts() { return packetDts; }

    public long getPacketDuration() { return packetDuration; }

    public int getTimeBaseNumerator() { return timeBaseNumerator; }

    public int getTimeBaseDenominator() { return timeBaseDenominator; }


    @Override
    public DataBufferRecord clone() {
        try {
            DataBufferRecord clone = (DataBufferRecord) super.clone();
            clone.dataBuffer = dataBuffer.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
