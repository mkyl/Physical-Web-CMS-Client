package org.physical_web.cms.beaconviewer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Kayali on 2017-08-17.
 */

public class BeaconScanner implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = BeaconScanner.class.getSimpleName();
    private static final long SCAN_PERIOD = 3000; // milliseconds to scan for
    private static final long SCAN_INTERVAL = 20000; // milliseconds to wait between scans

    private static final ParcelUuid eddystoneURLIdentifier = ParcelUuid
            .fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    private static final int EDDYSTONE_URL_FRAME_ID = 0x10;

    private BluetoothAdapter bluetoothAdapter;
    private BeaconListener listener;
    private List<SeenBeacon> seenBeacons;
    private Handler scanHandler;
    private Handler delayHandler;
    private Boolean stopScanning;

    public BeaconScanner(Context context, BeaconListener beaconListener) {
        final BluetoothManager bluetoothManager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        listener = beaconListener;
        seenBeacons = new ArrayList<>();
        scanHandler = new Handler();
        delayHandler = new Handler();
    }

    /**
     * Returns true if system has a bluetooth device and that device is enabled in software
     */
    public Boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void startListening() {
        stopScanning = false;
        scanHandler();
    }

    public void stopListening() {
        stopScanning = true;
    }

    private void scanHandler() {
        if (stopScanning)
            return;

        clearRSSIRecords();

        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(BeaconScanner.this);
            }
        }, SCAN_PERIOD);

        // start LE scan must be passed an array of UUIDs, we're interested in just one really
        UUID[] relevantUUIDs = new UUID[1];
        relevantUUIDs[0] = eddystoneURLIdentifier.getUuid();

        bluetoothAdapter.startLeScan(relevantUUIDs, this);

        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // return list of seen beacons
                listener.onScanComplete(seenBeacons);
                // restart scan
                scanHandler();
            }
        }, SCAN_INTERVAL);
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
        if (listener != null) {
            processURLPacket(bytes);

            SeenBeacon oldBeaconRecord = searchForOldBeaconRecord(bluetoothDevice);
            if (oldBeaconRecord == null) {
                SeenBeacon newlySeen = new SeenBeacon();
                newlySeen.device = bluetoothDevice;
                newlySeen.rssi = rssi;
                seenBeacons.add(newlySeen);
            } else {
                oldBeaconRecord.rssi = rssi;
            }
        }
    }

    private void clearRSSIRecords() {
        for (SeenBeacon seenBeacon : seenBeacons) {
            seenBeacon.rssi = -451;
        }
    }

    private SeenBeacon searchForOldBeaconRecord(BluetoothDevice device) {
        for (SeenBeacon seenBeacon : seenBeacons) {
            if (seenBeacon.device.equals(device))
                return seenBeacon;
        }
        return null;
    }

    private void processURLPacket(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (b == EDDYSTONE_URL_FRAME_ID) {
                String url = "https://" + new String(Arrays.copyOfRange(bytes, i + 3, bytes.length))
                        .replaceAll("\\s+","");
                listener.onFoundExhibitURI(Uri.parse(url));
            }
        }
    }
}

class SeenBeacon {
    BluetoothDevice device;
    int rssi;
}
