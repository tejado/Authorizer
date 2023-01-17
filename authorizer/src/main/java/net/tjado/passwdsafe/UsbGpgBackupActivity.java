/*
 * Copyright (©) 2018 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.documentfile.provider.DocumentFile;

import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.tjado.passwdsafe.lib.Utils;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;


public class UsbGpgBackupActivity extends Activity
{
    private static final int REQUEST_OPEN_DOCUMENT_TREE = 201;
    private Uri defFile;
    TextView textInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PasswdSafeApp.setupTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_gpg_backup);

        SharedPreferences prefs = Preferences.getSharedPrefs(this);
        defFile = Preferences.getDefFilePref(prefs);

        textInfo = findViewById(R.id.info);

        Button btnBackup = findViewById(R.id.btn_create_backup);
        btnBackup.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_OPEN_DOCUMENT_TREE);
            }
        });

        Button btnExit = findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String keyFileName = "pubkey.asc";

        textInfo.setText("");
        if(resultCode == RESULT_OK && requestCode == REQUEST_OPEN_DOCUMENT_TREE){

            if (defFile == null) {
                textInfo.append("No default file in app preferences specified... aborting!\n");
                return;
            }
            textInfo.append(String.format("Source file: %s\n", defFile.toString()));

            Uri uriTree = data.getData();
            if (uriTree == null) {
                textInfo.append("Error during selection of target folder... aborting!\n");
                return;
            }
            textInfo.append(String.format("Target folder: %s\n", uriTree.toString()));

            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, uriTree);

            DocumentFile keyFile = null;
            textInfo.append(String.format("Searching '%s' in target folder for backup encryption...\n", keyFileName));
            for (DocumentFile file : pickedDir.listFiles()) {
                if (!file.isDirectory() && file.canRead()) {
                    if (file.getName().equals(keyFileName)) {
                        keyFile = file;

                        textInfo.append("Keyfile found!\n");
                        break;
                    }
                }
            }

            if (keyFile == null) {
                textInfo.append("Keyfile not found... aborting!\n");
                return;
            }

            DocumentFile sourceFile = DocumentFile.fromSingleUri(this, defFile);
            DocumentFile targetFile = pickedDir.createFile("application/octet-stream", getNewFilename());

            textInfo.append(String.format("Can write target file: %s\n",  targetFile.canWrite()));

            try {
                InputStream keyFileInputStream = getContentResolver().openInputStream(keyFile.getUri());
                InputStream inputStream = getContentResolver().openInputStream(sourceFile.getUri());
                OutputStream outputStream = getContentResolver().openOutputStream(targetFile.getUri());

                textInfo.append("Loading public key...\n");
                PGPPublicKey pubKey = readPublicKeyFromCol(keyFileInputStream);

                String pubKeyId = Long.toHexString(pubKey.getKeyID()).toUpperCase();
                textInfo.append(String.format("Public key '%s' found...\n", pubKeyId));

                // check if the found public key is the same as the last backup
                SharedPreferences prefs = Preferences.getSharedPrefs(this);
                String lastPubKeyId = Preferences.getFileBackupUsbGpgKey(prefs);
                if (pubKeyId.equals(lastPubKeyId)) {
                    textInfo.append("Same public key as last backup!\n");
                } else {
                    if (lastPubKeyId.equals("")) {
                        lastPubKeyId = "not set";
                    }
                    textInfo.append(String.format("WARNING! New public key found! (Previous key was %s)\n", lastPubKeyId));
                    Preferences.setFileBackupUsbGpgKey(pubKeyId, prefs);
                }

                textInfo.append("Encrypt & write backup file...\n");
                encryptFile(outputStream, defFile, pubKey);
                Utils.closeStreams(inputStream, outputStream);

                textInfo.append("Backup finished!\n");
            } catch (Exception e) {
                textInfo.append(String.format("Exception: %s\n", e.getLocalizedMessage()));
                e.printStackTrace();
            }
        }
    }

    private String getNewFilename() {
        Date currentTime = Calendar.getInstance().getTime();
        String date = Utils.formatDateUriSafe(currentTime);

        return String.format("backup_%s.gpg", date);
    }


    /*
     * code adopted (and slightly modified) from https://github.com/keith0591/pgp-encryption/
     * Thanks to  Malkeith Singh <https://github.com/keith0591/>
     */
    @SuppressLint("NewApi")
    public void encryptFile(OutputStream encryptedOut, Uri inputFileUri, PGPPublicKey publicKey) throws IOException, PGPException {
        Security.addProvider(new BouncyCastleProvider());
        int bufferSize = 1 << 16;

        ContentResolver cr = this.getContentResolver();
        InputStream plaintextInputStream = cr.openInputStream(inputFileUri);


        AssetFileDescriptor fileDescriptor = getApplicationContext().getContentResolver().openAssetFileDescriptor(inputFileUri , "r");
        long fileSize = fileDescriptor.getLength();

        textInfo.append(String.format("File size: %d!\n", fileSize));

        PGPCompressedDataGenerator dataCompressor = new PGPCompressedDataGenerator(PGPCompressedData.ZLIB);

        PGPEncryptedDataGenerator dataEncryptor = new PGPEncryptedDataGenerator(new BcPGPDataEncryptorBuilder(
                SymmetricKeyAlgorithmTags.AES_256).setSecureRandom(new SecureRandom()).setWithIntegrityPacket(true));
        dataEncryptor.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(publicKey));

        OutputStream cOut = dataEncryptor.open(encryptedOut, new byte[bufferSize]);
        copyAsLiteralData(dataCompressor.open(cOut), plaintextInputStream, fileSize, bufferSize);
        dataCompressor.close();
        cOut.close();
        encryptedOut.close();
    }

    /**
     * Copies "length" amount of data from the input stream and writes it pgp literal data to the provided output stream
     *
     * @param outputStream the output stream to which data is to be written
     * @param in           the input stream from which data is to be read
     * @param length       the length of data to be read
     * @param bufferSize   the buffer size, as it uses buffer to speed up copying
     * @throws IOException for IO related errors
     */
    static void copyAsLiteralData(OutputStream outputStream, InputStream in, long length, int bufferSize) throws IOException {
        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
        OutputStream pOut = lData.open(outputStream, PGPLiteralData.BINARY, PGPLiteralData.CONSOLE, new Date(), new byte[bufferSize]);
        byte[] buff = new byte[bufferSize];
        try {
            int len;
            long totalBytesWritten = 0L;
            while (totalBytesWritten <= length && (len = in.read(buff)) > 0) {
                pOut.write(buff, 0, len);
                totalBytesWritten += len;
            }
            pOut.close();
        } finally {
            // Clearing buffer
            Arrays.fill(buff, (byte) 0);
            // Closing inputstream
            in.close();
        }
    }

    /*
     * code adopted (and slightly modified) from https://stackoverflow.com/a/33216302
     * Thanks to sheckoo90 <https://stackoverflow.com/users/893019/sheckoo90>
     */
    @SuppressWarnings("rawtypes")
    public static PGPPublicKey readPublicKeyFromCol(InputStream in) throws IOException, PGPException {
        in = PGPUtil.getDecoderStream(in);
        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in, new BcKeyFingerprintCalculator());
        PGPPublicKey key = null;
        Iterator rIt = pgpPub.getKeyRings();
        while (key == null && rIt.hasNext()) {
            PGPPublicKeyRing kRing = (PGPPublicKeyRing) rIt.next();
            Iterator kIt = kRing.getPublicKeys();
            while (key == null && kIt.hasNext()) {
                PGPPublicKey k = (PGPPublicKey) kIt.next();
                if (k.isEncryptionKey() && !k.isMasterKey()) {
                    key = k;
                }
            }
        }
        if (key == null) {
            throw new IllegalArgumentException("Can't find encryption key in key ring.");
        }
        return key;
    }
}
