/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.util.Pair;

/**
 *  Input method for selecting fields from a record
 *
 *  @author Jeff Harris
 */
public class PasswdSafeIME extends InputMethodService
{
    private View itsView;
    private int itsEnterAction = EditorInfo.IME_ACTION_NONE;

    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onCreateInputView()
     */
    @Override
    public View onCreateInputView()
    {
        itsView = getLayoutInflater().inflate(R.layout.input_method, null);
        refresh();

        Button btn;
        btn = (Button)itsView.findViewById(R.id.launch_passwdsafe);
        btn.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                openPasswdSafe();
            }
        });

        OnClickListener fieldListener = new OnClickListener()
        {
            public void onClick(View v)
            {
                sendText(v.getId());
            }
        };
        for (int id: new int[] {
                R.id.user, R.id.password, R.id.url, R.id.email,
                R.id.title, R.id.notes}) {
            btn = (Button)itsView.findViewById(id);
            btn.setOnClickListener(fieldListener);
        }

        OnClickListener commandListener = new OnClickListener()
        {
            public void onClick(View v)
            {
                doBtnCommand(v.getId());
            }
        };
        for (int id: new int[] {R.id.backspace, R.id.return_key, R.id.next}) {
            btn = (Button)itsView.findViewById(id);
            btn.setOnClickListener(commandListener);
        }

        return itsView;
    }

    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onStartInputView(android.view.inputmethod.EditorInfo, boolean)
     */
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting)
    {
        super.onStartInputView(info, restarting);
        refresh();

        int enterText;
        itsEnterAction =
                info.imeOptions & (EditorInfo.IME_MASK_ACTION |
                                   EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        switch (itsEnterAction) {
        case EditorInfo.IME_ACTION_DONE: {
            enterText = R.string.done;
            break;
        }
        case EditorInfo.IME_ACTION_GO: {
            enterText = R.string.go;
            break;
        }
        case EditorInfo.IME_ACTION_NEXT: {
            enterText = R.string.next;
            break;
        }
        case EditorInfo.IME_ACTION_SEARCH: {
            enterText = R.string.search;
            break;
        }
        case EditorInfo.IME_ACTION_SEND: {
            enterText = R.string.send;
            break;
        }
        default: {
            enterText = R.string.execute;
            itsEnterAction = EditorInfo.IME_ACTION_NONE;
            break;
        }
        }
        Button btn = (Button)itsView.findViewById(R.id.return_key);
        btn.setText(enterText);

        // TODO: remove next if doesn't work well
        btn = (Button)itsView.findViewById(R.id.next);
        btn.setVisibility((itsEnterAction != EditorInfo.IME_ACTION_NEXT) ?
                View.VISIBLE : View.GONE);
    }

    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onEvaluateFullscreenMode()
     */
    @Override
    public boolean onEvaluateFullscreenMode()
    {
        // Don't want to enter full-screen mode as not a real keyboard
        return false;
    }

    /** Open PasswdSafe */
    private final void openPasswdSafe()
    {
        Pair<PasswdFileData, PwsRecord> rc = refresh();
        if (rc.first == null) {
            PasswdSafeUtil.startMainActivity("com.jefftharris.passwdsafe",
                                             this);
        } else {
            String uuid = null;
            if (rc.second != null) {
                uuid = rc.first.getUUID(rc.second);
            }
            Intent intent = PasswdSafeUtil.createOpenIntent(
                    rc.first.getUri().getUri(), uuid);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    /** Send text to the connected editor */
    private final void sendText(int btnId)
    {
        InputConnection conn = getCurrentInputConnection();
        if (conn == null) {
            return;
        }

        String str = null;
        Pair<PasswdFileData, PwsRecord> rc = refresh();
        if (rc.second != null) {
            switch (btnId) {
            case R.id.user: {
                str = rc.first.getUsername(rc.second);
                break;
            }
            case R.id.password: {
                str = rc.first.getPassword(rc.second);
                break;
            }
            case R.id.url: {
                str = rc.first.getURL(rc.second);
                break;
            }
            case R.id.email: {
                str = rc.first.getEmail(rc.second);
                break;
            }
            case R.id.title: {
                str = rc.first.getTitle(rc.second);
                break;
            }
            case R.id.notes: {
                str = rc.first.getNotes(rc.second);
                break;
            }
            }
        }
        if (str != null) {
            conn.commitText(str, 1);
        }
    }

    /** Do a button command */
    private final void doBtnCommand(int btnId)
    {
        InputConnection conn = getCurrentInputConnection();
        if (conn == null) {
            return;
        }

        switch (btnId) {
        case R.id.backspace: {
            conn.deleteSurroundingText(1, 0);
            break;
        }
        case R.id.return_key: {
            if (itsEnterAction == EditorInfo.IME_ACTION_NONE) {
                conn.commitText("\n", 1);
            } else {
                conn.performEditorAction(itsEnterAction);
            }
            break;
        }
        case R.id.next: {
            conn.performEditorAction(EditorInfo.IME_ACTION_NEXT);
            break;
        }
        }
    }

    /** Refresh the fields from the current password data */
    private final Pair<PasswdFileData, PwsRecord> refresh()
    {
        // TODO: test file timeouts and file and record deletions
        // TODO: Check field type for password pastes?
        // TODO: show group
        // TODO: disable blank fields?

        PasswdSafeApp app = getPasswdSafeApp();
        PasswdFileData fileData = app.accessOpenFileData();
        PwsRecord rec = null;
        TextView filetv = (TextView)itsView.findViewById(R.id.file);
        if (fileData != null) {
            filetv.setText(fileData.getUri().toString());

            String uuid = app.getLastViewedRecord();
            if (uuid != null) {
                rec = fileData.getRecord(uuid);
            }
        } else {
            filetv.setText("NO FILE");
        }

        TextView rectv = (TextView)itsView.findViewById(R.id.record);
        if (rec != null) {
            rectv.setText(fileData.getTitle(rec));
        } else {
            rectv.setText("NO RECORD");
        }

        return new Pair<PasswdFileData, PwsRecord>(fileData, rec);
    }

    /** Get the PasswdSafeApp */
    private final PasswdSafeApp getPasswdSafeApp()
    {
        return (PasswdSafeApp)getApplication();
    }
}
