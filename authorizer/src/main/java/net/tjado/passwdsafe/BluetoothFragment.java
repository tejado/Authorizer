package net.tjado.passwdsafe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.tjado.bluetooth.BluetoothDeviceListing;
import net.tjado.bluetooth.BluetoothUtils;
import net.tjado.bluetooth.HidDeviceProfile;
import net.tjado.passwdsafe.lib.ActContext;
import net.tjado.passwdsafe.lib.DynamicPermissionMgr;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.bluetooth.BluetoothDeviceWrapper;
import net.tjado.passwdsafe.util.AboutUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Fragment for Bluetooth Device Listing & Keyboard/FIDO pairing
 */

@SuppressLint("MissingPermission")
public class BluetoothFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /** Update the view */
        void updateViewBluetooth();

        void checkBluetoothState();
    }

    private Listener itsListener;

    private SharedPreferences prefs;

    private ViewFlipper flipperSub;
    private ProgressBar btScanProgress;
    private Button btScanToggle;
    private TextView tvNoPairedDevices;
    private TextView tvScanDescription;
    private Button btRequestProgress;
    private Button btAppSettings;

    private BluetoothAdapter bluetoothAdapter;
    BluetoothDeviceListing bluetoothDeviceListing = null;

    private RecyclerView rvPairedDevices = null;
    private RvPairedDevicesAdapter rvPairedDevicesAdapter;
    private List<BluetoothDeviceWrapper> pairedDevices;

    RecyclerView rvDiscoveredDevices = null;
    private RvDiscoveredDevicesAdapter rvDiscoveredDevicesAdapter;
    private final ArrayList<BluetoothDeviceWrapper> discoveredDevices = new ArrayList<>();

    private final Handler checkBtProfileStateHandler = new Handler();

    private final int CHECK_APP_REGISTRATION_TIMEOUT_MS = 200;

    private static final String TAG = "BluetoothFragment";

    DynamicPermissionMgr itsPermissionMgr = null;

    /**
     * Create a new instance
     */
    public static BluetoothFragment newInstance()
    {
        return new BluetoothFragment();
    }

    @Override
    public void onAttach(@NonNull Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    public boolean androidSupportsBluetoothHid() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        ViewFlipper flipperOverall = rootView.findViewById(R.id.flipperOverall);
        flipperSub = rootView.findViewById(R.id.flipperSub);
        tvScanDescription = rootView.findViewById(R.id.scan_description);
        tvNoPairedDevices = rootView.findViewById(R.id.tv_no_paired_devices);
        btRequestProgress = rootView.findViewById(R.id.request_permissions);
        btAppSettings = rootView.findViewById(R.id.app_settings);

        btScanProgress = rootView.findViewById(R.id.scan_progress);
        btScanToggle = rootView.findViewById(R.id.btn_scan);

        rvPairedDevices = rootView.findViewById(R.id.rv_bluetooth_devices_paired);
        rvDiscoveredDevices = rootView.findViewById(R.id.rv_bluetooth_devices_scan);

        TextView tvBluetoothFeature = rootView.findViewById(R.id.tv_pref_bt);
        TextView tvBluetoothFido = rootView.findViewById(R.id.tv_pref_bt_fido);
        CheckBox cbBluetoothFeature = rootView.findViewById(R.id.cb_pref_bt);
        CheckBox cbBluetoothFido = rootView.findViewById(R.id.cb_pref_bt_fido);

        prefs = Preferences.getSharedPrefs(getContext());

        BluetoothForegroundService btService = ((PasswdSafe) requireActivity()).btService;

        cbBluetoothFeature.setChecked(Preferences.getBluetoothEnabled(prefs));
        if(Preferences.getBluetoothEnabled(prefs)) {
            cbBluetoothFido.setEnabled(true);
            cbBluetoothFido.setChecked(Preferences.getBluetoothFidoEnabled(prefs));
        } else {
            cbBluetoothFido.setChecked(false);
            cbBluetoothFido.setEnabled(false);
        }

        tvBluetoothFeature.setOnClickListener(item -> cbBluetoothFeature.performClick());
        tvBluetoothFido.setOnClickListener(item -> cbBluetoothFido.performClick());

        cbBluetoothFeature.setOnClickListener(item -> {
            Preferences.setBluetoothEnabledPref(cbBluetoothFeature.isChecked(), prefs);

            if(Preferences.getBluetoothEnabled(prefs)) {
                cbBluetoothFido.setEnabled(true);
                cbBluetoothFido.setChecked(Preferences.getBluetoothFidoEnabled(prefs));

                itsListener.checkBluetoothState();
            } else {
                cbBluetoothFido.setChecked(false);
                cbBluetoothFido.setEnabled(false);

                if(btService != null) {
                    btService.stopForegroundService();
                }
            }

            checkBluetoothState(null);
        });

        cbBluetoothFido.setOnClickListener(item -> {
            Preferences.setBluetoothFidoEnabledPref(cbBluetoothFido.isChecked(), prefs);

            if(cbBluetoothFido.isChecked()) {
                rvDiscoveredDevicesAdapter.notifyDataSetChanged();
            }

            if (btService != null) {
                if(cbBluetoothFido.isChecked()) {
                    btService.requireFidoMode();
                } else {
                    btService.requireKeyboardMode();
                }
            }
        });


        btScanToggle.setOnClickListener(item -> bluetoothScanToggle());

        if (!androidSupportsBluetoothHid()) {
            PasswdSafeUtil.dbginfo(TAG, "Android API version too low - aborting");
            flipperOverall.setDisplayedChild(1);
            return rootView;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();


        if (bluetoothAdapter == null) {
            PasswdSafeUtil.dbginfo(TAG, "BluetoothAdapter is null - aborting");
            flipperOverall.setDisplayedChild(2);
            return rootView;
        }

        itsPermissionMgr = new DynamicPermissionMgr(
                requireActivity(), rootView, 3, 4,
                BuildConfig.APPLICATION_ID, R.id.request_permissions, R.id.app_settings, requestPermissionLauncher);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            itsPermissionMgr.addPerm(Manifest.permission.BLUETOOTH_SCAN, true);
            itsPermissionMgr.addPerm(Manifest.permission.BLUETOOTH_CONNECT, true);
        } else {
            itsPermissionMgr.addPerm(Manifest.permission.BLUETOOTH, true);
            itsPermissionMgr.addPerm(Manifest.permission.BLUETOOTH_ADMIN, true);
            itsPermissionMgr.addPerm(Manifest.permission.ACCESS_COARSE_LOCATION, true);
            itsPermissionMgr.addPerm(Manifest.permission.ACCESS_FINE_LOCATION, true);
        }

        if (!itsPermissionMgr.checkPerms()) {
            PasswdSafeUtil.dbginfo(TAG, "Missing specific permissions - scan disabled");
            btScanToggle.setEnabled(false);

            tvScanDescription.setText(R.string.bt_no_scan_permission_desc);
            btRequestProgress.setVisibility(View.VISIBLE);
            btAppSettings.setVisibility(View.VISIBLE);
        } else {
            initRecyclerViews();
        }

        if (AboutUtils.checkShowBluetoothHelp(requireContext())) {
            showBluetoothHelp();
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu,
                                    @NonNull MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_bluetooth, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_bluetooth_help) {
            showBluetoothHelp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showBluetoothHelp() {
        PasswdSafeUtil.showInfoMsg(getString(R.string.bt_help), requireContext());
    }

    private void initRecyclerViews() {
        if (bluetoothDeviceListing != null) {
            return;
        }

        bluetoothDeviceListing = new BluetoothDeviceListing(requireContext());

        // paired devices
        pairedDevices = bluetoothDeviceListing.getAvailableDevices();
        rvPairedDevicesAdapter = new RvPairedDevicesAdapter(pairedDevices);
        rvPairedDevices.setAdapter(rvPairedDevicesAdapter);
        LinearLayoutManager llmPairedDevices = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        rvPairedDevices.setLayoutManager(llmPairedDevices);

        tvNoPairedDevices.setVisibility(pairedDevices.isEmpty() ? View.VISIBLE : View.GONE);

        // discovered devices during Bluetooth scan
        rvDiscoveredDevicesAdapter = new RvDiscoveredDevicesAdapter(discoveredDevices);
        rvDiscoveredDevices.setAdapter(rvDiscoveredDevicesAdapter);
        LinearLayoutManager llmDiscoveredDevices = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        rvDiscoveredDevices.setLayoutManager(llmDiscoveredDevices);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewBluetooth();

        if(!androidSupportsBluetoothHid() || !itsPermissionMgr.hasRequiredPerms()) {
            return;
        }

        final IntentFilter btStatusIntentFilter = new IntentFilter();
        btStatusIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        btStatusIntentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        requireContext().registerReceiver(btStateReceiver, btStatusIntentFilter);

        registerScanReceiver();

        checkBluetoothState(null);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (itsPermissionMgr != null && itsPermissionMgr.hasRequiredPerms()) {
            stopBluetoothDiscovery();

            try {
                unregisterScanReceiver();
            } catch (Exception e) {
                PasswdSafeUtil.dbginfo(TAG, e, "stopBluetoothDiscovery");
            }
        }
    }


    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    private void bluetoothScanToggle() {
        if (bluetoothAdapter.isDiscovering()) {
            stopBluetoothDiscovery();
        } else {
            clearAvailableDevices();
            startBluetoothDiscovery();
        }
    }

    private void startBluetoothDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    private void stopBluetoothDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        discoveryStopped();
    }

    private void registerScanReceiver() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        requireContext().registerReceiver(btScanReceiver, intentFilter);
    }

    private void unregisterScanReceiver() {
        requireContext().unregisterReceiver(btScanReceiver);
    }

    private void discoveryStarted() {
        btScanToggle.setText(R.string.bt_stop_scan);
        btScanProgress.setVisibility(View.VISIBLE);
    }

    private void discoveryStopped() {
        btScanToggle.setText(R.string.bt_start_scan);
        btScanProgress.setVisibility(View.GONE);
    }

    private void checkBluetoothState(Integer state) {

        if (state == null) {
            state = bluetoothAdapter.getState();
        }

        if (state == BluetoothAdapter.STATE_OFF) {
            PasswdSafeUtil.dbginfo(TAG, "BluetoothAdapter.STATE_OFF");
            if(flipperSub != null) {
                flipperSub.setDisplayedChild(2);
            }

        } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
            PasswdSafeUtil.dbginfo(TAG, "BluetoothAdapter.STATE_TURNING_OFF");
            if(flipperSub != null) {
                flipperSub.setDisplayedChild(2);
            }

        } else if (state == BluetoothAdapter.STATE_ON) {
            PasswdSafeUtil.dbginfo(TAG, "BluetoothAdapter.STATE_ON");
            if(flipperSub != null) {
                if(Preferences.getBluetoothEnabled(prefs)) {
                    flipperSub.setDisplayedChild(0);
                } else {
                    flipperSub.setDisplayedChild(1);
                }

                if (pairedDevices != null && rvPairedDevicesAdapter != null) {
                    pairedDevices.clear();
                    pairedDevices.addAll(bluetoothDeviceListing.getAvailableDevices());

                    tvNoPairedDevices.setVisibility(pairedDevices.isEmpty() ? View.VISIBLE : View.GONE);

                    rvPairedDevicesAdapter.notifyDataSetChanged();
                }
            }

        } else if (state == BluetoothAdapter.STATE_TURNING_ON) {
            PasswdSafeUtil.dbginfo(TAG, "BluetoothAdapter.STATE_TURNING_ON");
        }
    }

    private void checkDeviceState(Integer state) {
        PasswdSafeUtil.dbginfo(TAG, "checkDeviceState: " + state);
        if (state == BluetoothAdapter.STATE_CONNECTED) {
            PasswdSafeUtil.dbginfo(TAG, "BluetoothAdapter.STATE_CONNECTED");
            rvPairedDevicesAdapter.notifyDataSetChanged();
        }
    }

    @SuppressLint({"MissingPermission", "NewApi"})
    protected void addAvailableDevice(BluetoothDevice device) {
        BluetoothDeviceWrapper wrappedDevice = new BluetoothDeviceWrapper(device);
        if(!discoveredDevices.contains(wrappedDevice) && device.getName() != null) {
            discoveredDevices.add(wrappedDevice);
            rvDiscoveredDevicesAdapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("NewApi")
    protected void updateAvailableDevice(BluetoothDevice device) {
        Optional<BluetoothDeviceWrapper> optDevice = discoveredDevices.stream().filter(d -> d.getDevice().getAddress().equals(device.getAddress())).findFirst();
        if(optDevice.isPresent() && device.getName() != null) {
            discoveredDevices.remove(optDevice.get());
            discoveredDevices.add(new BluetoothDeviceWrapper(device));
            rvDiscoveredDevicesAdapter.notifyDataSetChanged();
        }
    }

    protected void clearAvailableDevices() {
        discoveredDevices.clear();

        if (rvDiscoveredDevicesAdapter != null) {
            rvDiscoveredDevicesAdapter.notifyDataSetChanged();
        }
    }

    public class RvPairedDevicesAdapter extends RecyclerView.Adapter<ViewHolderPairedDevice> {
        private final List<BluetoothDeviceWrapper> devices;
        private final BluetoothForegroundService btService;

        public RvPairedDevicesAdapter(List<BluetoothDeviceWrapper> devices) {
            this.devices = devices;
            this.btService = ((PasswdSafe) requireActivity()).btService;
        }

        @NonNull
        @Override
        public ViewHolderPairedDevice onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_bluetooth_devices, parent, false);
            return new ViewHolderPairedDevice(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolderPairedDevice holder, int position) {
            BluetoothDeviceWrapper device = devices.get(position);

            String displayType;
            String type = device.getType();
            holder.btnReconnect.setVisibility(View.GONE);
            if (type.equals(BluetoothDeviceListing.HID_KEYBOARD_HOST)) {
                displayType = "<font color=#008000><b>Keyboard</b></font>";
            } else if (type.equals(BluetoothDeviceListing.HID_FIDO_HOST)) {
                displayType = "<font color=#008000><b>FIDO (U2F/WebAuthn)</b></font>";
                holder.btnReconnect.setVisibility(View.VISIBLE);
            } else {
                displayType = "<font color=#ff0000><b>Unknown</b></font>";
            }

            holder.name.setText(device.getName());
            holder.address.setText(device.getAddress());
            holder.state.setText(Html.fromHtml(String.format(getString(R.string.bt_paired_as), displayType)));

            holder.btnDeviceMenu.setOnClickListener(view -> showDeviceMenu(view, device));

            holder.btnReconnect.setEnabled(true);
            holder.btnReconnect.setText(R.string.bt_reconnect);
            holder.btnReconnect.setTextAppearance(requireContext(), R.style.Widget_AppCompat_Button_Colored);

            if(btService != null && btService.getConnectedDevice() != null)  {
                BluetoothDeviceWrapper connectedDevice = new BluetoothDeviceWrapper(btService.getConnectedDevice());
                if(device.equals(connectedDevice)) {
                    holder.btnReconnect.setEnabled(false);
                    holder.btnReconnect.setText(R.string.bt_connected);
                    holder.btnReconnect.setTextAppearance(requireContext(), R.style.Widget_AppCompat_Button);
                }
            }

            // Reconnect in case of several paired FIDO devices
            holder.btnReconnect.setOnClickListener(item -> {
                if(btService != null) {
                    if(btService.getConnectedDevice() != null)  {
                        BluetoothDeviceWrapper connectedDevice = new BluetoothDeviceWrapper(btService.getConnectedDevice());
                        if(device.equals(connectedDevice)) {
                            holder.btnReconnect.setEnabled(false);
                            holder.btnReconnect.setText(R.string.bt_connected);
                            holder.btnReconnect.setTextAppearance(requireContext(), R.style.Widget_AppCompat_Button);

                            Toast.makeText(getActivity(), "Already connected!", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    stopBluetoothDiscovery();
                    SystemClock.sleep(50);

                    // As the standard connect does not work to switch between FIDO devices,
                    // the Bluetooth HID profile gets reinitialized. As device is already paired
                    // the pairing dialog is not shown and the device gets connected.
                    btService.pairAsFido(device.getDevice());

                    checkBtProfileStateHandler.postDelayed(() -> {
                        if(btService.isAppRegistered()) {
                            PasswdSafeUtil.dbginfo(TAG, "btService.isAppRegistered is TRUE");
                        } else {
                            PasswdSafeUtil.dbginfo(TAG, "btService.isAppRegistered is FALSE");
                            PasswdSafeUtil.showErrorMsg(getString(R.string.bt_unclean_state_error), new ActContext(requireContext()));
                        }
                    }, CHECK_APP_REGISTRATION_TIMEOUT_MS);
                } else {
                    PasswdSafeUtil.showErrorMsg(getString(R.string.bt_pairing_no_service), new ActContext(requireActivity()));
                }
            });
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        private void showDeviceMenu(View view, BluetoothDeviceWrapper device) {
            String type = device.getType();

            PopupMenu popup = new PopupMenu(requireContext(), view);
            popup.getMenuInflater().inflate(R.menu.cardview_bluetooth_device, popup.getMenu());

            MenuItem defaultMenu = popup.getMenu().findItem(R.id.menu_default);
            if(type.equals(BluetoothDeviceListing.HID_FIDO_HOST) && bluetoothDeviceListing.isHidDefaultDevice(device)) {
                defaultMenu.setEnabled(false);
                defaultMenu.setTitle("Is default");
            } else if(type.equals(BluetoothDeviceListing.HID_KEYBOARD_HOST)) {
                defaultMenu.setVisible(false);
            } else {
                defaultMenu.setEnabled(true);
            }

            popup.setOnMenuItemClickListener(item -> {
                popupClick(item, device);
                return true;
            });

            popup.show();
        }


        private void popupClick(MenuItem item, BluetoothDeviceWrapper device) {

            PasswdSafeUtil.dbginfo(TAG, "Clicked: " + device.getName());

            int itemId = item.getItemId();
            if (itemId == R.id.menu_default) {
                PasswdSafeUtil.dbginfo(TAG, "Paired device menu: clicked set default");
                if(!rvPairedDevices.isComputingLayout()) {
                    bluetoothDeviceListing.cacheHidDefaultDevice(device.getDevice());
                }

            } else if (itemId == R.id.menu_unpair) {
                PasswdSafeUtil.dbginfo(TAG, "Paired device menu: clicked unpair");
                AlertDialog.Builder alert = new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirm))
                    .setMessage(getString(R.string.bt_unpair_info))
                    .setPositiveButton(R.string.confirm,
                        (dialog, whichButton) -> {
                            try {
                                BluetoothUtils.removeBond(device.getDevice());
                            } catch (NoSuchMethodException e) {
                                PasswdSafeUtil.showErrorMsg(getString(R.string.bt_no_unpair), new ActContext(requireActivity()));
                            }
                        })
                    .setNegativeButton(R.string.cancel, null);
                alert.show();
            }
        }
    }

    public static class ViewHolderPairedDevice extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView address;
        private final TextView state;
        private final Button btnReconnect;
        private final ImageButton btnDeviceMenu;

        public ViewHolderPairedDevice(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
            state = itemView.findViewById(R.id.state);
            btnReconnect = itemView.findViewById(R.id.btn_reconnect);
            btnDeviceMenu = itemView.findViewById(R.id.device_menu);
        }
    }


    public class RvDiscoveredDevicesAdapter extends RecyclerView.Adapter<ViewHolderDiscoveredDevice> {

        final private ArrayList<BluetoothDeviceWrapper> devices;

        public RvDiscoveredDevicesAdapter(ArrayList<BluetoothDeviceWrapper> devices) {
            this.devices = devices;
        }

        @NonNull
        @Override
        public ViewHolderDiscoveredDevice onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_bluetooth_devices_scan, parent, false);
            return new ViewHolderDiscoveredDevice(view);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onBindViewHolder(ViewHolderDiscoveredDevice holder, int position)
        {
            BluetoothDeviceWrapper device = devices.get(position);
            int state = device.getDevice().getBondState();

            holder.name.setText(device.getName());
            holder.address.setText(device.getAddress());
            holder.state.setText(BluetoothUtils.parseBondState(state));

            holder.btnKeyboard.setOnClickListener(item -> {
                BluetoothForegroundService btService = ((PasswdSafe) requireActivity()).btService;
                if(btService != null) {
                    stopBluetoothDiscovery();
                    SystemClock.sleep(50);
                    btService.pairAsKeyboard(device.getDevice());

                    checkBtProfileStateHandler.postDelayed(() -> {
                        if(btService.isAppRegistered()) {
                            PasswdSafeUtil.dbginfo(TAG, "btService.isAppRegistered is TRUE");
                        } else {
                            PasswdSafeUtil.dbginfo(TAG, "btService.isAppRegistered is FALSE");
                            PasswdSafeUtil.showErrorMsg(getString(R.string.bt_unclean_state_error), new ActContext(requireContext()));
                        }
                    }, CHECK_APP_REGISTRATION_TIMEOUT_MS);
                } else {
                    PasswdSafeUtil.showErrorMsg(getString(R.string.bt_pairing_no_service), new ActContext(requireActivity()));
                }
            });

            holder.btnFido.setEnabled(Preferences.getBluetoothFidoEnabled(prefs));

            holder.btnFido.setOnClickListener(item -> {
                BluetoothForegroundService btService = ((PasswdSafe) requireActivity()).btService;
                if(btService != null) {
                    stopBluetoothDiscovery();
                    SystemClock.sleep(50);
                    btService.pairAsFido(device.getDevice());

                    checkBtProfileStateHandler.postDelayed(() -> {
                        if(btService.isAppRegistered()) {
                            PasswdSafeUtil.dbginfo(TAG, "btService.isAppRegistered is TRUE");
                        } else {
                            PasswdSafeUtil.dbginfo(TAG, "btService.isAppRegistered is FALSE");
                            PasswdSafeUtil.showErrorMsg(getString(R.string.bt_unclean_state_error), new ActContext(requireContext()));
                        }
                    }, CHECK_APP_REGISTRATION_TIMEOUT_MS);
                } else {
                    PasswdSafeUtil.showErrorMsg(getString(R.string.bt_pairing_no_service), new ActContext(requireActivity()));
                }
            });
        }

        @Override
        public int getItemCount(){
            return devices.size();
        }

    }

    public static class ViewHolderDiscoveredDevice extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView address;
        private final TextView state;
        private final Button btnKeyboard;
        private final Button btnFido;

        public ViewHolderDiscoveredDevice(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
            state = itemView.findViewById(R.id.state);

            btnKeyboard = itemView.findViewById(R.id.btn_keyboard);
            btnFido = itemView.findViewById(R.id.btn_fido);
        }
    }


    protected final BroadcastReceiver btStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                checkBluetoothState(state);
                clearAvailableDevices();
            } else if(action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR);
                checkDeviceState(state);
            }
        }
    };


    /** Handles bluetooth scan responses and other indicators. */
    protected final BroadcastReceiver btScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getContext() == null) {
                PasswdSafeUtil.dbginfo("BluetoothScanReceiver", "BluetoothScanReceiver context disappeared");
                return;
            }

            final String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            switch (action == null ? "" : action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    PasswdSafeUtil.dbginfo("BluetoothScanReceiver", "BluetoothAdapter.ACTION_DISCOVERY_STARTED");
                    discoveryStarted();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    PasswdSafeUtil.dbginfo("BluetoothScanReceiver", "BluetoothAdapter.ACTION_DISCOVERY_FINISHED");
                    discoveryStopped();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    PasswdSafeUtil.dbginfo("BluetoothScanReceiver", "BluetoothDevice.ACTION_FOUND: " + device.getName());
                    if (HidDeviceProfile.isProfileSupported(device)) {
                        addAvailableDevice(device);
                    }
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    PasswdSafeUtil.dbginfo("BluetoothScanReceiver", "BluetoothDevice.ACTION_BOND_STATE_CHANGED");
                    updateAvailableDevice(device);

                    pairedDevices.clear();
                    pairedDevices.addAll(bluetoothDeviceListing.getAvailableDevices());

                    tvNoPairedDevices.setVisibility(pairedDevices.isEmpty() ? View.VISIBLE : View.GONE);

                    rvPairedDevicesAdapter.notifyDataSetChanged();

                    break;
                case BluetoothDevice.ACTION_NAME_CHANGED:
                    PasswdSafeUtil.dbginfo("BluetoothScanReceiver", "BluetoothDevice.ACTION_NAME_CHANGED");
                    break;
                default: // fall out
            }
        }
    };


    final private ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (!result.containsValue(false)) {
                    PasswdSafeUtil.dbginfo(TAG, "RequestMultiplePermissions: granted");
                    btScanToggle.setEnabled(true);

                    tvScanDescription.setText(getString(R.string.bt_scan_description));
                    btRequestProgress.setVisibility(View.GONE);
                    btAppSettings.setVisibility(View.GONE);

                    IntentFilter btStatusIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                    requireContext().registerReceiver(btStateReceiver, btStatusIntentFilter);
                    registerScanReceiver();
                    checkBluetoothState(null);

                    initRecyclerViews();
                } else {
                    PasswdSafeUtil.dbginfo(TAG, "RequestMultiplePermissions: missing permissions - scan disabled");
                    btScanToggle.setEnabled(false);

                    tvScanDescription.setText(R.string.bt_no_scan_permission_desc);
                    btRequestProgress.setVisibility(View.VISIBLE);
                    btAppSettings.setVisibility(View.VISIBLE);
                }
            }
    );

}
