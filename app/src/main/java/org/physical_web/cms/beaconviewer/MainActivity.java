package org.physical_web.cms.beaconviewer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements BeaconListener{
    BeaconScanner beaconScanner;
    Uri baseExhibitURI = null;

    TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconScanner = new BeaconScanner(this, this);

        status = (TextView) findViewById(R.id.status);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPermissions();
        beaconScanner.startListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        beaconScanner.stopListening();
    }

    private void getPermissions() {
        if (!beaconScanner.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }

        int locationPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (locationPermissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 42);
        }
    }

    @Override
    public void onScanComplete(List<SeenBeacon> beaconList) {
        SeenBeacon closestBeacon = null;
        int maxRSSI = -451;
        for (SeenBeacon seenBeacon : beaconList) {
            if (seenBeacon.rssi > maxRSSI) {
                maxRSSI = seenBeacon.rssi;
                closestBeacon = seenBeacon;
            }
        }

        if (closestBeacon == null) {
            // no beacons seen
            status.setText("No beacons around");
        } else {
            status.setText("Closest beacon: " + closestBeacon.device.getAddress());
        }
    }

    @Override
    public void onFoundExhibitURI(Uri uri) {
        if (baseExhibitURI == null) {
            baseExhibitURI = uri;
        }
    }
}
