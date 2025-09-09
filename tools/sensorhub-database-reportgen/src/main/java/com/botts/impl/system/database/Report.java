package com.botts.impl.system.database;


public class Report {

    private ReportType reportType;
    private String startTime;
    private String endTime;
    private String outfileName;


    public Report(ReportType type, String startTime, String endTime) {
        this.reportType = type;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getOutfileName() {
        return outfileName;
    }

    public void setOutfileName(String outfileName) {
        this.outfileName = outfileName;
    }
}
