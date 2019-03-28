/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.pwsafe.lib.UUID;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;
import org.pwsafe.lib.file.Owner;
import org.pwsafe.lib.file.PwsByteField;
import org.pwsafe.lib.file.PwsField;
import org.pwsafe.lib.file.PwsFieldTypeV2;
import org.pwsafe.lib.file.PwsFieldTypeV3;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileV1;
import org.pwsafe.lib.file.PwsFileV2;
import org.pwsafe.lib.file.PwsFileV3;
import org.pwsafe.lib.file.PwsIntegerField;
import org.pwsafe.lib.file.PwsPasswdField;
import org.pwsafe.lib.file.PwsPasswdUnicodeField;
import org.pwsafe.lib.file.PwsPassword;
import org.pwsafe.lib.file.PwsRecord;
import org.pwsafe.lib.file.PwsRecordV1;
import org.pwsafe.lib.file.PwsRecordV2;
import org.pwsafe.lib.file.PwsRecordV3;
import org.pwsafe.lib.file.PwsStorage;
import org.pwsafe.lib.file.PwsStringField;
import org.pwsafe.lib.file.PwsStringUnicodeField;
import org.pwsafe.lib.file.PwsTimeField;
import org.pwsafe.lib.file.PwsUUIDField;
import org.pwsafe.lib.file.PwsUnknownField;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.util.Pair;

@SuppressWarnings("SameParameterValue")
public class PasswdFileData
{
    private PasswdFileUri itsUri;
    private PwsFile itsPwsFile;
    private final HashMap<String, PwsRecord> itsRecordsByUUID = new HashMap<>();
    private final Map<PwsRecord, PasswdRecord> itsPasswdRecords =
        new IdentityHashMap<>();
    private final ArrayList<PwsRecord> itsRecords = new ArrayList<>();
    private HeaderPasswdPolicies itsHdrPolicies = new HeaderPasswdPolicies();
    private boolean itsIsOpenReadOnly = false;
    private boolean itsIsYubikey = false;

    private static final List<PasswdFileDataObserver> itsObservers =
            new ArrayList<>();

    private static final String TAG = "PasswdFileData";

    private static final int FIELD_UNSUPPORTED = -1;
    private static final int FIELD_NOT_PRESENT = -2;

    public PasswdFileData(PasswdFileUri uri)
    {
        itsUri = uri;
    }

    public void load(Owner<PwsPassword>.Param passwd, boolean readonly,
                     Context context)
            throws IOException, EndOfFileException, InvalidPassphraseException,
                   UnsupportedFileVersionException
    {
        itsIsOpenReadOnly = readonly;
        itsPwsFile = itsUri.load(passwd, context);

        if (itsIsOpenReadOnly || !itsUri.isWritable().first) {
            itsPwsFile.setReadOnly(true);
        }
        finishOpenFile();
    }

    public void createNewFile(Owner<PwsPassword>.Param passwd, Context context)
        throws IOException
    {
        itsPwsFile = itsUri.createNew(passwd, context);
        save(context);
        finishOpenFile();
    }

    public void save(Context context)
        throws IOException, ConcurrentModificationException
    {
        if (itsPwsFile != null) {
            for (int idx = 0; idx < itsRecords.size(); ++idx) {
                PwsRecord rec = itsRecords.get(idx);
                if (rec.isModified()) {
                    PasswdSafeUtil.dbginfo(TAG, "Updating idx: %d", idx);
                    itsPwsFile.set(idx, rec);
                    rec.resetModified();
                }
            }

            setSaveHdrFields(context);

            PwsStorage storage = itsPwsFile.getStorage();
            try {
                storage.setSaveHelper(new PasswdFileUri.SaveHelper(context));
                itsPwsFile.save();
                notifyObservers(this);
            } finally {
                storage.setSaveHelper(null);
            }
        }
    }

    public void close()
    {
        itsUri = null;
        itsPwsFile.dispose();
        itsPwsFile = null;
        indexRecords();
    }

    public ArrayList<PwsRecord> getRecords()
    {
        return itsRecords;
    }

    public PwsRecord getRecord(String uuid)
    {
        return itsRecordsByUUID.get(uuid);
    }

    public PasswdRecord getPasswdRecord(PwsRecord rec)
    {
        return itsPasswdRecords.get(rec);
    }

    /** Get the collection of PasswdRecords in the file */
    public Collection<PasswdRecord> getPasswdRecords()
    {
        return itsPasswdRecords.values();
    }

    public PwsRecord createRecord()
    {
        if (itsPwsFile != null) {
            return itsPwsFile.newRecord();
        } else {
            return null;
        }
    }

    public final void addRecord(PwsRecord rec)
    {
        if (itsPwsFile != null) {
            itsPwsFile.add(rec);
            indexRecords();
        }
    }

