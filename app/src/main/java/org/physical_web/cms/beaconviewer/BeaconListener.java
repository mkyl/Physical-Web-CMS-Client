package org.physical_web.cms.beaconviewer;

import android.net.Uri;

import java.util.List;

/**
 * Created by Kayali on 2017-08-17.
 */

public interface BeaconListener {
    void onScanComplete(List<SeenBeacon> beaconList);
    void onFoundExhibitURI(Uri uri);
    void onScanStarted();
}
