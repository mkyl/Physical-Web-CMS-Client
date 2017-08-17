package org.physical_web.cms.beaconviewer;

import java.util.List;

/**
 * Created by Kayali on 2017-08-17.
 */

public interface BeaconListener {
    void onScanComplete(List<SeenBeacon> beaconList);
}
