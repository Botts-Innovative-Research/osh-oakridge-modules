package com.botts.impl.service.oscar.spreadsheet;

import com.botts.impl.system.lane.LaneSystem;
import org.sensorhub.impl.module.ModuleRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SpreadsheetParser {

    public List<String> headers = new ArrayList<>();

    public SpreadsheetParser() {
        headers.addAll(List.of("RowType",
                "Name",
                "UniqueID",
                "AutoStart",
                "Latitude",
                "Longitude",
                "RPMConfigType",
                "RPMHost",
                "RPMPort",
                "AspectAddressStart",
                "AspectAddressEnd",
                "EMLEnabled",
                "EMLCollimated"
        ));
    }

    public void deserialize(String csv) {

    }

    public String serialize(Collection<LaneSystem> laneSystems) {

    }

}
