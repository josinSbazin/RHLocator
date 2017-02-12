package ru.com.rh.rhlocator.location_utils;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;

import ru.com.rh.rhlocator.Constants;
import ru.com.rh.rhlocator.R;


public abstract class LocationIntentService extends IntentService {
    static final String TAG = "FetchAddressIS";
    static final long TIME_OUT = 5 * 1000;

    ResultReceiver mReceiver;
    Location mLocation;
    Timer mTimer;


    public LocationIntentService() {
        super(TAG);
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

    void getCityName(Location location) {
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
            //TODO Сделать в AsynkTask
            String address = GeocoderByHttpAndJsonForGenymotion.getCurrentLocationViaJSON(location.getLatitude(), location.getLongitude());
            if (address != null && !address.equals(""))
                deliverResultToReceiver(Constants.SUCCESS_RESULT, address);
            else {
                deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            }
        } else {
            Address address = addresses.get(0);
            String addressFragments = address.getLocality();
            Log.i(TAG, getString(R.string.address_found));
            deliverResultToReceiver(Constants.SUCCESS_RESULT, addressFragments);
        }
    }

    protected abstract void getLocation();

    protected abstract void deliverResultToReceiver(int resultCode, String message);
}
