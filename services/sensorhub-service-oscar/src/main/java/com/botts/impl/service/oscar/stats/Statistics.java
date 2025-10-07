package com.botts.impl.service.oscar.stats;

public class Statistics {

    private long numOccupancies = 0;
    private long numGammaAlarms = 0;
    private long numNeutronAlarms = 0;
    private long numFaults = 0;
    private long numTampers = 0;

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

        public Builder numFaults(long numFaults) {
            instance.numFaults = numFaults;
            return this;
        }

        public Builder numTampers(long numTampers) {
            instance.numTampers = numTampers;
            return this;
        }

        public Statistics build() {
            return instance;
        }

    }

}
