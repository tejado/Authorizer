package net.tjado.webauthn.util;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricPrompt.CryptoObject;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executor;

public class WioBiometricPrompt {

    private static final String TAG = "WioBiometricPrompt";

    private static List<PromptCallback> mCallbacks = new ArrayList<>();
    public static final int PROMPT_TIMEOUT_MS = 30000;
    private final BiometricPrompt biometricPrompt;
    private final Handler mainHandler;
    private final Runnable timeout = this::cancelAuthentication;

    public abstract static class PromptCallback {
        private final boolean autoClear;

        public PromptCallback(boolean autoClear) {
            this.autoClear = autoClear;
        }

        public abstract void onResult(boolean result, CryptoObject cryptoObject);
    }

    public static void registerCallback(PromptCallback callback) {
        if (mCallbacks == null) {
            mCallbacks = new ArrayList<>();
        }

        if (callback == null) {
            return;
        }

        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    public static void removeCallback(PromptCallback callback) {
        if (mCallbacks == null || callback == null) {
            return;
        }

        mCallbacks.remove(callback);
    }

    public static void removeCallback(int index) {
        if (mCallbacks == null || mCallbacks.size() <= index) {
            return;
        }

        mCallbacks.remove(index);
    }

    public static void clearCallbacks() {
        mCallbacks = new ArrayList<>();
    }

    public WioBiometricPrompt(FragmentActivity fragmentActivity, String title, String subtitle,
                              boolean alternativeLoginAllowed, CryptoObject cryptoObject) {

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDeviceCredentialAllowed(alternativeLoginAllowed)
                .build();

        Executor executor = ContextCompat.getMainExecutor(fragmentActivity);

        biometricPrompt = new BiometricPrompt(fragmentActivity, executor, new BiometricPrompt.AuthenticationCallback() {
            @SuppressLint("SwitchIntDef")
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                switch (errorCode) {
                    case BiometricPrompt.ERROR_USER_CANCELED:
                        Log.e(TAG, "User cancelled biometric prompt");
                        break;
                    case BiometricPrompt.ERROR_TIMEOUT:
                    case BiometricPrompt.ERROR_CANCELED:
                        Log.e(TAG, "Timeout biometric prompt");
                        break;
                    default:
                        Log.e(TAG, "An unrecoverable error occurred " + errString.toString());
                }
                iterateOverCb(false, null);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d(TAG, "Biometrics recognised successfully");
                iterateOverCb(true, result.getCryptoObject());
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.d(TAG, "Biometrics not recognised");
                iterateOverCb(false, null);
            }
        });

        mainHandler = new Handler(fragmentActivity.getMainLooper());
        mainHandler.post(() -> {
            if (cryptoObject != null) {
                biometricPrompt.authenticate(promptInfo, cryptoObject);
            } else {
                biometricPrompt.authenticate(promptInfo);
            }
        });

        mainHandler.postDelayed(timeout, PROMPT_TIMEOUT_MS);
    }

    public WioBiometricPrompt(FragmentActivity fragmentActivity, String title, String subtitle,
                              boolean alternativeLoginAllowed) {
        this(fragmentActivity, title, subtitle, alternativeLoginAllowed, null);
    }

    public void cancelAuthentication() {
        if (mainHandler != null) mainHandler.removeCallbacks(timeout);
        biometricPrompt.cancelAuthentication();
    }

    private void iterateOverCb(boolean result, CryptoObject cryptoObject) {
        // Remove timeout if it didn't happen
        if (mainHandler != null) mainHandler.removeCallbacks(timeout);

        if (mCallbacks == null) return;

        ListIterator<PromptCallback> iter = mCallbacks.listIterator();
        while(iter.hasNext()) {
            PromptCallback callback = iter.next();
            if (callback == null) {
                iter.remove();
                continue;
            }

            try {
                callback.onResult(result, cryptoObject);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (callback.autoClear) {
                iter.remove();
            }
        }
    }
}
