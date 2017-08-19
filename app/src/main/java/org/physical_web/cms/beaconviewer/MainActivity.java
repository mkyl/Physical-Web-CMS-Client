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
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BeaconListener{
    BeaconScanner beaconScanner;
    Uri baseExhibitURI = null;

    TextView status;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconScanner = new BeaconScanner(this, this);

        status = (TextView) findViewById(R.id.status);
        webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
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
    public void onScanStarted() {
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
    }

    @Override
    public void onScanComplete(List<SeenBeacon> beaconList) {
        findViewById(R.id.progress).setVisibility(View.GONE);

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
            status.setVisibility(View.VISIBLE);
            status.setText(R.string.intro_text);
        } else {
            if (baseExhibitURI != null) {
                String contentURI = baseExhibitURI + "/" + closestBeacon.device.getAddress()
                        .replaceAll(":", "-").toLowerCase();
                Log.d("MainActivity", "Loading uri: " + contentURI);
                webView.loadUrl(contentURI);
                status.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onFoundExhibitURI(Uri uri) {
        if (baseExhibitURI == null) {
            if (uri.toString().contains("goo.gl"))
                expandAndSetURI(uri);
            else
                baseExhibitURI = uri;
        }
    }

    private void expandAndSetURI(Uri uri) {
        Log.d("MainActivity", "Expanding URI: " + uri.toString());
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this, new HurlStack() {
            @Override
            protected HttpURLConnection createConnection(URL url) throws IOException {
                HttpURLConnection connection = super.createConnection(url);
                connection.setInstanceFollowRedirects(false);

                return connection;
            }
        });

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, uri.toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e("MainActivity", "Those code should never run");
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // 301 is an "error"
                Uri uri = Uri.parse(error.networkResponse.headers.get("Location"));
                Log.d("MainActivity", "Expanded uri: " + uri.toString());
                baseExhibitURI = uri;
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
}
