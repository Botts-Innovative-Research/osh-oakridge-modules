package com.botts.impl.service.bucket;

import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BucketSecurity extends ModuleSecurity {

    private final Map<String, BucketPermissions> bucketPermissions;

    public final IPermission api_get;
    public final IPermission api_put;
    public final IPermission api_delete;
    public final IPermission api_create;
    public final IPermission api_list;

    public class BucketPermissions {
        public IPermission get;
        public IPermission put;
        public IPermission delete;
        public IPermission create;
        public IPermission list;
    }

    public BucketSecurity(BucketService module, Collection<String> buckets, boolean enable) {
        super(module, "bucketservice", enable);

        this.bucketPermissions = new HashMap<>();

        this.api_get = new ItemPermission(rootPerm, "get");
        this.api_put = new ItemPermission(rootPerm, "put");
        this.api_delete = new ItemPermission(rootPerm, "delete");
        this.api_create = new ItemPermission(rootPerm, "create");
        this.api_list = new ItemPermission(rootPerm, "list");

        buckets.forEach(this::addBucket);

        module.getParentHub().getSecurityManager().registerModulePermissions(rootPerm);
    }

    public void addBucket(String bucket) {
        bucketPermissions.put(bucket, createBucketPermissions(bucket));
    }

    private BucketPermissions createBucketPermissions(String bucket) {
        BucketPermissions perms = new BucketPermissions();
        perms.get = new ItemPermission(this.api_get, bucket);
        perms.create = new ItemPermission(this.api_create, bucket);
        perms.delete = new ItemPermission(this.api_delete, bucket);
        perms.put = new ItemPermission(this.api_put, bucket);
        perms.list = new ItemPermission(this.api_list, bucket);
        return perms;
    }

    public Map<String, BucketPermissions> getBucketPermissions() {
        return bucketPermissions;
    }

    public BucketPermissions getBucketPermission(String bucket) {
        return bucketPermissions.get(bucket);
    }

    public void checkParentPermission(IPermission permission, String bucket) {

    }

}
