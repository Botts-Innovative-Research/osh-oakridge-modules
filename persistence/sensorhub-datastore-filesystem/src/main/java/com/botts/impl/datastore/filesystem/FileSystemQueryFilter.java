package com.botts.impl.datastore.filesystem;

import org.sensorhub.api.datastore.IQueryFilter;

public class FileSystemQueryFilter implements IQueryFilter {

    @Override
    public long getLimit() {
        return 0;
    }

}
