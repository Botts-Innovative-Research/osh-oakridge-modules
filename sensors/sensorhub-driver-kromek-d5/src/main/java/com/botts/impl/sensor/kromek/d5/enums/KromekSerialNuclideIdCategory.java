/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (c) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */

package com.botts.impl.sensor.kromek.d5.enums;

public enum KromekSerialNuclideIdCategory {
    KROMEK_SERIAL_NUCLIDE_ID_CATEGORY_SNM("Special Nuclear Material"), // Special Nuclear Material
    KROMEK_SERIAL_NUCLIDE_ID_CATEGORY_Medical("Medical"),
    KROMEK_SERIAL_NUCLIDE_ID_CATEGORY_Industrial("Industrial"),
    KROMEK_SERIAL_NUCLIDE_ID_CATEGORY_NORM("Naturally Ocurring Radioactive Material"), // Naturally Occurring Radioactive Material
    KROMEK_SERIAL_NUCLIDE_ID_CATEGORY_UNKNOWN("UNKNOWN");

    private String name;
     KromekSerialNuclideIdCategory(String name){
         this.name = name;
     }

    public String getName() {
        return name;
    }
}
