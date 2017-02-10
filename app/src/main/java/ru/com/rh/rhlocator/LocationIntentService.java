package ru.com.rh.rhlocator;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class LocationIntentService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "FetchAddressIS";
    private static final int LOCATION_INTERVAL = 5000;
    private static final long TIME_OUT = 20 * 1000;

    private Timer mTimer;
    private ResultReceiver mReceiver;
    private FusedLocationProviderApi mFusedLocationProviderApi;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;

    public LocationIntentService() {
        super(TAG);
    }

    protected synchronized void buildGoogleApiClient() {
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
            Log.e(TAG,"SecurityException", e);
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

    private void getLocation() {
        mFusedLocationProviderApi = LocationServices.FusedLocationApi;
        buildGoogleApiClient();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

        if (mReceiver == null) {
            Log.wtf(TAG, "No receiver received. There is nowhere to send the results.");
            return;
        }
        getLocation();
    }

    private void getCityName(Location location) {
        String errorMessage = "";

        if (location == null) {
            errorMessage = getString(R.string.no_location_data_provided);
            Log.wtf(TAG, errorMessage);
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
        } catch (IOException ioException) {
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), illegalArgumentException);
        }

        if (addresses == null || addresses.size() == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            String addressFragments = address.getLocality();
            Log.i(TAG, getString(R.string.address_found));
            deliverResultToReceiver(Constants.SUCCESS_RESULT, addressFragments);
        }
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }

    class GetLocation extends TimerTask {
        @Override
        public void run() {
            try {

            mFusedLocationProviderApi.removeLocationUpdates(mGoogleApiClient, LocationIntentService.this);
            mLocation = mFusedLocationProviderApi.getLastLocation(mGoogleApiClient);
            getCityName(mLocation);
            } catch (SecurityException e) {
                Log.e(TAG,"SecurityException", e);
            }
        }
    }

}
