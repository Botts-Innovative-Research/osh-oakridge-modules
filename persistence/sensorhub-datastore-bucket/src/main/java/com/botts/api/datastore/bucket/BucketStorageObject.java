package com.botts.api.datastore.bucket;

import java.io.IOException;
import java.io.InputStream;

public interface BucketStorageObject {

    InputStream getContent() throws IOException;

}
