package ru.com.rh.rhlocator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            getLocation();
//            Toast toast = Toast.makeText(this, "All good", Toast.LENGTH_SHORT);
//            toast.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)){
                    askPermissionRationale();
                } else goToNotWorkedActivity();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void askPermissionRationale() {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(getString(R.string.permission_tip))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    }
                }).show();
    }

    private void goToNotWorkedActivity() {
        Intent i = new Intent(MainActivity.this, NotWorkedActivity.class);
        finish();
        startActivity(i);
    }

    private void getLocation(){
        Toast toast = Toast.makeText(this, "All good", Toast.LENGTH_SHORT);
        toast.show();
    }
}
