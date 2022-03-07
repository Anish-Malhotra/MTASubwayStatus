package com.anish.mta.data;

import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

public class MtaLineInfo {

    private final Long inceptionTime;
    private final String line;

    private Stopwatch stopwatch;

    private TripStatus status;

    /*
     * A simple POJO to capture only the line status details that we need
     * Additional logic for setDelayed/setNormal to track delayed/normal uptime
     */

    public MtaLineInfo(String line, Long statusStartTime) {
        this.line = line;
        this.stopwatch = Stopwatch.createUnstarted();
        this.inceptionTime = statusStartTime;
        this.status = TripStatus.NORMAL;
    }

    public TripStatus getStatus() {
        return status;
    }

    public boolean isDelayed() {
        return status.equals(TripStatus.DELAYED);
    }

    public double getRelativeUptime() {
        long totalTime = System.currentTimeMillis() - inceptionTime;
        return 1 - ((double) stopwatch.elapsed(TimeUnit.MILLISECONDS) / totalTime);
    }

    public void setDelayed() {
        this.status = TripStatus.DELAYED;
        this.stopwatch.start();
    }

    public void setNormal() {
        this.status = TripStatus.NORMAL;
        this.stopwatch.stop();
    }

    public enum TripStatus {
        DELAYED, NORMAL;
    }
}
