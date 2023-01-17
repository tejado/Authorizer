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

/** Helper class to store the keyboard state and retrieve the binary report. */
public class KeyboardReport {

    private final byte[] keyboardData = "M0ABCDEF".getBytes();

    KeyboardReport() {
        Arrays.fill(keyboardData, (byte) 0);
    }

    byte[] setValue(int modifier, int key1, int key2, int key3, int key4, int key5, int key6) {
        keyboardData[0] = (byte) modifier;
        keyboardData[1] = 0;
        keyboardData[2] = (byte) key1;
        keyboardData[3] = (byte) key2;
        keyboardData[4] = (byte) key3;
        keyboardData[5] = (byte) key4;
        keyboardData[6] = (byte) key5;
        keyboardData[7] = (byte) key6;
        return keyboardData;
    }

    byte[] setValue(byte[] scancode) {
        keyboardData[0] = scancode[0];
        keyboardData[1] = scancode[1];
        keyboardData[2] = scancode[2];
        keyboardData[3] = scancode[3];
        keyboardData[4] = scancode[4];
        keyboardData[5] = scancode[5];
        keyboardData[6] = scancode[6];
        keyboardData[7] = scancode[7];
        return keyboardData;
    }

    byte[] getReport() {
        return keyboardData;
    }

    /** Interface to send the Keyboard data with. */
    public interface KeyboardDataSender {
        /**
         * Send Keyboard data to the connected HID Host device. Up to six buttons pressed
         * simultaneously are supported (not including modifier keys).
         *
         * @param modifier Modifier keys bit mask (Ctrl/Shift/Alt/GUI).
         * @param key1 Scan code of the 1st button that is currently pressed (or 0 if none).
         * @param key2 Scan code of the 2nd button that is currently pressed (or 0 if none).
         * @param key3 Scan code of the 3rd button that is currently pressed (or 0 if none).
         * @param key4 Scan code of the 4th button that is currently pressed (or 0 if none).
         * @param key5 Scan code of the 5th button that is currently pressed (or 0 if none).
         * @param key6 Scan code of the 6th button that is currently pressed (or 0 if none).
         */
        void sendKeyboard(int modifier, int key1, int key2, int key3, int key4, int key5, int key6);
        void sendScancode(byte[] scancode);
    }
}
