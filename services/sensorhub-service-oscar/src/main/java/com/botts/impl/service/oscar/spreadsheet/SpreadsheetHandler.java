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

package com.botts.impl.service.oscar.spreadsheet;

import com.botts.api.service.bucket.IBucketService;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public class SpreadsheetHandler {

    ISensorHub hub;
    SpreadsheetParser parser;

    public SpreadsheetHandler(ISensorHub hub) {
        this.hub = hub;
    }

    public void handleFile(String filepath) {
        parser.loadFile(Path.of(filepath));
    }

    public OutputStream handleUpload(String filename) throws DataStoreException {
        System.out.println("Receiving file: " + filename);

        return null;
    }

    private void handleCSV(String csvData) {
        parser.load(csvData);
    }

}
