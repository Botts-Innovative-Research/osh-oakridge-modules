package com.botts.impl.service.oscar.spreadsheet;

import com.botts.impl.system.lane.LaneSystem;
import org.sensorhub.impl.module.ModuleRegistry;

import java.util.List;

public class SpreadsheetBinding {

    public static final String[] HEADERS = {"RowType", "Name", "UniqueID", "AutoStart", };

    private ModuleRegistry registry;

    public SpreadsheetBinding(ModuleRegistry registry) {
        this.registry = registry;
    }

    public String serialize() {
        // TODO: Convert module registry loaded lanes to CSV
        List<LaneSystem> lanes = registry.getLoadedModules(LaneSystem.class).stream().toList();
        if (lanes.isEmpty())
            return "";
        // parse lanes
        return "";
    }

    public void deserialize(String csv) {
        // TODO:
    }

}
