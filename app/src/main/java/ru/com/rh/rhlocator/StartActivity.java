package ru.com.rh.rhlocator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;

import ru.com.rh.rhlocator.data.Contract;
import ru.com.rh.rhlocator.data.SQLHelper;
import ru.com.rh.rhlocator.location_utils.LocationIntentServiceWithGoogleApi;
import ru.com.rh.rhlocator.location_utils.LocationIntentServiceWithLocationManager;

public class StartActivity extends AppCompatActivity {

    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";
    protected static final String IS_CREATE_KEY = "is-create";
    private static final String TAG = "StartActivity";

    private SQLHelper mSQLHelper;
    private TextView mCityTW;
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
        mSQLHelper = new SQLHelper(this);

        mListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getSupportFragmentManager();
                ItemFragment dialogFragment = new ItemFragment();
                Bundle bundle = new Bundle();
                String[] array = getStringsFromDB();
                bundle.putStringArray(Constants.CITIES_ARRAY_DATA_KEY, array);
                dialogFragment.setArguments(bundle);
                dialogFragment.show(manager, "dialog");
            }
        });

        mAddressRequested = false;
        mAddressOutput = "";

        updateValuesFromBundle(savedInstanceState);
        updateUIWidgets();

        checkPermission();
    }

    //begin checking permission methods
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
                retrieveLocation();
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
    //end checking permission methods


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(IS_CREATE_KEY, isRetrieveLocation);
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }

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
                insert(mAddressOutput);
            }

            mAddressRequested = false;
            updateUIWidgets();
        }
    }

    private void startIntentService() {
        Intent intent;
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
                && GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE >= 10010000) {
            intent = new Intent(this, LocationIntentServiceWithGoogleApi.class);
        } else {
            intent = new Intent(this, LocationIntentServiceWithLocationManager.class);
        }
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        startService(intent);
    }

    private void retrieveLocation() {
        if (!isRetrieveLocation) {
            isRetrieveLocation = true;

            startIntentService();
            mAddressRequested = true;
            updateUIWidgets();
        }
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

    protected void displayAddressOutput() {
        mCityTW.setText(mAddressOutput);
    }

    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void insert(String s) {
        SQLiteDatabase db = mSQLHelper.getWritableDatabase();

        ContentValues value = new ContentValues();
        value.put(Contract.COLUMN_CITY_NAME, s);

        long newRowId = db.insert(Contract.TABLE_NAME, null, value);

        if (newRowId == -1) {
            Log.d(TAG, "=(");
        } else {
            Log.d(TAG, "=)");
        }
    }

    String[] getStringsFromDB() {
        SQLiteDatabase db = mSQLHelper.getReadableDatabase();

        ArrayList<String> list = new ArrayList<String>();

        String query = "SELECT * FROM " + Contract.TABLE_NAME;
        Cursor cursor  = db.rawQuery(query, null);
        try {
            while (cursor.moveToNext()) {
                String note = cursor.getString(cursor.getColumnIndex(Contract.COLUMN_CITY_NAME));
                list.add(note);
            }
        } finally {
        cursor.close();
        }
        return castToString(list.toArray());
    }

    String[] castToString(Object[] objects) {
        if (objects == null || objects.length == 0) return null;
        String[] result = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            result[i] = (String)objects[i];
        }
        return result;
    }
}
