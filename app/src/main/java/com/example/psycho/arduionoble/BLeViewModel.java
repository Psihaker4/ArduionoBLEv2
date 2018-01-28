package com.example.psycho.arduionoble;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BLeViewModel extends AndroidViewModel {

    private static final int SCAN_PERIOD = 1000;
    private static final String SERVICE_UUID="00000000-0000-0000-0000-000000000010";
    private static final String CLIENT_UUID="00002902-0000-1000-8000-00805f9b34fb";
    private static final String CONTROL_UUID="00000000-0000-0000-0000-000000000001";
    private static final String SENSORS_UUID = "00000000-0000-0000-0000-000000000002";

    private boolean scanning;
    private boolean connected;

    private Context context;

    private MutableLiveData<List<BluetoothDevice>> devices = new MutableLiveData<>();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner bluetoothLeScanner;

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic controls;
    private BluetoothGattCharacteristic sensors;

    private List<BluetoothDevice> scanResults;
    private ScanSettings scanSettings;
    private List<ScanFilter> scanFilters;
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            add(result);
            Log.d("BLeViewModel", "onScanResult: NEW");
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) add(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d("BLeViewModel", "onScanFailed: " + errorCode);
        }

        private void add(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            scanResults.add(device);
        }
    };

    private BluetoothGattCallback gattClientCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status != BluetoothGatt.GATT_SUCCESS){
                Log.d("BLeViewModel", "onConnectionStateChange: DIS");
                return;
            }

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d("BLeViewModel", "onConnectionStateChange: CONNECT");
                    connected = true;
                    gatt.discoverServices();
                    return;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d("BLeViewModel", "onConnectionStateChange: DISCONNECT");
                    connected2.postValue(false);
                    disconnect();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }
            Log.d("BLeViewModel", "onServicesDiscovered: ");
            BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
            controls = service.getCharacteristic(UUID.fromString(CONTROL_UUID));
            sensors = service.getCharacteristic(UUID.fromString(SENSORS_UUID));

            gatt.setCharacteristicNotification(sensors, true);

            BluetoothGattDescriptor descriptor = sensors.getDescriptor(
                    UUID.fromString(CLIENT_UUID));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
            data = new ArrayList<>();
            connected2.postValue(true);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d("BLeViewModel", "onCharacteristicChanged: "+characteristic.getStringValue(0));
            String s = characteristic.getStringValue(0);

            float[] d = new float[]{
                    Float.parseFloat(s.substring(0,5))/100,
                    Float.parseFloat(s.substring(5,10))/100,
                    Float.parseFloat(s.substring(10,15))/100,
                    Float.parseFloat(s.substring(15,20))/1000,
            };
            Log.d("BLeViewModel", "onCharacteristicChanged: "+ Arrays.toString(d));
            data.add(d);
            Log.d("BLeViewModel", "onCharacteristicChanged: "+data.size());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d("BLeViewModel", "onCharacteristicRead: "+characteristic.getUuid());
        }
    };

    private List<float[]> data;

    public BLeViewModel(Application application) {
        super(application);
        context = application.getBaseContext();
        bluetoothManager = (BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.d("BLeViewModel", "configure: FAILED");
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanFilters = new ArrayList<>();

        Log.d("BLeViewModel", "configure: SUC");
        startScan();
    }

    public MutableLiveData<List<BluetoothDevice>> getDevices() {
        return devices;
    }

    public void choose(BluetoothDevice device){
        connect(device);
    }

    private boolean checkBluetooth() {
        Log.d("BLeViewModel", "checkBluetooth: ");
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return false;
        }

        return true;
    }

    private void startScan() {
        Log.d("BLeViewModel", "startScan: ");
        if (!checkBluetooth() || scanning) return;

        scanResults = new ArrayList<>();
        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);

        scanning = true;

        new Handler().postDelayed(this::stopScan, SCAN_PERIOD);
        Log.d("BLeViewModel", "startScan: @@@");
    }

    private void stopScan() {
        Log.d("BLeViewModel", "stopScan: "+bluetoothLeScanner+" "+scanning+" "+checkBluetooth());
        if (scanning && checkBluetooth() && bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
            scanComplete();
            Log.d("BLeViewModel", "stopScan: COMPLETE");
        }

        scanning = false;
    }

    private void scanComplete() {
        devices.setValue(scanResults);
    }

    private void connect(BluetoothDevice device) {
        Log.d("BLeViewModel", "connect: ");
        gatt = device.connectGatt(context, false, gattClientCallback);
    }

    private void disconnect(){
        Log.d("BLeViewModel", "disconnect: ");
        connected = false;
        if(gatt == null) return;
        gatt.disconnect();
        gatt.close();
    }

    public void send(int data) {
        Log.d("BLeViewModel", "send: ");
        if (!connected) return;
        controls.setValue(data, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        while(!gatt.writeCharacteristic(controls)){
            Log.d("BLeViewModel", "sending: ");
        }
        Log.d("BLeViewModel", "sends: COMPLETE");

    }

    MutableLiveData<Boolean> connected2 = new MutableLiveData<>();

    public MutableLiveData<Boolean> getConnected2() {
        return connected2;
    }
}