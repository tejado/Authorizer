package net.tjado.passwdsafe;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import net.tjado.bluetooth.BluetoothDeviceListing;
import net.tjado.bluetooth.BluetoothUtils;
import net.tjado.bluetooth.HidDeviceController;
import net.tjado.passwdsafe.lib.ActContext;
import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.bluetooth.BluetoothDeviceWrapper;

import static android.app.Notification.DEFAULT_SOUND;
import static android.app.Notification.DEFAULT_VIBRATE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;


@SuppressLint({"MissingPermission", "NewApi"})
public class BluetoothForegroundService extends Service {

    private static final String TAG = "BluetoothFgrndService";
    private static final String CHANNEL_ID = "BluetoothServiceChannel";
    private static final int MAIN_NOTIFICATION_ID = 1;
    private static final int REQUEST_NOTIFICATION_ID = 2;
    private static boolean openFileStarted = false;
    private static final Object mLock = new Object();
    private static boolean isStarted = false;
    private boolean isInitPhase = false;

    private boolean isBtProfileAlreadyRegistered = false;

    private final IBinder binder = new BluetoothForegroundBinder();

    private HidDeviceController hidDeviceController;
    private BluetoothDeviceListing bluetoothDeviceListing;
    private BluetoothDevice pairingDevice = null;
    private BtServiceProfileListener profileListener = null;

    private byte[] keyboardOutput = null;

    final private Handler openFileResetHandler = new Handler();
    final private Handler initAppRegistrationHandler = new Handler();

    final private int OPEN_FILE_TIMEOUT_MS = 20 * 1000;
    final private int INIT_APP_REGISTRATION_TIMEOUT_MS = 2 * 1000;

    private NotificationManagerCompat notificationManager = null;
    private NotificationCompat.Builder serviceNotificationBuilder = null;

    SharedPreferences prefs = null;


    private final BroadcastReceiver btStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                checkBluetoothState(state);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(isStarted) {
            PasswdSafeUtil.dbginfo(TAG,"Skipping onStartCommand - Already started ... ");
            return START_STICKY;
        }
        isStarted = true;

        prefs = Preferences.getSharedPrefs(getApplicationContext());

