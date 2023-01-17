/*
 * Copyright 2018 Google LLC All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.tjado.bluetooth;

/** Helper class to store the battery state and retrieve the binary report. */
class BatteryReport {

    private final byte[] batteryData = new byte[] {0};

    /**
     * Store the current battery level in the report.
     *
     * @param level Battery level, must be in the [0.0, 1.0] interval
     * @return Byte array that represents the report
     */
    byte[] setValue(float level) {
        int val = (int) Math.ceil(level * 255);
        batteryData[0] = (byte) (val & 0xff);
        return batteryData;
    }

    byte[] getReport() {
        return batteryData;
    }

    /** Interface to send the Battery data with. */
    public interface BatteryDataSender {
        /**
         * Send the Battery data to the connected HID Host device.
         *
         * @param level Current battery level
         */
        void sendBatteryLevel(float level);
    }
}
