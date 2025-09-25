package com.botts.api.datastore.bucket;

import org.sensorhub.api.datastore.IQueryFilter;

public class BucketStorageQueryFilter implements IQueryFilter {

    @Override
    public long getLimit() {
        return 0;
    }

}