        // A service needs to manage its own lifecycle - if Bluetooth gets deactivated the service
        // needs to terminate itself. It can't be done by the activity as it might be not running.
        IntentFilter btStatusIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btStatusBroadcastReceiver, btStatusIntentFilter);

        PasswdSafeUtil.dbginfo(TAG,"Executing onStartCommand - " + intent);
        createNotificationChannel();

        showBroadcastNotification();
        setHid();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForegroundService();
    }

    public void stopForegroundService() {
        PasswdSafeUtil.dbginfo(TAG, "Stop foreground service.");

        endBroadcast();

        try {
            unregisterReceiver(btStatusBroadcastReceiver);
        } catch (Exception ignored) {}


        isStarted = false;
        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    final class BluetoothForegroundBinder extends Binder {
        BluetoothForegroundService getService() {
            return BluetoothForegroundService.this;
        }
    }

    private void showBroadcastNotification() {
        Intent notificationIntent = new Intent(this, PasswdSafe.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), notificationIntent, FLAG_UPDATE_CURRENT + ApiCompat.getPendingIntentImmutableFlag());
        //NotificationCompat.Action action = new NotificationCompat.Action(R.drawable.ic_action_lock, "START/STOP", pendingIntent);

        serviceNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_body_init))
                .setSmallIcon(R.drawable.selector_menu_policies)
                .setContentIntent(pendingIntent)
                //.addAction(action)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOngoing(true);

        startForeground(MAIN_NOTIFICATION_ID, serviceNotificationBuilder.build());
    }

    private void endBroadcast() {
        if(hidDeviceController != null && profileListener != null) {
            hidDeviceController.unregister(profileListener);
            profileListener = null;
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    private void setHid() {
        isInitPhase = true;

        profileListener = new BtServiceProfileListener();
        hidDeviceController = HidDeviceController.getInstance();

        if(Preferences.getBluetoothFidoEnabled(prefs)) {
            hidDeviceController.registerFido(getApplicationContext(), profileListener);
        } else {
            hidDeviceController.registerKeyboard(getApplicationContext(), profileListener);
        }

        bluetoothDeviceListing = new BluetoothDeviceListing(getApplicationContext());

        initAppRegistrationHandler.postDelayed(() -> {
            if(isInitPhase) {
                isInitPhase = false;
                isBtProfileAlreadyRegistered = true;
                updateServiceNotificationContent(getString(R.string.notification_body_bt_unclean));
            }
        }, INIT_APP_REGISTRATION_TIMEOUT_MS);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Authorizer Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        serviceChannel.setShowBadge(true);
        serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        notificationManager = NotificationManagerCompat.from(this);
        notificationManager.createNotificationChannel(serviceChannel);
    }

    private void updateServiceNotificationContent(String text) {
        if(notificationManager != null && text != null && serviceNotificationBuilder != null) {
            serviceNotificationBuilder.setContentText(text);
            notificationManager.notify(MAIN_NOTIFICATION_ID, serviceNotificationBuilder.build());
        }
    }

    private void showRequestNotification(){
        Intent intent = new Intent(this, PasswdSafe.class);
        intent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, intent, FLAG_UPDATE_CURRENT + ApiCompat.getPendingIntentImmutableFlag());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.selector_menu_policies)
                .setContentTitle(getString(R.string.notification_title_actionrequired))
                .setContentText(getString(R.string.notification_body_actionrequired))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setDefaults(DEFAULT_SOUND | DEFAULT_VIBRATE)
                .setAutoCancel(true)
                .setTimeoutAfter(10000)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        notificationManager.notify(REQUEST_NOTIFICATION_ID, builder.build());
    }


    public void pairAsKeyboard(BluetoothDevice device) {
        PasswdSafeUtil.dbginfo(TAG, "Start Keyboard pairing");
        pairingDevice = device;

        hidDeviceController.disconnect();
        requireKeyboardMode();

        // requestConnect will only save the device for the connect after the bt app profile
        // was successfully registered.
        hidDeviceController.requestConnect(device);

        PasswdSafeUtil.dbginfo(TAG, "pref update: " + bluetoothDeviceListing.cacheHidDeviceAsKeyboard(device));
    }

    public void pairAsFido(BluetoothDevice device) {

        if(!Preferences.getBluetoothFidoEnabled(prefs)) {
            PasswdSafeUtil.dbginfo(TAG, "FIDO-mode is deactivated... abort pairing");
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "Start FIDO pairing");
        pairingDevice = device;

        hidDeviceController.disconnect();
        // As the standard connect does not work reliable to switch between FIDO devices, this
        // pairing method is also used for connecting to already paired devices. Due to that
        // we enforce the reinitialization of the FIDO Bluetooth profile.
        requireFidoMode(true);

        // requestConnect will only save the device for the connect after the bt app profile
        // was successfully registered.
        hidDeviceController.requestConnect(device);

        PasswdSafeUtil.dbginfo(TAG, "pref update: " + bluetoothDeviceListing.cacheHidDeviceAsFido(device));
    }

    public void requireKeyboardMode() {
        if(!hidDeviceController.isHidKeyboardMode()) {
            hidDeviceController.unregister(profileListener);
            SystemClock.sleep(50);
            hidDeviceController.registerKeyboard(getApplicationContext(), profileListener);
        }
    }

    public void requireFidoMode() {
        requireFidoMode(false);
    }

    public void requireFidoMode(boolean enforce) {
        if((!hidDeviceController.isHidFidoMode() || enforce) && Preferences.getBluetoothFidoEnabled(prefs)) {
            hidDeviceController.unregister(profileListener);
            SystemClock.sleep(50);
            hidDeviceController.registerFido(getApplicationContext(), profileListener);
        }
    }

    public void connectAndType(BluetoothDevice device, byte[] autotypeString) {
        PasswdSafeUtil.dbginfo(TAG, "Connect And Type");
        requireKeyboardMode();

        keyboardOutput = autotypeString;
        hidDeviceController.requestConnect(device);
    }

    public BluetoothDevice getConnectedDevice() {
        return hidDeviceController.getConnectedDevice();
    }

    public boolean isAppRegistered() {
        return !isBtProfileAlreadyRegistered && hidDeviceController.getRegisterAppStatus();
    }

    private void checkBluetoothState(Integer state) {
        if (state == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) getApplication().getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter btAdapter = bluetoothManager.getAdapter();
            if (btAdapter == null) {
                return;
            }

            state = btAdapter.getState();
        }

        if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
            PasswdSafeUtil.dbginfo(TAG, "BluetoothAdapter.STATE_OFF // STATE_TURNING_OFF");
            stopForegroundService();
        }
    }


    private final class BtServiceProfileListener implements HidDeviceController.ProfileListener {
        @Override
        public void onAppStatusChanged(boolean registered) {
            /*
             * If we are registered, we have an active controller and a default device
             * we proceed in try to connect with the default HID host. Since we do not
             * know if the device is in range and this may fail, we need to handle it.
             */
            PasswdSafeUtil.dbginfo(TAG, "onAppStatusChanged - registered: " + registered);

            if(isInitPhase && registered) {
                isInitPhase = false;
                isBtProfileAlreadyRegistered = false;
                updateServiceNotificationContent(getString(R.string.notification_body_foregroundservice));
            }

            // Connect to default device in case of app was registered and no pairing is in progress
            if (registered && hidDeviceController != null && pairingDevice == null) {
                BluetoothDeviceWrapper defaultDevice = bluetoothDeviceListing.getHidDefaultDevice();

                if (
                    defaultDevice != null &&
                    (
                            (hidDeviceController.isHidFidoMode() && bluetoothDeviceListing.isFidoHost(defaultDevice)) ||
                            (hidDeviceController.isHidKeyboardMode() && bluetoothDeviceListing.isKeyboardHost(defaultDevice))
                    )
                ){
                    PasswdSafeUtil.dbginfo(TAG, "onAppStatusChanged - requestConnect to default device");
                    hidDeviceController.requestConnect(defaultDevice.getDevice());
                }
            }

            if (registered && pairingDevice != null) {
                pairingDevice = null;
            }

            if (!registered && hidDeviceController != null && profileListener != null) {
                PasswdSafeUtil.dbginfo(TAG, "onAppStatusChanged - unregister profileListener");
                hidDeviceController.unregister(profileListener);
            }
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            synchronized (mLock) {
                if (state == BluetoothProfile.STATE_CONNECTED && device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    PasswdSafeUtil.dbginfo(TAG, "onConnectionStateChanged: CONNECTED: " + BluetoothUtils.getDeviceDisplayName(device));

                    if (
                        hidDeviceController.isHidKeyboardMode() &&
                        pairingDevice == null &&
                        bluetoothDeviceListing.isKeyboardHost(device) &&
                        keyboardOutput != null
                    ){
                        PasswdSafeUtil.dbginfo(TAG, "onConnectionStateChanged: initiate HID Keyboard Autotype");
                        SystemClock.sleep(100);
                        hidDeviceController.sendToKeyboardHost(keyboardOutput);
                        SystemClock.sleep(500);
                        requireFidoMode();

                    } else if(pairingDevice != null && pairingDevice.equals(device)) {
                        // pairing seems to be successful
                        pairingDevice = null;
                        requireFidoMode();
                    }
                }
            }
        }

        @Override
        public void onInterruptData(BluetoothDevice device, int reportId, byte[] data, BluetoothHidDevice inputHost) {

            if(!hidDeviceController.isHidFidoMode()) {
                PasswdSafeUtil.dbginfo(TAG, "onInterruptData - received data in non-FIDO mode... aborting");
                return;
            }

            if(!Preferences.getBluetoothFidoEnabled(prefs)) {
                PasswdSafeUtil.dbginfo(TAG, "onInterruptData - received data with FIDO disabled... aborting");
            }

            PasswdSafe activity = ((PasswdSafeApp) getApplication()).getActiveActivity();
            if (PasswdSafe.mTransactionManager != null && activity != null && activity.isFileOpen()) {
                openFileStarted = false;

                PasswdSafe.mTransactionManager.handleReport(data, (rawReports) -> {
                    for (byte[] report : rawReports) {
                        inputHost.sendReport(device, reportId, report);
                    }
                });

            } else {
                if(activity != null && !openFileStarted) {
                    PasswdSafeUtil.dbginfo(TAG, "App is open - notify user inside app");

                    // setting flag that file opening getting triggered on multiple interrupts of
                    // one authentication request
                    openFileStarted = true;
                    // reset the variable in case of subsequent authentication requests
                    openFileResetHandler.postDelayed(() -> openFileStarted = false, OPEN_FILE_TIMEOUT_MS);

                    if (!activity.openDefaultFile()) {
                        PasswdSafeUtil.showErrorMsg("Incoming FIDO request - please open respective PasswdSafe file!", new ActContext(activity));
                    }
                }

                PasswdSafeUtil.dbginfo(TAG, "Notification sent to user!");
                showRequestNotification();
            }
        }

        @Override
        public void onServiceStateChanged(BluetoothProfile proxy) {}
    }
}