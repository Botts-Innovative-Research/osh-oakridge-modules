package com.botts.impl.service.oscar;

import org.sensorhub.api.datastore.DataStoreException;

import java.io.OutputStream;

public interface IFileHandler {

    boolean isValidFileType(String fileName, String mimeType);

    boolean handleFile(String filename);

    OutputStream handleUpload(String filename) throws DataStoreException;

}
