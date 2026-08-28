package com.botts.impl.service.oscar.stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Statistics {
    // Occupancies
    private long numOccupancies = 0;
    private long numGammaAlarms = 0;
    private long numNeutronAlarms = 0;
    private long numGammaNeutronAlarms = 0;
    // Faults
    private long numFaults = 0;
    private long numGammaFaults = 0;
    private long numNeutronFaults = 0;
    private long numTampers = 0;
    // Per-lane breakdown (empty when running on a node with no registered lanes)
    private List<LaneStatistics> perLane = new ArrayList<>();

    public long getNumOccupancies() {
        return numOccupancies;
    }

    public long getNumGammaAlarms() {
        return numGammaAlarms;
    }

    public long getNumNeutronAlarms() {
        return numNeutronAlarms;
    }

    public long getNumFaults() {
        return numFaults;
    }

    public long getNumTampers() {
        return numTampers;
    }

    public long getNumGammaNeutronAlarms() {
        return numGammaNeutronAlarms;
    }

    public long getNumGammaFaults() {
        return numGammaFaults;
    }

    public long getNumNeutronFaults() {
        return numNeutronFaults;
    }

    public List<LaneStatistics> getPerLane() {
        return perLane;
    }

    public static class Builder {

        Statistics instance;

        public Builder() {
            this.instance = new Statistics();
        }

        public Builder numOccupancies(long numOccupancies) {
            instance.numOccupancies = numOccupancies;
            return this;
        }

        public Builder numGammaAlarms(long numGammaAlarms) {
            instance.numGammaAlarms = numGammaAlarms;
            return this;
        }

        public Builder numNeutronAlarms(long numNeutronAlarms) {
            instance.numNeutronAlarms = numNeutronAlarms;
            return this;
        }

        public Builder numGammaNeutronAlarms(long numGammaNeutronAlarms) {
            instance.numGammaNeutronAlarms = numGammaNeutronAlarms;
            return this;
        }

        public Builder numFaults(long numFaults) {
            instance.numFaults = numFaults;
            return this;
        }

        public Builder numGammaFaults(long numGammaFaults) {
            instance.numGammaFaults = numGammaFaults;
            return this;
        }

        public Builder numNeutronFaults(long numNeutronFaults) {
            instance.numNeutronFaults = numNeutronFaults;
            return this;
        }

        public Builder numTampers(long numTampers) {
            instance.numTampers = numTampers;
            return this;
        }

        public Builder perLane(List<LaneStatistics> perLane) {
            instance.perLane = perLane != null ? perLane : new ArrayList<>();
            return this;
        }

        public Statistics build() {
            return instance;
        }

    }

    public Statistics add(Statistics other) {
        return new Statistics.Builder()
                .numOccupancies(this.numOccupancies + other.numOccupancies)
                .numGammaAlarms(this.numGammaAlarms + other.numGammaAlarms)
                .numNeutronAlarms(this.numNeutronAlarms + other.numNeutronAlarms)
                .numGammaNeutronAlarms(this.numGammaNeutronAlarms + other.numGammaNeutronAlarms)
                .numFaults(this.numFaults + other.numFaults)
                .numGammaFaults(this.numGammaFaults + other.numGammaFaults)
                .numNeutronFaults(this.numNeutronFaults + other.numNeutronFaults)
                .numTampers(this.numTampers + other.numTampers)
                .perLane(mergePerLane(this.perLane, other.perLane))
                .build();
    }

    public static Statistics zero() {
        return new Statistics.Builder().build(); // all zeros
    }

    // Merge two per-lane lists by laneUID, summing counts and weighting the
    // adjudication-latency average by adjudication count.
    private static List<LaneStatistics> mergePerLane(List<LaneStatistics> a, List<LaneStatistics> b) {
        if ((a == null || a.isEmpty()) && (b == null || b.isEmpty()))
            return new ArrayList<>();

        Map<String, LaneStatistics> merged = new LinkedHashMap<>();
        if (a != null) for (var s : a) merged.put(s.getLaneUID(), s);
        if (b != null) {
            for (var rhs : b) {
                var lhs = merged.get(rhs.getLaneUID());
                merged.put(rhs.getLaneUID(), lhs == null ? rhs : lhs.add(rhs));
            }
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * Per-lane breakdown carried alongside the node-level counts. Adjudication
     * latency is stored in seconds and combined with a count-weighted average.
     */
    public static class LaneStatistics {
        private final String laneUID;
        private final long numOccupancies;
        private final long numGammaAlarms;
        private final long numNeutronAlarms;
        private final long numGammaNeutronAlarms;
        private final long numFaults;
        private final long numGammaFaults;
        private final long numNeutronFaults;
        private final long numTampers;
        private final long numAdjudicated;
        private final double avgTimeToAdjudicateSec;

        private LaneStatistics(Builder b) {
            this.laneUID = b.laneUID;
            this.numOccupancies = b.numOccupancies;
            this.numGammaAlarms = b.numGammaAlarms;
            this.numNeutronAlarms = b.numNeutronAlarms;
            this.numGammaNeutronAlarms = b.numGammaNeutronAlarms;
            this.numFaults = b.numFaults;
            this.numGammaFaults = b.numGammaFaults;
            this.numNeutronFaults = b.numNeutronFaults;
            this.numTampers = b.numTampers;
            this.numAdjudicated = b.numAdjudicated;
            this.avgTimeToAdjudicateSec = b.avgTimeToAdjudicateSec;
        }

        public String getLaneUID() { return laneUID; }
        public long getNumOccupancies() { return numOccupancies; }
        public long getNumGammaAlarms() { return numGammaAlarms; }
        public long getNumNeutronAlarms() { return numNeutronAlarms; }
        public long getNumGammaNeutronAlarms() { return numGammaNeutronAlarms; }
        public long getNumFaults() { return numFaults; }
        public long getNumGammaFaults() { return numGammaFaults; }
        public long getNumNeutronFaults() { return numNeutronFaults; }
        public long getNumTampers() { return numTampers; }
        public long getNumAdjudicated() { return numAdjudicated; }
        public double getAvgTimeToAdjudicateSec() { return avgTimeToAdjudicateSec; }

        public LaneStatistics add(LaneStatistics other) {
            long totalAdj = this.numAdjudicated + other.numAdjudicated;
            double avg = totalAdj == 0 ? 0.0
                    : (this.avgTimeToAdjudicateSec * this.numAdjudicated
                       + other.avgTimeToAdjudicateSec * other.numAdjudicated) / totalAdj;
            return new Builder()
                    .laneUID(this.laneUID)
                    .numOccupancies(this.numOccupancies + other.numOccupancies)
                    .numGammaAlarms(this.numGammaAlarms + other.numGammaAlarms)
                    .numNeutronAlarms(this.numNeutronAlarms + other.numNeutronAlarms)
                    .numGammaNeutronAlarms(this.numGammaNeutronAlarms + other.numGammaNeutronAlarms)
                    .numFaults(this.numFaults + other.numFaults)
                    .numGammaFaults(this.numGammaFaults + other.numGammaFaults)
                    .numNeutronFaults(this.numNeutronFaults + other.numNeutronFaults)
                    .numTampers(this.numTampers + other.numTampers)
                    .numAdjudicated(totalAdj)
                    .avgTimeToAdjudicateSec(avg)
                    .build();
        }

        public static class Builder {
            private String laneUID = "";
            private long numOccupancies;
            private long numGammaAlarms;
            private long numNeutronAlarms;
            private long numGammaNeutronAlarms;
            private long numFaults;
            private long numGammaFaults;
            private long numNeutronFaults;
            private long numTampers;
            private long numAdjudicated;
            private double avgTimeToAdjudicateSec;

            public Builder laneUID(String v) { this.laneUID = v; return this; }
            public Builder numOccupancies(long v) { this.numOccupancies = v; return this; }
            public Builder numGammaAlarms(long v) { this.numGammaAlarms = v; return this; }
            public Builder numNeutronAlarms(long v) { this.numNeutronAlarms = v; return this; }
            public Builder numGammaNeutronAlarms(long v) { this.numGammaNeutronAlarms = v; return this; }
            public Builder numFaults(long v) { this.numFaults = v; return this; }
            public Builder numGammaFaults(long v) { this.numGammaFaults = v; return this; }
            public Builder numNeutronFaults(long v) { this.numNeutronFaults = v; return this; }
            public Builder numTampers(long v) { this.numTampers = v; return this; }
            public Builder numAdjudicated(long v) { this.numAdjudicated = v; return this; }
            public Builder avgTimeToAdjudicateSec(double v) { this.avgTimeToAdjudicateSec = v; return this; }

            public LaneStatistics build() { return new LaneStatistics(this); }
        }
    }

}
