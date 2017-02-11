package ru.com.rh.rhlocator.location_utils;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Timer;
import java.util.TimerTask;

import ru.com.rh.rhlocator.Constants;
import ru.com.rh.rhlocator.R;

public class LocationIntentServiceWithGoogleApi extends LocationIntentService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int LOCATION_INTERVAL = 1000;

    private FusedLocationProviderApi mFusedLocationProviderApi;
    private GoogleApiClient mGoogleApiClient;

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation != null) {
            getCityName(mLocation);
        } else {
            LocationRequest mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(LOCATION_INTERVAL);
            mLocationRequest.setFastestInterval(LOCATION_INTERVAL);
            mLocationRequest.setNumUpdates(1);
            mFusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mTimer = new Timer();
            mTimer.schedule(new GetLocation(), TIME_OUT);
        }
        } catch (SecurityException e) {
            Log.e(TAG, getString(R.string.perm_probl), e);
        }
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.i(TAG, result.toString());
        Toast.makeText(this, result.toString(),Toast.LENGTH_SHORT).show();
        getCityName(mLocation);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            mLocation = location;
            mTimer.cancel();
            mFusedLocationProviderApi.removeLocationUpdates(mGoogleApiClient, this);
            getCityName(mLocation);
        }
    }

    protected void getLocation() {
        mFusedLocationProviderApi = LocationServices.FusedLocationApi;
        buildGoogleApiClient();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    private class GetLocation extends TimerTask {
        @Override
        public void run() {
            try {

            mFusedLocationProviderApi.removeLocationUpdates(mGoogleApiClient, LocationIntentServiceWithGoogleApi.this);
            mLocation = mFusedLocationProviderApi.getLastLocation(mGoogleApiClient);
            getCityName(mLocation);
            } catch (SecurityException e) {
                Log.e(TAG, getString(R.string.perm_probl), e);
            }
        }
    }

    protected void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }

}
