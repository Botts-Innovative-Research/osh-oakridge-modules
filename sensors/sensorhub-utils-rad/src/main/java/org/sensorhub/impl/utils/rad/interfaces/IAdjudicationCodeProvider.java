package org.sensorhub.impl.utils.rad.interfaces;

/** Supplies service-configured disposition codes without a lane-to-service dependency. */
public interface IAdjudicationCodeProvider {
    boolean isCustomAdjudicationCode(int code);
}
