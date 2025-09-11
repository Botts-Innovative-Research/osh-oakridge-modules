package com.botts.impl.service.oscar.reports;

import com.botts.impl.service.oscar.OSCARSystem;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.vast.swe.SWEHelper;

import java.util.concurrent.CompletableFuture;

public class RequestReportControl extends AbstractControlInterface<OSCARSystem> {

    public static final String NAME = "requestReport";
    public static final String LABEL = "Request Report";
    public static final String DESCRIPTION = "Control to request operations, performance, and maintenance reports";

    DataComponent commandStructure;
    SWEHelper fac;

    enum ReportType {
        REPORT_1,
        REPORT_2,
        I_FORGOT_WHAT_TYPES_THERE_ARE
    }

    protected RequestReportControl(OSCARSystem parent) {
        super(NAME, parent);

        commandStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("startDateTime", fac.createTime()
                        .withIso8601Format()
                        .description("Start datetime (ISO 8601)"))
                .addField("endDateTime", fac.createTime()
                        .withIso8601Format()
                        .description("End datetime (ISO 8601)"))
                .addField("reportType", fac.createCategory()
                        .description("Report type to request")
                        .addAllowedValues(ReportType.class))
                .build();
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandStructure;
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {
        return null;
    }

}
