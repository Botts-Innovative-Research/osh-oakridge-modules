package com.botts.impl.service.oscar.reports.helpers;

public enum ReportCmdType {
    RDS_SITE,
    RDS_SITE_PDF,
    RDS_SITE_CSV,
    RDS_SITE_JSON,
    RDS_SITE_XLSX,
    LANE,
    LANE_PDF,
    LANE_CSV,
    LANE_JSON,
    LANE_XLSX,
    EVENT,
    EVENT_PDF,
    EVENT_CSV,
    EVENT_JSON,
    EVENT_XLSX,
    ADJUDICATION,
    ADJUDICATION_PDF,
    ADJUDICATION_CSV,
    ADJUDICATION_JSON,
    ADJUDICATION_XLSX,
    ADJUDICATION_DETAILS,
    ADJUDICATION_DETAILS_PDF,
    ADJUDICATION_DETAILS_CSV,
    ADJUDICATION_DETAILS_JSON,
    ADJUDICATION_DETAILS_XLSX,
    VEHICLE_INCIDENT,
    VEHICLE_INCIDENT_PDF,
    VEHICLE_INCIDENT_CSV,
    VEHICLE_INCIDENT_JSON,
    VEHICLE_INCIDENT_XLSX;

    public boolean isAdjudicationDetails() {
        return baseType() == ADJUDICATION_DETAILS;
    }

    public boolean isVehicleIncident() {
        return baseType() == VEHICLE_INCIDENT;
    }

    public ReportCmdType baseType() {
        return switch (this) {
            case RDS_SITE, RDS_SITE_PDF, RDS_SITE_CSV, RDS_SITE_JSON, RDS_SITE_XLSX -> RDS_SITE;
            case LANE, LANE_PDF, LANE_CSV, LANE_JSON, LANE_XLSX -> LANE;
            case EVENT, EVENT_PDF, EVENT_CSV, EVENT_JSON, EVENT_XLSX -> EVENT;
            case ADJUDICATION, ADJUDICATION_PDF, ADJUDICATION_CSV, ADJUDICATION_JSON, ADJUDICATION_XLSX -> ADJUDICATION;
            case ADJUDICATION_DETAILS,
                 ADJUDICATION_DETAILS_PDF,
                 ADJUDICATION_DETAILS_CSV,
                 ADJUDICATION_DETAILS_JSON,
                 ADJUDICATION_DETAILS_XLSX -> ADJUDICATION_DETAILS;
            case VEHICLE_INCIDENT,
                 VEHICLE_INCIDENT_PDF,
                 VEHICLE_INCIDENT_CSV,
                 VEHICLE_INCIDENT_JSON,
                 VEHICLE_INCIDENT_XLSX -> VEHICLE_INCIDENT;
        };
    }

    public ReportFormat defaultFormat() {
        String name = name();
        if (name.endsWith("_CSV")) {
            return ReportFormat.CSV;
        }
        if (name.endsWith("_JSON")) {
            return ReportFormat.JSON;
        }
        if (name.endsWith("_XLSX")) {
            return ReportFormat.XLSX;
        }
        return ReportFormat.PDF;
    }
}
