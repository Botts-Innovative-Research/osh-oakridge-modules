package org.sensorhub.impl.sensor.ffmpeg.outputs.util;

import java.io.ByteArrayOutputStream;

public class ByteArraySeekableBuffer {
    private ByteArrayOutputStream baos;
    private int position = 0;

    public ByteArraySeekableBuffer(int initialSize) {
        baos = new ByteArrayOutputStream(initialSize);
    }

    public synchronized int write(byte[] data, int offset, int length) {
        // Ensure capacity
        if (position < baos.size()) {
            // overwrite existing bytes
            byte[] buf = baos.toByteArray();
            int end = Math.min(buf.length, position + length);
            System.arraycopy(data, offset, buf, position, end - position);
            baos.reset();
            baos.write(buf, 0, buf.length);
        } else {
            baos.write(data, offset, length);
        }
        position += length;
        return length;
    }

    public synchronized long seek(long offset, int whence) {
        int newPos;
        switch (whence) {
            case 0: // SEEK_SET
                newPos = (int) offset;
                break;
            case 1: // SEEK_CUR
                newPos = position + (int) offset;
                break;
            case 2: // SEEK_END
                newPos = baos.size() + (int) offset;
                break;
            case org.bytedeco.ffmpeg.global.avformat.AVSEEK_SIZE:
                return baos.size();
            default:
                return -1;
        }
        if (newPos < 0) newPos = 0;
        position = newPos;
        return position;
    }

    public synchronized byte[] getData() {
        return baos.toByteArray();
    }

    public synchronized int size() {
        return baos.size();
    }
}