    public final boolean removeRecord(PwsRecord rec, Context context)
    {
        int errMsg = 0;
        do {
            if (itsPwsFile == null) {
                errMsg = net.tjado.passwdsafe.R.string.record_not_found;
                break;
            }
            PasswdRecord passwdRec = getPasswdRecord(rec);
            if (passwdRec == null) {
                errMsg = net.tjado.passwdsafe.R.string.record_not_found;
                break;
            }
            if (!passwdRec.getRefsToRecord().isEmpty()) {
                errMsg = net.tjado.passwdsafe.R.string.record_has_references;
                break;
            }

            String recuuid = getUUID(rec);
            if (recuuid == null) {
                errMsg = net.tjado.passwdsafe.R.string.record_not_found;
                break;
            }

            for (int i = 0; i < itsRecords.size(); ++i) {
                PwsRecord r = itsRecords.get(i);
                String ruuid = getUUID(r);
                if (recuuid.equals(ruuid)) {
                    boolean rc = itsPwsFile.removeRecord(i);
                    if (rc) {
                        indexRecords();
                    } else {
                        errMsg = net.tjado.passwdsafe.R.string.record_not_found;
                    }
                    break;
                }
            }
        } while(false);

        if (errMsg != 0) {
            String msg = context.getString(net.tjado.passwdsafe.R.string.cannot_delete_record,
                                           context.getString(errMsg));
            PasswdSafeUtil.showErrorMsg(msg, context);
            return false;
        }
        return true;
    }

    public final void changePasswd(Owner<PwsPassword>.Param passwd)
    {
        itsPwsFile.setPassphrase(passwd);
    }

    public final PasswdFileUri getUri()
    {
        return itsUri;
    }

    /**
     * Get whether a Yubikey was used to open the file
     */
    public final boolean isNotYubikey()
    {
        return !itsIsYubikey;
    }

    /**
     * Set whether a Yubikey was used to open the file
     */
    public final void setYubikey(boolean yubikey)
    {
        itsIsYubikey = yubikey;
    }

    public final boolean canEdit()
    {
        return !itsIsOpenReadOnly &&
               (itsPwsFile != null) &&
               !itsPwsFile.isReadOnly() &&
               ((itsPwsFile.getFileVersionMajor() == PwsFileV3.VERSION) ||
                (itsPwsFile.getFileVersionMajor() == PwsFileV2.VERSION));
    }

    public final boolean canDelete()
    {
        return (itsPwsFile != null) && !itsPwsFile.isReadOnly() &&
               itsUri.isDeletable();
    }

    public final boolean isV3()
    {
        return (itsPwsFile != null) &&
            (itsPwsFile.getFileVersionMajor() == PwsFileV3.VERSION);
    }

    private boolean isV2()
    {
        return (itsPwsFile != null) &&
            (itsPwsFile.getFileVersionMajor() == PwsFileV2.VERSION);
    }

    public final String getOpenPasswordEncoding()
    {
        return (itsPwsFile != null) ? itsPwsFile.getOpenPasswordEncoding() :
            null;
    }

    public final String getId(PwsRecord rec)
    {
        return PasswdRecord.getRecordId(getGroup(rec), getTitle(rec),
                                        getUsername(rec));
    }

    /** Get the time the record was created */
    public final Date getCreationTime(PwsRecord rec)
    {
        return getDateField(rec, PwsRecordV3.CREATION_TIME);
    }

