package com.example.lifeguard;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

public class LocationActivity extends AppCompatActivity {

    private static final int REQ = 100;

    TextView tvLat, tvLon, tvAddr;
    LocationManager lm;
    AlertDialog loadingDialog;

    Handler timeoutHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        tvLat = findViewById(R.id.tvLat);
        tvLon = findViewById(R.id.tvLon);
        tvAddr = findViewById(R.id.tvAddr);

        lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        findViewById(R.id.btnGet).setOnClickListener(v -> getLocation());
    }

    private void showLoading() {
        if (loadingDialog == null) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setView(new ProgressBar(this));
            b.setCancelable(false);
            loadingDialog = b.create();
        }
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void getLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ);
            return;
        }

        boolean gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gps && !net) {
            Toast.makeText(this, "Turn ON GPS or Internet", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading();

        // Timeout after 8 sec
        timeoutHandler.postDelayed(() -> {
            hideLoading();
            lm.removeUpdates(listener);
            Toast.makeText(this, "No location found", Toast.LENGTH_SHORT).show();
        }, 8000);

        try {
            if (net)
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);

            if (gps)
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);

        } catch (Exception e) {
            hideLoading();
            Toast.makeText(this, "Error requesting location", Toast.LENGTH_SHORT).show();
        }
    }

    private final LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location loc) {
            showLocation(loc);
            hideLoading();
            lm.removeUpdates(this);
            timeoutHandler.removeCallbacksAndMessages(null);
        }
    };

    private void showLocation(Location loc) {
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();

        tvLat.setText("Latitude: " + lat);
        tvLon.setText("Longitude: " + lon);

        Geocoder g = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> list = g.getFromLocation(lat, lon, 1);
            if (list != null && !list.isEmpty()) {
                tvAddr.setText("Address: " + list.get(0).getAddressLine(0));
            } else {
                tvAddr.setText("Address not found");
            }
            // ✅ Pass address to next activity (CardView screen)
            Intent intent = new Intent(LocationActivity.this, MainActivity.class);
            String address = "address:";
            intent.putExtra("user_address", address);
            startActivity(intent);
        } catch (Exception e) {
            tvAddr.setText("Geocoder error");
        }

    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(code, p, r);
        if (code == REQ && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        }
    }
}
