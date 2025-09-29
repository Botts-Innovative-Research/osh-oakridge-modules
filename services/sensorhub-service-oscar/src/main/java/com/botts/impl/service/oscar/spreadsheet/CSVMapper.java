package com.botts.impl.service.oscar.spreadsheet;

import java.util.Map;

public abstract class CSVMapper<ConfigType> {

    Schema schema;

    public CSVMapper(Schema schema) {
        this.schema = schema;
    }

    abstract ConfigType createConfig(Map<String, String> csv);

    abstract String createCSV(ConfigType config);

}