    public final String getEmail(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.EMAIL);
    }

    public final void setEmail(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.EMAIL);
    }

    public final String getGroup(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.GROUP);
    }

    public final void setGroup(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.GROUP);
    }

    /**
     * Split the group into the given list
     */
    public static void splitGroup(String group, ArrayList<String> groups)
    {
        groups.clear();
        String[] splitGroups = TextUtils.split(group, "\\.");
        for (String splitGroup: splitGroups) {
            if (TextUtils.isEmpty(splitGroup)) {
                if (!groups.isEmpty()) {
                    int pos = groups.size() - 1;
                    String last = groups.get(pos);
                    groups.set(pos, last + ".");
                }
            } else {
                groups.add(splitGroup);
            }
        }
    }

    /** Get the time the record was last modified */
    public final Date getLastModTime(PwsRecord rec)
    {
        return getDateField(rec, PwsRecordV3.LAST_MOD_TIME);
    }

    public final String getNotes(PwsRecord rec)
    {
        String s = getField(rec, PwsRecordV3.NOTES);
        if (s != null) {
            s = s.replace("\r\n", "\n");
        }
        return s;
    }

    public final void setNotes(String str, PwsRecord rec)
    {
        if (str != null) {
            str = str.replace("\n", "\r\n");
        }
        setField(str, rec, PwsRecordV3.NOTES);
    }

    public final String getPassword(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.PASSWORD);
    }

    public final void setPassword(String oldPasswd, String newPasswd,
                                  PwsRecord rec)
    {
        PasswdHistory history = getPasswdHistory(rec);
        if ((history != null) && !TextUtils.isEmpty(oldPasswd)) {
            Date passwdDate = getPasswdLastModTime(rec);
            if (passwdDate == null) {
                passwdDate = getCreationTime(rec);
            }
            history.addPasswd(oldPasswd, passwdDate);
            setPasswdHistory(history, rec, false);
        }
        setField(newPasswd, rec, PwsRecordV3.PASSWORD);

        PasswdExpiration expiry = getPasswdExpiry(rec);
        Date expTime = null;
        if ((expiry != null) && expiry.itsIsRecurring &&
            (expiry.itsInterval > 0)) {
            long exp = System.currentTimeMillis();
            exp += (long)expiry.itsInterval * DateUtils.DAY_IN_MILLIS;
            expTime = new Date(exp);
        }
        setField(expTime, rec, PwsRecordV3.PASSWORD_LIFETIME, false);

        // Update PasswdRecord and indexes if the record exists
        PasswdRecord passwdRec = getPasswdRecord(rec);
        if (passwdRec != null) {
            PwsRecord oldRef = passwdRec.getRef();
            if (oldRef != null) {
                PasswdRecord oldPasswdRec = getPasswdRecord(oldRef);
                oldPasswdRec.removeRefToRecord(rec);
            }
            passwdRec.passwordChanged(this);
            PwsRecord newRef = passwdRec.getRef();
            if (newRef != null) {
                PasswdRecord newPasswdRec = getPasswdRecord(newRef);
                newPasswdRec.addRefToRecord(rec);
            }
        }
    }

    /** Get the password expiration */
    public final PasswdExpiration getPasswdExpiry(PwsRecord rec)
    {
        PasswdExpiration expiry = null;
        Date expTime = getDateField(rec, PwsRecordV3.PASSWORD_LIFETIME);
        if (expTime != null) {
            Integer expInt =
                getIntField(rec, PwsRecordV3.PASSWORD_EXPIRY_INTERVAL);
            boolean haveInt = (expInt != null);
            expiry = new PasswdExpiration(expTime, haveInt ? expInt : 0,
                                          haveInt);
        }
        return expiry;
    }

    /** Set the password expiration */
    public final void setPasswdExpiry(PasswdExpiration expiry, PwsRecord rec)
    {
        Date expDate = null;
        int expInterval = 0;
        if (expiry != null) {
            expDate = expiry.itsExpiration;
            if (expiry.itsIsRecurring) {
                expInterval = expiry.itsInterval;
            }
        }
        setField(expDate, rec, PwsRecordV3.PASSWORD_LIFETIME);
        setField((expInterval != 0) ? expInterval : null, rec,
                 PwsRecordV3.PASSWORD_EXPIRY_INTERVAL);

        PasswdRecord passwdRec = getPasswdRecord(rec);
        if (passwdRec != null) {
            passwdRec.passwdExpiryChanged(this);
        }
    }

    /** Get the time the password was last modified */
    public final Date getPasswdLastModTime(PwsRecord rec)
    {
        return getDateField(rec, PwsRecordV3.PASSWORD_MOD_TIME);
    }

    /** Clear the time the password was last modified */
    public final void clearPasswdLastModTime(PwsRecord rec)
    {
        setField(null, rec, PwsRecordV3.PASSWORD_MOD_TIME);
    }

    public final PasswdHistory getPasswdHistory(PwsRecord rec)
    {
        String fieldStr = getField(rec, PwsRecordV3.PASSWORD_HISTORY);
        if (!TextUtils.isEmpty(fieldStr)) {
            try {
                return new PasswdHistory(fieldStr);
            } catch (Exception e) {
                Log.e(TAG, "Error reading password history: " + e, e);
            }
        }
        return null;
    }

    public final void setPasswdHistory(PasswdHistory history, PwsRecord rec,
                                       boolean updateModTime)
    {
        setField((history == null) ? null : history.toString(),
                 rec, PwsRecordV3.PASSWORD_HISTORY, updateModTime);
    }

    /** Get the password policy contained in a record */
    public final PasswdPolicy getPasswdPolicy(PwsRecord rec)
    {
        return PasswdPolicy.parseRecordPolicy(
            getField(rec, PwsRecordV3.PASSWORD_POLICY_NAME),
            getField(rec, PwsRecordV3.PASSWORD_POLICY),
            getField(rec, PwsRecordV3.OWN_PASSWORD_SYMBOLS));
    }

    /** Set the password policy for a record */
    public final void setPasswdPolicy(PasswdPolicy policy, PwsRecord rec)
    {
        setPasswdPolicyImpl(policy, rec, true);
    }

    /** Get the icon contained in a record */
    public final String getIcon(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.ICON);
    }

    /** Set the icon for a record */
    public final void setIcon(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.ICON);
    }

    /** Get the OTP contained in a record */
    public final String getOtp(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.OTP);
    }

    /** Set the OTP for a record */
    public final void setOtp(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.OTP);
    }

    /** Get the Autotype Delimiter contained in a record */
    public final Integer getAutotypeDelimiter(PwsRecord rec)
    {
        return getIntField(rec, PwsRecordV3.AUTOTYPE_DELIMITER);
    }

    /** Set the Autotype Delimiter for a record */
    public final void setAutotypeDelimiter(Integer str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.AUTOTYPE_DELIMITER);
    }

    /** Get the Autotype Return Suffix contained in a record */
    public final Integer getAutotypeReturnSuffix(PwsRecord rec)
    {
        return getIntField(rec, PwsRecordV3.AUTOTYPE_RETURNSUFFIX);
    }

    /** Set the Autotype Return Suffix for a record */
    public final void setAutotypeReturnSuffix(Integer str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.AUTOTYPE_RETURNSUFFIX);
    }

    public final boolean isProtected(PwsRecord rec)
    {
        boolean prot = false;
        PwsField field = doGetRecField(rec, PwsRecordV3.PROTECTED_ENTRY);
        if (field != null) {
            byte[] value = field.getBytes();
            if ((value != null) && (value.length > 0)) {
                prot = (value[0] != 0);
            }
        }
        return prot;
    }

    public final void setProtected(boolean prot, PwsRecord rec)
    {
        byte val = prot ? (byte)1 : (byte)0;
        setField(val, rec, PwsRecordV3.PROTECTED_ENTRY);
        updateFormatVersion(PwsRecordV3.DB_FMT_MINOR_3_25);
    }

    public final String getTitle(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.TITLE);
    }

    public final void setTitle(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.TITLE);
    }

    public final String getUsername(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.USERNAME);
    }

    public final void setUsername(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.USERNAME);
    }

    public final String getURL(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.URL);
    }

    public final void setURL(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.URL);
    }

    public final String getUUID(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.UUID);
    }

    public final String getHdrVersion()
    {
        return getHdrField(PwsRecordV3.HEADER_VERSION);
    }

    public final String getHdrLastSaveUser()
    {
        return getHdrField(PwsRecordV3.HEADER_LAST_SAVE_USER);
    }

    private void setHdrLastSaveUser(String user)
    {
        setHdrField(PwsRecordV3.HEADER_LAST_SAVE_USER, user);
    }

    public final String getHdrLastSaveHost()
    {
        return getHdrField(PwsRecordV3.HEADER_LAST_SAVE_HOST);
    }

    private void setHdrLastSaveHost(String host)
    {
        setHdrField(PwsRecordV3.HEADER_LAST_SAVE_HOST, host);
    }

    public final String getHdrLastSaveApp()
    {
        return getHdrField(PwsRecordV3.HEADER_LAST_SAVE_WHAT);
    }

    private void setHdrLastSaveApp(String app)
    {
        setHdrField(PwsRecordV3.HEADER_LAST_SAVE_WHAT, app);
    }

    public final String getHdrLastSaveTime()
    {
        return getHdrField(PwsRecordV3.HEADER_LAST_SAVE_TIME);
    }

    private void setHdrLastSaveTime(Date date)
    {
        setHdrField(PwsRecordV3.HEADER_LAST_SAVE_TIME, date);
    }


    /** Get the named password policies from the file header */
    public HeaderPasswdPolicies getHdrPasswdPolicies()
    {
        return itsHdrPolicies;
    }

    /**
     * Set the named password policies in the file header
     *
     * @param policies The policies; null to remove the field
     * @param policyRename If non-null the old and new names of a renamed
     *                      policy
     */
    public final void setHdrPasswdPolicies(List<PasswdPolicy> policies,
                                           Pair<String, String> policyRename)
    {
        setHdrField(PwsRecordV3.HEADER_NAMED_PASSWORD_POLICIES,
                    PasswdPolicy.hdrPoliciesToString(policies));
        updateFormatVersion(PwsRecordV3.DB_FMT_MINOR_3_28);
        if (policyRename != null) {
            // Rename policy in records as needed
            for (PasswdRecord rec: itsPasswdRecords.values()) {
                PasswdPolicy recPolicy = rec.getPasswdPolicy();
                if ((recPolicy == null) ||
                    (recPolicy.getLocation() !=
                        PasswdPolicy.Location.RECORD_NAME) ||
                    (!recPolicy.getName().equals(policyRename.first))) {
                    continue;
                }
                recPolicy = new PasswdPolicy(policyRename.second, recPolicy);
                PasswdSafeUtil.dbginfo(TAG, "Rename policy to %s for %s",
                                       recPolicy.getName(),
                                       getId(rec.getRecord()));

                setPasswdPolicyImpl(recPolicy, rec.getRecord(), false);
            }
            indexRecords();
        } else {
            indexPasswdPolicies();
        }
    }

    private static int hexBytesToInt(byte[] bytes, int pos, int len)
    {
        int i = 0;
        for (int idx = pos; idx < (pos + len); ++idx) {
            i <<= 4;
            i |= Character.digit(bytes[idx], 16);
        }
        return i;
    }

    /** Add an observer for file changes */
    public static void addObserver(PasswdFileDataObserver observer)
    {
        itsObservers.add(observer);
    }

    private void setSaveHdrFields(Context context)
    {
        setHdrLastSaveApp(PasswdSafeUtil.getAppTitle(context) +
                          " " +
                          PasswdSafeUtil.getAppVersion(context));
        setHdrLastSaveUser("User");
        setHdrLastSaveHost(Build.MODEL);
        setHdrLastSaveTime(new Date());
    }

    private void updateFormatVersion(byte minMinor)
    {
        if (isV3()) {
            PwsRecord rec = ((PwsFileV3)itsPwsFile).getHeaderRecord();
            int minor = getHdrMinorVersion(rec);
            if ((minor != -1) && (minor < minMinor)) {
                setHdrMinorVersion(rec, minMinor);
            }
        }
    }

    /** Set the password policy for a record and optionally update indexes */
    private void setPasswdPolicyImpl(PasswdPolicy policy,
                                           PwsRecord rec,
                                           boolean index)
    {
        PasswdPolicy.RecordPolicyStrs strs =
            PasswdPolicy.recordPolicyToString(policy);
        setField((strs == null) ? null : strs.itsPolicyName,
                 rec, PwsRecordV3.PASSWORD_POLICY_NAME);
        setField((strs == null) ? null : strs.itsPolicyStr,
                 rec, PwsRecordV3.PASSWORD_POLICY);
        setField((strs == null) ? null : strs.itsOwnSymbols,
                 rec, PwsRecordV3.OWN_PASSWORD_SYMBOLS);
        updateFormatVersion(PwsRecordV3.DB_FMT_MINOR_3_28);

        if (index) {
            PasswdRecord passwdRec = getPasswdRecord(rec);
            if (passwdRec != null) {
                passwdRec.passwdPolicyChanged(this);
            }
            indexPasswdPolicies();
        }
    }

    /** Get a field value as a string */
    private String getField(PwsRecord rec, int fieldId)
    {
        if (itsPwsFile == null) {
            return "";
        }

        fieldId = getVersionFieldId(fieldId);
        if (fieldId == FIELD_UNSUPPORTED) {
            return "(unsupported)";
        }
        PwsField field = doGetField(rec, fieldId);
        return (field == null) ? null : field.toString();
    }

    /** Get a field value as an 4 byte integer */
    private Integer getIntField(PwsRecord rec, int fieldId)
    {
        Integer val = null;
        PwsField field = doGetRecField(rec, fieldId);
        if ((field != null) && (field instanceof PwsIntegerField)) {
            val = (Integer)field.getValue();
        }
        return val;
    }

    /** Get a field value as a Date */
    private Date getDateField(PwsRecord rec, int fieldId)
    {
        Date date = null;
        PwsField field = doGetRecField(rec, fieldId);
        if ((field != null) && (field instanceof PwsTimeField)) {
            date = (Date)field.getValue();
        }
        return date;
    }

    private int getVersionFieldId(int fieldId)
    {
        if (itsPwsFile == null) {
            return FIELD_NOT_PRESENT;
        }

        switch (itsPwsFile.getFileVersionMajor())
        {
        case PwsFileV3.VERSION:
        {
            break;
        }
        case PwsFileV2.VERSION:
        {
            switch (fieldId)
            {
            case PwsRecordV3.GROUP:
            {
                fieldId = PwsRecordV2.GROUP;
                break;
            }
            case PwsRecordV3.NOTES:
            {
                fieldId = PwsRecordV2.NOTES;
                break;
            }
            case PwsRecordV3.PASSWORD:
            {
                fieldId = PwsRecordV2.PASSWORD;
                break;
            }
            case PwsRecordV3.PASSWORD_LIFETIME:
            {
                fieldId = PwsRecordV2.PASSWORD_LIFETIME;
                break;
            }
            case PwsRecordV3.TITLE:
            {
                fieldId = PwsRecordV2.TITLE;
                break;
            }
            case PwsRecordV3.USERNAME:
            {
                fieldId = PwsRecordV2.USERNAME;
                break;
            }
            case PwsRecordV3.UUID:
            {
                fieldId = PwsRecordV2.UUID;
                break;
            }
            case PwsRecordV3.URL:
            {
                fieldId = PwsRecordV2.URL;
                break;
            }
            case PwsRecordV3.EMAIL:
            case PwsRecordV3.PASSWORD_HISTORY:
            case PwsRecordV3.PROTECTED_ENTRY:
            case PwsRecordV3.OWN_PASSWORD_SYMBOLS:
            case PwsRecordV3.PASSWORD_POLICY_NAME:
            case PwsRecordV3.CREATION_TIME:
            case PwsRecordV3.PASSWORD_MOD_TIME:
            case PwsRecordV3.LAST_MOD_TIME:
            case PwsRecordV3.PASSWORD_EXPIRY_INTERVAL:
            {
                fieldId = FIELD_NOT_PRESENT;
                break;
            }
            }
            break;
        }
        case PwsFileV1.VERSION:
        {
            switch (fieldId)
            {
            case PwsRecordV3.NOTES:
            {
                fieldId = PwsRecordV1.NOTES;
                break;
            }
            case PwsRecordV3.PASSWORD:
            {
                fieldId = PwsRecordV1.PASSWORD;
                break;
            }
            case PwsRecordV3.TITLE:
            {
                fieldId = PwsRecordV1.TITLE;
                break;
            }
            case PwsRecordV3.USERNAME:
            {
                fieldId = PwsRecordV1.USERNAME;
                break;
            }
            case PwsRecordV3.UUID:
            {
                // No real UUID field for V1, so just use the phantom one
                fieldId = PwsRecordV1.UUID;
                break;
            }
            case PwsRecordV3.EMAIL:
            case PwsRecordV3.GROUP:
            case PwsRecordV3.PASSWORD_LIFETIME:
            case PwsRecordV3.URL:
            case PwsRecordV3.PASSWORD_HISTORY:
            case PwsRecordV3.PROTECTED_ENTRY:
            case PwsRecordV3.OWN_PASSWORD_SYMBOLS:
            case PwsRecordV3.PASSWORD_POLICY_NAME:
            case PwsRecordV3.CREATION_TIME:
            case PwsRecordV3.PASSWORD_MOD_TIME:
            case PwsRecordV3.LAST_MOD_TIME:
            case PwsRecordV3.PASSWORD_EXPIRY_INTERVAL:
            {
                fieldId = FIELD_NOT_PRESENT;
                break;
            }
            }
            break;
        }
        default:
        {
            fieldId = FIELD_UNSUPPORTED;
            break;
        }
        }

        return fieldId;
    }


    private String getHdrField(int fieldId)
    {
        if (itsPwsFile == null) {
            return "";
        }

        switch (itsPwsFile.getFileVersionMajor())
        {
        case PwsFileV3.VERSION:
        {
            break;
        }
        case PwsFileV2.VERSION:
        case PwsFileV1.VERSION:
        {
            fieldId = FIELD_NOT_PRESENT;
            break;
        }
        default:
        {
            fieldId = FIELD_UNSUPPORTED;
            break;
        }
        }

        if (isV3()) {
            PwsRecord rec = ((PwsFileV3)itsPwsFile).getHeaderRecord();
            switch (fieldId)
            {
            case PwsRecordV3.HEADER_VERSION:
            {
                return String.format(Locale.US, "%d.%02d",
                                     3, getHdrMinorVersion(rec));
            }
            case PwsRecordV3.HEADER_LAST_SAVE_TIME:
            {
                PwsField time = doGetField(rec, fieldId);
                if (time == null) {
                    return null;
                }
                byte[] bytes = time.getBytes();
                if (bytes.length == 8)
                {
                    byte[] binbytes = new byte[4];
                    Util.putIntToByteArray(
                        binbytes, hexBytesToInt(bytes, 0, bytes.length), 0);
                    bytes = binbytes;
                }
                Date d = new Date(Util.getMillisFromByteArray(bytes, 0));
                return d.toString();
            }
            case PwsRecordV3.HEADER_LAST_SAVE_USER:
            {
                PwsField field = doGetField(rec, fieldId);
                if (field != null) {
                    return doHdrFieldToString(field);
                }

                return getHdrLastSaveWhoField(rec, true);
            }
            case PwsRecordV3.HEADER_LAST_SAVE_HOST:
            {
                PwsField field = doGetField(rec, fieldId);
                if (field != null) {
                    return doHdrFieldToString(field);
                }

                return getHdrLastSaveWhoField(rec, false);
            }
            case PwsRecordV3.HEADER_LAST_SAVE_WHO:
            case PwsRecordV3.HEADER_LAST_SAVE_WHAT:
            case PwsRecordV3.HEADER_NAMED_PASSWORD_POLICIES:
            {
                PwsField field = doGetField(rec, fieldId);
                if (field != null) {
                    return doHdrFieldToString(field);
                }
                return null;
            }
            default:
            {
                return null;
            }
            }
        } else {
            return null;
        }
    }

    private void setHdrField(int fieldId, Object value)
    {
        if (itsPwsFile == null) {
            return;
        }

        if (isV3()) {
            PwsRecord rec = ((PwsFileV3)itsPwsFile).getHeaderRecord();
            switch (fieldId)
            {
            case PwsRecordV3.HEADER_LAST_SAVE_TIME:
            {
                long timeVal = ((Date)value).getTime();
                byte[] newbytes;
                int minor = getHdrMinorVersion(rec);
                if (minor >= 2) {
                    newbytes = new byte[4];
                    Util.putMillisToByteArray(newbytes, timeVal, 0);
                } else {
                    int secs = (int) (timeVal / 1000);
                    String str = String.format("%08x", secs);
                    newbytes = str.getBytes();
                }
                rec.setField(new PwsUnknownField(fieldId, newbytes));
                break;
            }
            case PwsRecordV3.HEADER_LAST_SAVE_WHAT:
            case PwsRecordV3.HEADER_NAMED_PASSWORD_POLICIES:
            {
                doSetHdrFieldString(rec, fieldId,
                                    (value == null) ? null : value.toString());
                break;
            }
            case PwsRecordV3.HEADER_LAST_SAVE_USER:
            {
                int minor = getHdrMinorVersion(rec);
                if (minor >= 2) {
                    doSetHdrFieldString(rec, PwsRecordV3.HEADER_LAST_SAVE_USER,
                                        value.toString());
                } else {
                    setHdrLastSaveWhoField(rec, value.toString(),
                                           getHdrLastSaveWhoField(rec, false));
                }
                break;
            }
            case PwsRecordV3.HEADER_LAST_SAVE_HOST:
            {
                int minor = getHdrMinorVersion(rec);
                if (minor >= 2) {
                    doSetHdrFieldString(rec, PwsRecordV3.HEADER_LAST_SAVE_HOST,
                                        value.toString());
                } else {
                    setHdrLastSaveWhoField(rec,
                                           getHdrLastSaveWhoField(rec, true),
                                           value.toString());
                }
                break;
            }
            default:
            {
                break;
            }
            }
        }
    }


    private static String doHdrFieldToString(PwsField field)
    {
        try {
            return new String(field.getBytes(), "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            return null;
        }
    }


    private static void doSetHdrFieldString(PwsRecord rec,
                                            int fieldId, String val)
    {
        try {
            PwsField field = null;
            if (val != null) {
                field = new PwsUnknownField(fieldId, val.getBytes("UTF-8"));
            }
            setOrRemoveField(field, fieldId, rec);
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Invalid encoding", e);
        }
    }

    /** Get a non-header record's field after translating its field
     * identifier */
    private PwsField doGetRecField(PwsRecord rec, int fieldId)
    {
        return doGetField(rec, getVersionFieldId(fieldId));
    }

    /** Get a field from a record */
    private static PwsField doGetField(PwsRecord rec, int fieldId)
    {
        switch (fieldId)
        {
        case FIELD_UNSUPPORTED:
        case FIELD_NOT_PRESENT:
        {
            return null;
        }
        default:
        {
            return rec.getField(fieldId);
        }
        }
    }

    private void setField(Object val, PwsRecord rec, int fieldId)
    {
        setField(val, rec, fieldId, true);
    }

    private void setField(Object val, PwsRecord rec, int fieldId,
                          boolean updateModTime)
    {
        PwsField field = null;
        switch (itsPwsFile.getFileVersionMajor())
        {
        case PwsFileV3.VERSION:
        {
            switch (fieldId)
            {
            case PwsRecordV3.EMAIL:
            case PwsRecordV3.GROUP:
            case PwsRecordV3.NOTES:
            case PwsRecordV3.TITLE:
            case PwsRecordV3.URL:
            case PwsRecordV3.ICON:
            case PwsRecordV3.OTP:
            case PwsRecordV3.USERNAME:
            case PwsRecordV3.PASSWORD_HISTORY:
            case PwsRecordV3.PASSWORD_POLICY:
            case PwsRecordV3.OWN_PASSWORD_SYMBOLS:
            case PwsRecordV3.PASSWORD_POLICY_NAME:
            {
                String str = (val == null) ? null : val.toString();
                if (!TextUtils.isEmpty(str)) {
                    field = new PwsStringUnicodeField(fieldId, str);
                }
                break;
            }
            case PwsRecordV3.PASSWORD:
            {
                String str = (val == null) ? null : val.toString();
                if (!TextUtils.isEmpty(str)) {
                    field = new PwsPasswdUnicodeField(fieldId, str, itsPwsFile);
                }
                break;
            }
            case PwsRecordV3.PROTECTED_ENTRY: {
                Byte b = (Byte)val;
                if ((b != null) && (b != 0)) {
                    field = new PwsByteField(fieldId, b);
                }
                break;
            }
            case PwsRecordV3.PASSWORD_LIFETIME: {
                Date d = (Date)val;
                if ((d != null) && (d.getTime() != 0)) {
                    field = new PwsTimeField(fieldId, d);
                }
                break;
            }
            case PwsRecordV3.AUTOTYPE_DELIMITER:
            case PwsRecordV3.AUTOTYPE_RETURNSUFFIX:
            case PwsRecordV3.PASSWORD_EXPIRY_INTERVAL: {
                Integer ival = (Integer)val;
                if ((ival != null) && (ival != 0)) {
                    field = new PwsIntegerField(fieldId, ival);
                }
                break;
            }
            default:
            {
                fieldId = FIELD_UNSUPPORTED;
                break;
            }
            }
            break;
        }
        case PwsFileV2.VERSION:
        {
            switch (fieldId)
            {
            case PwsRecordV3.GROUP:
            case PwsRecordV3.NOTES:
            case PwsRecordV3.TITLE:
            case PwsRecordV3.USERNAME:
            {
                String str = (val == null) ? null : val.toString();
                if (!TextUtils.isEmpty(str)) {
                    field = new PwsStringField(fieldId, str);
                }
                break;
            }
            case PwsRecordV3.PASSWORD:
            {
                String str = (val == null) ? null : val.toString();
                if (!TextUtils.isEmpty(str)) {
                    field = new PwsPasswdField(fieldId, str, itsPwsFile);
                }
                break;
            }
            default:
            {
                fieldId = FIELD_UNSUPPORTED;
                break;
            }
            }
            break;
        }
        default:
        {
            fieldId = FIELD_UNSUPPORTED;
            break;
        }
        }

        if (fieldId != FIELD_UNSUPPORTED) {
            setOrRemoveField(field, fieldId, rec);
            if (updateModTime && isV3() && itsPasswdRecords.containsKey(rec)) {
                int modFieldId = (fieldId == PwsRecordV3.PASSWORD) ?
                    PwsRecordV3.PASSWORD_MOD_TIME : PwsRecordV3.LAST_MOD_TIME;
                rec.setField(new PwsTimeField(modFieldId, new Date()));
            }
        }
    }

    private static void setOrRemoveField(PwsField field, int fieldId,
                                         PwsRecord rec)
    {
        if (field != null) {
            rec.setField(field);
        } else {
            rec.removeField(fieldId);
        }
    }

    private void finishOpenFile()
    {
        indexRecords();
        notifyObservers(this);
        PasswdSafeUtil.dbginfo(TAG, "file loaded");
    }

    private void indexRecords()
    {
        itsRecords.clear();
        itsRecordsByUUID.clear();
        itsPasswdRecords.clear();
        if (itsPwsFile != null) {
            itsRecords.ensureCapacity(itsPwsFile.getRecordCount());
            Iterator<PwsRecord> recIter = itsPwsFile.getRecords();
            while (recIter.hasNext()) {
                PwsRecord rec = recIter.next();
                String uuid = getUUID(rec);
                if (uuid == null) {
                    // Add a UUID field for records without one.  The record
                    // will not be marked as modified unless the user manually
                    // edits it.
                    PwsUUIDField uuidField = new PwsUUIDField(
                        isV2() ? PwsFieldTypeV2.UUID : PwsFieldTypeV3.UUID,
                        new UUID());
                    boolean modified = rec.isModified();
                    rec.setField(uuidField);
                    if (!modified) {
                        rec.resetModified();
                    }
                    uuid = uuidField.toString();
                }

                itsRecords.add(rec);
                itsRecordsByUUID.put(uuid, rec);
            }
        }
        for (PwsRecord rec: itsRecords) {
            itsPasswdRecords.put(rec, new PasswdRecord(rec, this));
        }
        for (PasswdRecord passwdRec: itsPasswdRecords.values()) {
            PwsRecord ref = passwdRec.getRef();
            PasswdRecord referencedRecord = itsPasswdRecords.get(ref);
            if (referencedRecord != null) {
                referencedRecord.addRefToRecord(passwdRec.getRecord());
            }
        }

        indexPasswdPolicies();
    }

    /** Index the password policies */
    private void indexPasswdPolicies()
    {
        List<PasswdPolicy> hdrPolicies =
            PasswdPolicy.parseHdrPolicies(
                getHdrField(PwsRecordV3.HEADER_NAMED_PASSWORD_POLICIES));
        itsHdrPolicies = new HeaderPasswdPolicies(itsPasswdRecords.values(),
                                                  hdrPolicies);
    }


    private static int getHdrMinorVersion(PwsRecord rec)
    {
        PwsField ver = doGetField(rec, PwsRecordV3.HEADER_VERSION);
        if (ver == null) {
            return -1;
        }
        byte[] bytes = ver.getBytes();
        if ((bytes == null) || (bytes.length == 0)) {
            return -1;
        }
        return bytes[0];
    }

    private static void setHdrMinorVersion(PwsRecord rec, byte minor)
    {
        PwsField ver = doGetField(rec, PwsRecordV3.HEADER_VERSION);
        if (ver == null) {
            return;
        }
        byte[] bytes = ver.getBytes();
        if ((bytes == null) || (bytes.length == 0)) {
            return;
        }

        byte[] newbytes = new byte[bytes.length];
        System.arraycopy(bytes, 0, newbytes, 0, bytes.length);
        newbytes[0] = minor;
        PwsField newVer = new PwsUnknownField(PwsRecordV3.HEADER_VERSION,
                                              newbytes);
        rec.setField(newVer);
    }

    private static String getHdrLastSaveWhoField(PwsRecord rec,
                                                 boolean isUser) {
        PwsField field = doGetField(rec, PwsRecordV3.HEADER_LAST_SAVE_WHO);
        if (field == null) {
            return null;
        }

        String str = doHdrFieldToString(field);
        if (str == null) {
            return null;
        }

        if (str.length() < 4) {
            Log.e(TAG, "Invalid who length: " + str.length());
            return null;
        }
        int len = Integer.parseInt(str.substring(0, 4), 16);

        if ((len + 4) > str.length()) {
            Log.e(TAG, "Invalid user length: " + (len + 4));
            return null;
        }

        if (isUser) {
            return str.substring(4, len + 4);
        } else {
            return str.substring(len + 4);
        }
    }


    private static void setHdrLastSaveWhoField(PwsRecord rec,
                                               String user, String host)
    {
        String who = String.format("%04x%s%s", user.length(), user, host);
        doSetHdrFieldString(rec, PwsRecordV3.HEADER_LAST_SAVE_WHO, who);
    }


    /** Notify observer of file changes */
    private static void notifyObservers(PasswdFileData fileData)
    {
        AsyncTask<PasswdFileData, Void, PasswdFileData> notifyTask =
            new AsyncTask<PasswdFileData, Void, PasswdFileData>()
            {
                @Override
                protected PasswdFileData doInBackground(PasswdFileData... params)
                {
                    return params[0];
                }

                @Override
                protected void onPostExecute(PasswdFileData fileData)
                {
                    for (PasswdFileDataObserver obs: itsObservers) {
                        obs.passwdFileDataChanged(fileData);
                    }
                }
            };
        notifyTask.execute(fileData);
    }
}
