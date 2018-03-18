/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package info.zamojski.soft.towerinvestigator.utils;

import android.os.Build;

public class DeviceUtils {

    public static String dumpDeviceInfo() {
        StringBuilder sb = new StringBuilder();

        dumpHeader(sb);
        dumpBuild(sb);
        sb.append("\n\n");

        return sb.toString();
    }

    private static void dumpHeader(StringBuilder sb) {
        sb.append("DEVICE INFO:\n");
    }

    private static void dumpBuild(StringBuilder sb) {
        sb.append("\tSDK = ");
        sb.append(Build.VERSION.SDK_INT);
        sb.append("\n");
        sb.append("\tCODENAME = ");
        sb.append(Build.VERSION.CODENAME);
        sb.append("\n");
        sb.append("\tRELEASE = ");
        sb.append(Build.VERSION.RELEASE);
        sb.append("\n");
        sb.append("\tDEVICE = ");
        sb.append(Build.DEVICE);
        sb.append("\n");
        sb.append("\tHARDWARE = ");
        sb.append(Build.HARDWARE);
        sb.append("\n");
        sb.append("\tMANUFACTURER = ");
        sb.append(Build.MANUFACTURER);
        sb.append("\n");
        sb.append("\tMODEL = ");
        sb.append(Build.MODEL);
        sb.append("\n");
        sb.append("\tPRODUCT = ");
        sb.append(Build.PRODUCT);
        sb.append("\n");
        sb.append("\tBRAND = ");
        sb.append(Build.BRAND);
        sb.append("\n");
        sb.append("\tBOOTLOADER = ");
        sb.append(Build.BOOTLOADER);
        sb.append("\n");
        sb.append("\tBOARD = ");
        sb.append(Build.BOARD);
        sb.append("\n");
        sb.append("\tID = ");
        sb.append(Build.ID);
        sb.append("\n");
        sb.append("\tRADIO VERSION = ");
        sb.append(Build.getRadioVersion());
        sb.append("\n");
    }
}
