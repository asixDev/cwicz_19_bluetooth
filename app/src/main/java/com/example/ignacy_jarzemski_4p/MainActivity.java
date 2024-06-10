package com.example.ignacy_jarzemski_4p;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MY_BLUETOOTH_APP";
    private static final int BLUETOOTH_CONNECT_REQ_PERM_OFF = 8880;
    private static final int BLUETOOTH_CONNECT_REQ_PERM_ON = 8881;
    private static final int BLUETOOTH_SCAN_REQ_PERM_NEW = 8882;
    private static final int BLUETOOTH_ADVERTISE_REQ_PERM_TURN = 8883;
    private BluetoothAdapter bluetoothAdapter;
    private ActivityResultLauncher<Intent> bluetoothStartLauncher;
    ActivityResultLauncher<Intent> discoverableLauncher;
    private final SimpleDateFormat logFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private boolean hasBluetooth = true;
    private TextView title, newDevices, macDisplay, textLog;
    private Button scanButt, visibilityButt;
    private ListView listPaired, listNew;
    private ScrollView main;
    private ArrayAdapter<String> arrayAdapterPaired, arrayAdapterNew;
    ArrayList<String> arrayListPaired, arrayListNew;
    private final BroadcastReceiver myStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        bluetoothOn();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        bluetoothOff();
                        break;
                }
            }
        }
    };
    private final BroadcastReceiver myDiscoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    if (deviceName == null) deviceName = "UNAVAILABLE NAME";
                    String deviceAddress = device.getAddress();
                    if (deviceAddress == null) deviceAddress = "UNAVAILABLE ADDRESS";
                    Log.v(TAG, "deviceName=" + deviceName + " deviceAddress=" + deviceAddress);
                    arrayListNew.add(deviceName + "\n" + deviceAddress);
                    scanButt.setText(getString(R.string.scan_in_progress, arrayListNew.size()));
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                arrayListNew.clear();
                scanButt.setEnabled(false);
                scanButt.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.disabled));
                scanButt.setText(getString(R.string.scan_in_progress, arrayListNew.size()));
                Toast.makeText(getApplicationContext(), "Started new device scan...", Toast.LENGTH_SHORT).show();
                arrayAdapterNew.notifyDataSetChanged();
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                scanButt.setEnabled(true);
                scanButt.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.tooth_blue));
                scanButt.setText(R.string.scan_devices);
                Toast.makeText(getApplicationContext(), "Finished new device scan...", Toast.LENGTH_SHORT).show();
                arrayAdapterNew.notifyDataSetChanged();
                unregisterReceiver(this);
            }
        }
    };

    private final BroadcastReceiver myDiscoveryStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)){
                final String currentTimeStamp = logFormat.format(new Date());
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    textLog.append("\n".concat("received start discoverable @").concat(currentTimeStamp));
                    main.scrollToDescendant(textLog);
                } else {
                    textLog.append("\n".concat("received stop discoverable @").concat(currentTimeStamp));
                    main.scrollToDescendant(textLog);
                    unregisterReceiver(this);
                }
            }
        }
    };

    private View.OnTouchListener listViewScrollHandler = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            main.requestDisallowInterceptTouchEvent(true);
            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_UP:
                    main.requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        }
    };

    private void bluetoothOn() {
        title.setBackgroundResource(R.color.active);
        newDevices.setBackgroundResource(R.color.active);
        scanButt.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.tooth_blue));
        scanButt.setEnabled(true);
        visibilityButt.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.tooth_blue));
        visibilityButt.setEnabled(true);
    }

    private void bluetoothOff() {
        title.setBackgroundResource(R.color.inactive);
        newDevices.setBackgroundResource(R.color.inactive);
        scanButt.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.tooth_blue_off));
        scanButt.setEnabled(false);
        visibilityButt.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.tooth_blue_off));
        visibilityButt.setEnabled(false);
        Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        bluetoothStartLauncher.launch(bluetoothIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main = findViewById(R.id.main);

        title = findViewById(R.id.app_title);
        newDevices = findViewById(R.id.new_title);
        macDisplay = findViewById(R.id.mac_display);
        textLog = findViewById(R.id.text_log);

        scanButt = findViewById(R.id.scan_button);
        visibilityButt = findViewById(R.id.visibility_button);

        listPaired = findViewById(R.id.paired_list);
        listPaired.setOnTouchListener(listViewScrollHandler);
        listNew = findViewById(R.id.new_list);
        listNew.setOnTouchListener(listViewScrollHandler);

        arrayListPaired = new ArrayList<>();
        arrayAdapterPaired = new ArrayAdapter<>(this, R.layout.list_view_items, R.id.item_content, arrayListPaired);
        listPaired.setAdapter(arrayAdapterPaired);

        arrayListNew = new ArrayList<>();
        arrayAdapterNew = new ArrayAdapter<>(this, R.layout.list_view_items, R.id.item_content, arrayListNew);
        listNew.setAdapter(arrayAdapterNew);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            hasBluetooth = false;
            Toast.makeText(this, "DEVICE DOESN'T HAVE BLUETOOTH", Toast.LENGTH_LONG).show();
        }else{
            bluetoothStartLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == MainActivity.RESULT_OK) bluetoothOn();
                        }
                    }
            );
            if (bluetoothAdapter.isEnabled()){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothOn();
                } else {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_CONNECT_REQ_PERM_ON);
                }
            }else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothOff();
                } else {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_CONNECT_REQ_PERM_OFF);
                }
            }
            discoverableLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() != MainActivity.RESULT_CANCELED) {
                                textLog.append("\n".concat("visibility for ").concat(String.valueOf(result.getResultCode())).concat(" seconds @").concat(logFormat.format(new Date())));
                                main.scrollToDescendant(textLog);
                                Log.v(TAG, "discoverable launch");
                            }
                        }
                    }
            );
            scanButt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    scan();
                }
            });
            visibilityButt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    makeDiscoverable();
                }
            });
        }
    }

    private void makeDiscoverable() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            turnDiscoverable();
        } else {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_ADMIN}, BLUETOOTH_ADVERTISE_REQ_PERM_TURN);
        }
    }

    private void turnDiscoverable() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 15);
            IntentFilter stateReceiverFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            registerReceiver(myDiscoveryStateReceiver, stateReceiverFilter);
            if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
            discoverableLauncher.launch(discoverableIntent);
        }
    }

    private void scan() {
        arrayListPaired.clear();
        getPaired();
        arrayAdapterPaired.notifyDataSetChanged();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            getNew();
        } else {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADMIN}, BLUETOOTH_SCAN_REQ_PERM_NEW);
        }
    }

    private void getNew() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(myDiscoveryReceiver, filter);
            if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
            bluetoothAdapter.startDiscovery();
        }
    }

    private void getPaired() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED){
            macDisplay.setText(getString(R.string.my_mac_prefix).concat(" ").concat(bluetoothAdapter.getAddress()).concat("\n(can't get on modern android versions)"));
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (!pairedDevices.isEmpty()){
                for (BluetoothDevice pairedDevice : pairedDevices) {
                    String deviceName = pairedDevice.getName();
                    String deviceAddress = pairedDevice.getAddress();
                    Log.v(TAG, "deviceName=" + deviceName + " deviceAddress=" + deviceAddress);
                    arrayListPaired.add(deviceName + "\n" + deviceAddress);
                }
            } else {
                Log.v(TAG, "pairedDevices.size()=" + pairedDevices.size());
                arrayListPaired.add("no paired devices :c");
            }
        } else Toast.makeText(this, "ERROR - NO PERMISSION", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case BLUETOOTH_CONNECT_REQ_PERM_OFF:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    bluetoothOff();
                } else {
                    Toast.makeText(this, "BLUETOOTH PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;
            case BLUETOOTH_CONNECT_REQ_PERM_ON:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    bluetoothOn();
                } else {
                    Toast.makeText(this, "BLUETOOTH PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;
            case BLUETOOTH_SCAN_REQ_PERM_NEW:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getNew();
                } else {
                    Toast.makeText(this, "CAN'T GET NEW DEVICES WITHOUT PERMISSION", Toast.LENGTH_LONG).show();
                }
                break;
            case BLUETOOTH_ADVERTISE_REQ_PERM_TURN:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    turnDiscoverable();
                } else {
                    Toast.makeText(this, "NO PERMISSION", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        if (hasBluetooth) unregisterReceiver(myStateChangedReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (hasBluetooth) registerReceiver(this.myStateChangedReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.cancelDiscovery();
            unregisterReceiver(myDiscoveryReceiver);
            unregisterReceiver(myDiscoveryStateReceiver);
        }
        if (hasBluetooth) unregisterReceiver(myStateChangedReceiver);
        super.onDestroy();
    }
}