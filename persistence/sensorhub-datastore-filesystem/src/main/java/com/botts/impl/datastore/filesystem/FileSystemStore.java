/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.datastore.filesystem;

import com.botts.api.datastore.bucket.BucketStorageKey;
import com.botts.api.datastore.bucket.BucketStorageObject;
import com.botts.api.datastore.bucket.BucketStorageQueryFilter;
import com.botts.api.datastore.bucket.IBucketStore;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class FileSystemStore implements IBucketStore {

    private final String rootDirectory;

    public FileSystemStore(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public String getDatastoreName() {
        return rootDirectory;
    }

    @Override
    public Stream<Entry<BucketStorageKey, BucketStorageObject>> selectEntries(BucketStorageQueryFilter query, Set<BucketStorageField> fields) {
        return Stream.empty();
    }

    @Override
    public void commit() throws DataStoreException {

    }

    @Override
    public void backup(OutputStream is) throws IOException {

    }

    @Override
    public void restore(InputStream os) throws IOException {

    }

    @Override
    public BucketStorageQueryFilter selectAllFilter() {
        return null;
    }


    @Override
    public void createBucket(String bucketName, Map<String, String> metadata) {

    }

    @Override
    public void deleteBucket(String bucketName) {

    }

    @Override
    public boolean bucketExists(String bucketName) {
        return false;
    }

    @Override
    public Stream<String> listBuckets() {
        return Stream.empty();
    }

    @Override
    public BucketStorageKey putObject(String bucketName, String key, InputStream data, String contentType, Map<String, String> metadata) {
        return null;
    }

    @Override
    public BucketStorageObject getObject(String bucketName, String key) {
        return null;
    }

    @Override
    public InputStream getObjectStream(String bucketName, String key) {
        return null;
    }

    @Override
    public boolean deleteObject(String bucketName, String key) {
        return false;
    }

    @Override
    public void copyObject(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {

    }

    @Override
    public Stream<BucketStorageObject> listObjects(String bucketName) {
        return Stream.empty();
    }

    @Override
    public BucketStorageObject get(Object key) {
        return null;
    }

    @Override
    public BucketStorageObject put(BucketStorageKey key, BucketStorageObject value) {
        return null;
    }

    @Override
    public BucketStorageObject remove(Object key) {
        return null;
    }

    @Override
    public void clear() {

    }
}
