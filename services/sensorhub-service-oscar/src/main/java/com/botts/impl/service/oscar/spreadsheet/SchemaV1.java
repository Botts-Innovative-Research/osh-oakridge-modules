package com.botts.impl.service.oscar.spreadsheet;

import java.util.ArrayList;
import java.util.List;

public class SchemaV1 implements Schema {

    public List<String> headers = new ArrayList<>();



    public SchemaV1() {
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

    @Override
    public List<String> getHeaders() {
        return headers;
    }
}
