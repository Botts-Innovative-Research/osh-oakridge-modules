package com.botts.impl.service.bucket;

import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BucketStorageSecurity extends ModuleSecurity {

    private final Map<String, BucketPermission> bucketPermissions;

    public final IPermission api_get;
    public final IPermission api_put;
    public final IPermission api_delete;
    public final IPermission api_create;

    public class BucketPermission {
        public IPermission get;
        public IPermission put;
        public IPermission delete;
        public IPermission create;
    }

    public BucketStorageSecurity(BucketStorageService module, Collection<String> buckets, boolean enable) {
        super(module, "bucketservice", enable);

        this.bucketPermissions = new HashMap<>();

        this.api_get = new ItemPermission(rootPerm, "get");

        this.api_put = new ItemPermission(rootPerm, "put");

        this.api_delete = new ItemPermission(rootPerm, "delete");

        this.api_create = new ItemPermission(rootPerm, "create");

        buckets.forEach(bucket -> {
            BucketPermission bucketPermission = new BucketPermission();
            bucketPermission.get = new ItemPermission(api_get, bucket);
            bucketPermission.create = new ItemPermission(api_create, bucket);
            bucketPermission.delete = new ItemPermission(api_delete, bucket);
            bucketPermission.put = new ItemPermission(api_put, bucket);
            bucketPermissions.put(bucket, bucketPermission);
        });

    }

    public Map<String, BucketPermission> getBucketPermissions() {
        return bucketPermissions;
    }

    public BucketPermission getBucketPermission(String bucket) {
        return bucketPermissions.get(bucket);
    }


}
