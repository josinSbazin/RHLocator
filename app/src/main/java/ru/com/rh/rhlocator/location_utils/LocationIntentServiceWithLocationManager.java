package ru.com.rh.rhlocator.location_utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import ru.com.rh.rhlocator.Constants;
import ru.com.rh.rhlocator.R;

public class LocationIntentServiceWithLocationManager extends LocationIntentService {
    private LocationManager lm;
    private boolean gpsEnabled;
    private boolean networkEnabled;

    @Override
    protected void getLocation() {
            if (lm == null)
                lm = (LocationManager) getApplicationContext()
                        .getSystemService(Context.LOCATION_SERVICE);

            try {
                gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (!gpsEnabled && !networkEnabled) {
                    getCityName(null);
                } else {
                    if (gpsEnabled)
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);

                    if (networkEnabled)
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);

                    mTimer = new Timer();
                    mTimer.schedule(new GetLocation(), TIME_OUT);
                }
            } catch (SecurityException e) {
                Log.e(TAG, getString(R.string.perm_probl), e);
            }
    }

    LocationListener locationListenerGps = new LocationListener() {
        public void onLocationChanged(Location location) {
            mTimer.cancel();
            try {
            lm.removeUpdates(this);
            lm.removeUpdates(locationListenerNetwork);
            } catch (SecurityException e) {Log.e(TAG, getString(R.string.perm_probl), e);}
            getCityName(location);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            mTimer.cancel();
            try {
                lm.removeUpdates(this);
                lm.removeUpdates(locationListenerGps);
            } catch (SecurityException e) {Log.e(TAG, getString(R.string.perm_probl), e);}
            getCityName(location);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    @Override
    protected void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }

    class GetLocation extends TimerTask {
        @Override
        public void run() {
            try {
                lm.removeUpdates(locationListenerGps);
                lm.removeUpdates(locationListenerNetwork);
            }catch (SecurityException e) {Log.e(TAG, getString(R.string.perm_probl), e);}

            Location netLoc = null, gpsLoc = null;
            if(gpsEnabled)
                try {
                    gpsLoc=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } catch (SecurityException e) {Log.e(TAG, getString(R.string.perm_probl), e);}
            if(networkEnabled)
                try {
                netLoc=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch (SecurityException e) {Log.e(TAG, getString(R.string.perm_probl), e);}

            if(gpsLoc!=null && netLoc!=null){
                if(gpsLoc.getTime()>netLoc.getTime())
                    getCityName(gpsLoc);
                else
                    getCityName(netLoc);
                return;
            }

            if(gpsLoc!=null){
                getCityName(gpsLoc);
                return;
            }
            if(netLoc!=null){
                getCityName(netLoc);
                return;
            }
            getCityName(null);
        }
    }
}
