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

import java.util.Arrays;

/** Helper class to store the mouse state and retrieve the binary report. */
public class MouseReport {

    private final byte[] mouseData = "BXYW".getBytes();

    MouseReport() {
        Arrays.fill(mouseData, (byte) 0);
    }

    byte[] setValue(boolean left, boolean right, boolean middle, int x, int y, int wheel) {
        int buttons = ((left ? 1 : 0) | (right ? 2 : 0) | (middle ? 4 : 0));
        mouseData[0] = (byte) buttons;
        mouseData[1] = (byte) x;
        mouseData[2] = (byte) y;
        mouseData[3] = (byte) wheel;
        return mouseData;
    }

    byte[] getReport() {
        return mouseData;
    }

    /** Interface to send the Mouse data with. */
    public interface MouseDataSender {
        /**
         * Send the Mouse data to the connected HID Host device.
         *
         * @param left Left mouse button press state.
         * @param right Right mouse button press state.
         * @param middle Middle mouse button (a.k.a. wheel) press state.
         * @param dX Mouse movement along X axis since the last event.
         * @param dY Mouse movement along Y axis since the last event.
         * @param dWheel Mouse wheel rotation since the last event.
         */
        void sendMouse(boolean left, boolean right, boolean middle, int dX, int dY, int dWheel);
    }
}
