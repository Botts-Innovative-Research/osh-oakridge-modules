package com.botts.impl.service.bucket;

import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;

public class BucketStorageSecurity extends ModuleSecurity {

    public final IPermission api_get;
    public final IPermission api_put;
    public final IPermission api_delete;
    public final IPermission api_create;

    public BucketStorageSecurity(BucketStorageService module, boolean enable) {
        super(module, "fileservice", enable);
        // TODO: Config security for different directories from root
        BucketStorageServiceConfig config = module.getConfiguration();

        api_get = new ItemPermission(rootPerm, "get");
        api_put = new ItemPermission(rootPerm, "put");
        api_delete = new ItemPermission(rootPerm, "delete");
        api_create = new ItemPermission(rootPerm, "create");
    }
}
