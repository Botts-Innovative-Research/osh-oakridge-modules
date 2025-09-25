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

package com.botts.impl.service.oscar.siteinfo;


import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class SitemapDiagramHandler {

    ISensorHub hub;

//    FileService fileService;

    public SitemapDiagramHandler(ISensorHub hub) {
        this.hub = hub;
    }

    public void handleFile(String filename) {
        File file = new File(filename);

    }

    public OutputStream handleUpload(String filename) throws FileNotFoundException {
        System.out.println("Receiving file: " + filename);
        // TODO: Better logic to specify where config should be
        File file = new File(filename);

        return new FileOutputStream(file);
    }
}
