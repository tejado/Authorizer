/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 * Authors: Siemens AG <max.wittig@siemens.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
 * Copyright (C) 2017  Max Wittig, Siemens AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.tjado.passwdsafe.otp;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import net.tjado.passwdsafe.R;


public class AddTextWatcher implements TextWatcher
{
    private final Button mButton;
    private final EditText mSecret;
    private final EditText mInterval;

    public AddTextWatcher(Activity activity) {
        mButton = (Button) activity.findViewById(R.id.add);
        mSecret = (EditText) activity.findViewById(R.id.secret);
        mInterval = (EditText) activity.findViewById(R.id.interval);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mButton.setEnabled(false);

        if (mSecret.getText().length() < 8)
            return;

        if (mInterval.getText().length() == 0)
            return;

        mButton.setEnabled(true);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
