package ru.com.rh.rhlocator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

public class StartActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener {

    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";
    protected static final String IS_CREATE_KEY = "is-create";

    private TextView mCityTW;
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    protected boolean mAddressRequested;
    protected String mAddressOutput;
    private AddressResultReceiver mResultReceiver;
    ProgressBar mProgressBar;
    Button mListButton;
    private boolean isRetrieveLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mResultReceiver = new AddressResultReceiver(new Handler());
        mCityTW = (TextView) findViewById(R.id.cityTW);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mListButton = (Button) findViewById(R.id.listButton);

        mAddressRequested = false;
        mAddressOutput = "";

        updateValuesFromBundle(savedInstanceState);
        updateUIWidgets();
        buildGoogleApiClient();

        checkPermission();
    }

    //start for checking permission methods
    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(StartActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(StartActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            retrieveLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mAddressOutput = "";
                recreate();
            }
            else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(StartActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)){
                    askPermissionRationale();
                } else goToNotWorkedActivity();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void askPermissionRationale() {
        new AlertDialog.Builder(StartActivity.this)
                .setMessage(getString(R.string.permission_tip))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(StartActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    }
                }).setCancelable(false).show();
    }

    private void goToNotWorkedActivity() {
        Intent i = new Intent(StartActivity.this, NotWorkedActivity.class);
        finish();
        startActivity(i);
    }
    //end for checking permission methods

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(IS_CREATE_KEY)) {
                isRetrieveLocation = savedInstanceState.getBoolean(IS_CREATE_KEY);
            }
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                if (!Geocoder.isPresent()) {
                    Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                    return;
                }
                if (mAddressRequested) {
                    startIntentService();
                }
            } else {
                somethingWrong();
            }
        } catch (SecurityException e) {
            checkPermission();
        }
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        Parcer par = new Parcer(mResultReceiver, mLastLocation);
        intent.putExtra(Constants.RECEIVER_AND_LOCATION_DATA, par);

        startService(intent);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        somethingWrong();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    protected void displayAddressOutput() {
        mCityTW.setText(mAddressOutput);
    }


    private void updateUIWidgets() {
        if (mAddressRequested) {
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            mListButton.setEnabled(false);
        } else {
            mProgressBar.setVisibility(ProgressBar.GONE);
            mListButton.setEnabled(true);
        }
    }

    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putBoolean(IS_CREATE_KEY, isRetrieveLocation);

        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);

        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }

    class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();

            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }

            mAddressRequested = false;
            updateUIWidgets();
        }
    }

    private void retrieveLocation() {
        if (!isRetrieveLocation) {
            if (mGoogleApiClient.isConnected() && mLastLocation != null) {
                startIntentService();
            }
            mAddressRequested = true;
            updateUIWidgets();
            isRetrieveLocation = true;
        }
    }

    private void somethingWrong() {
        mAddressOutput = getString(R.string.service_not_available);
        displayAddressOutput();
        mAddressRequested = false;
        updateUIWidgets();
    }
}
