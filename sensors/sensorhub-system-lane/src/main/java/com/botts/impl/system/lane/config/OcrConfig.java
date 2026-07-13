/*******************************************************************************

  The contents of this file are subject to the Mozilla Public License, v. 2.0.
  If a copy of the MPL was not distributed with this file, You can obtain one
  at http://mozilla.org/MPL/2.0/.

  Software distributed under the License is distributed on an "AS IS" basis,
  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
  for the specific language governing rights and limitations under the License.

  The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
  Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-lane vehicle OCR settings. The feature is strictly opt-in: when
 * disabled (the default) the lane registers no OCR datastream, subscribes to
 * nothing, and no OCR runtime or models are ever loaded on the node.
 */
public class OcrConfig {

    @DisplayInfo(label = "Enable Vehicle OCR", desc = "Master switch. When enabled, camera recordings of "
            + "alarming occupancies are OCR'd for shipping container numbers and license plates, and the "
            + "results are published on the lane's vehicleOcr datastream.")
    public boolean enabled = false;

    @DisplayInfo(label = "Model Directory", desc = "Directory containing det.onnx, rec.onnx and dict.txt, "
            + "relative to the node directory or absolute.")
    public String modelDir = "models/ocr";

    @DisplayInfo(label = "Read Container Numbers", desc = "Look for ISO 6346 shipping container numbers.")
    public boolean readContainerIds = true;

    @DisplayInfo(label = "Read License Plates", desc = "Look for license plate numbers.")
    public boolean readPlates = true;

    @DisplayInfo(label = "Camera UIDs", desc = "Only OCR recordings from these camera system UIDs. "
            + "Leave empty to use all of the lane's cameras.")
    public List<String> cameraUids = new ArrayList<>();

    @DisplayInfo(label = "Frames Per Clip", desc = "Number of keyframes sampled from each recorded clip.")
    public int framesPerVideo = 8;

    @DisplayInfo(label = "Container Min Confidence", desc = "Container reads below this confidence are "
            + "dropped unless their ISO 6346 check digit validates.")
    public double containerMinConfidence = 0.5;

    @DisplayInfo(label = "Plate Min Confidence", desc = "Plate reads below this confidence are dropped.")
    public double plateMinConfidence = 0.6;

    @DisplayInfo(label = "Plate Autofill Confidence", desc = "The operator UI only auto-fills a plate at or "
            + "above this confidence; below it, the read is offered as a click-to-apply suggestion.")
    public double plateAutofillConfidence = 0.85;
}
