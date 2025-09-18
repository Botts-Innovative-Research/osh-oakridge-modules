package com.botts.impl.service.oscar.video;

public class VideoRetention extends Thread {

    VideoRetention(int maxTime, boolean threeFrame) {
        super();
        threadSettings();
    }

    private void threadSettings() {
        this.setDaemon(true);
        this.setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {

    }
}
