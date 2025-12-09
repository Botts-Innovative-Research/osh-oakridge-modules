package com.botts.impl.service.oscar.stats;

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
                .build();
    }

    public static Statistics zero() {
        return new Statistics.Builder().build(); // all zeros
    }

}
