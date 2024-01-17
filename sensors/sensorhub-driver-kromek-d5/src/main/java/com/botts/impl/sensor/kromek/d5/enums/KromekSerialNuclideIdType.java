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

public enum KromekSerialNuclideIdType {
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Am_241("Am-241", "Americium 241"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Ba_133("Ba-133", "Barium-133"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Cs_137("Cs-137", "Cesium-137"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Co_57("Co-57", "Cobalt-57"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Co_60("Co-60", "Cobalt-60"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_EU_152("EU-152", "Europium-152"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_F_18("F-18", "Fluorine-18"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Ga_67("Ga-67", "Gallium-67"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_I_123("I-123", "Iodine-123"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_I_131("I-131", "Iodine-131"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Ir_192("Ir-192", "Iridium-192"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Lu_177("Lu-177", "Lutetium-177"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Lu_177m("Lu 177m", "Lutetium-177M"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Mo_99("Mo-99", "Molybdenum-99"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Np_237("Np-237", "Neptunium-237"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Pu_239("Pu-239", "Plutonium-239"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_WGPu("WGPu", "Weapons Grade Plutonium"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_RGPu("RGPu", "Radiation Grade Plutonium"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_K_40("K-40", "Potassium-40"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Ra_226("Ra-226", "Radium-226"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Na_22("Na-33", "Sodium-23"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Tc_99m("Tc-99m", "Technetium-99m"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Tl_201("Tl-201", "Thallium-201"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Th_232("Th-232", "Thorium-232"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_U_235("U-235", "Uranium-235"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_U_238("U-238", "Uranium-238"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_DU("DU", "Depleted Uranium"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_HEU("HEU", "Highly Enriched Uranium"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_Y_88("Y-88", "Yitrium-88"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_MAX("MAX", "MAX"),
    KROMEK_SERIAL_NUCLIDE_ID_TYPE_UNKNOWN("UNKNOWN", "UNKNOWN");

    private String name;
    private String id;

    KromekSerialNuclideIdType(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}