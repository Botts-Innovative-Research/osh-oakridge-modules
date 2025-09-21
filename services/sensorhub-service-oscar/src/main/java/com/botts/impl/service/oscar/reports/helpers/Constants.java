package com.botts.impl.service.oscar.reports.helpers;

import static org.vast.swe.SWEHelper.getPropertyUri;

public class Constants {

    public static final String DEF_GAMMA = getPropertyUri("gamma-gross-count");
    public static final String DEF_NEUTRON = getPropertyUri("neutron-gross-count");
    public static final String DEF_OCCUPANCY = getPropertyUri("pillar-occupancy-count");
    public static final String DEF_ALARM = getPropertyUri("alarm");
    public static final String DEF_TAMPER = getPropertyUri("tamper-status");
    public static final String DEF_THRESHOLD = getPropertyUri("threshold");

}
