package com.botts.impl.datastore.filesystem;

import com.botts.api.datastore.bucket.BucketStorageObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileSystemObject implements BucketStorageObject {

    private File file;

    public FileSystemObject(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public InputStream getContent() throws IOException {
        return new FileInputStream(file);
    }

}
