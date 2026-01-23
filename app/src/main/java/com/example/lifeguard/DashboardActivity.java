package com.example.lifeguard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.telephony.SmsManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
public  class DashboardActivity extends BaseActivity implements SensorEventListener {

    MaterialButton btnSOS;


    SensorManager sensorManager;
    Sensor accelerometer;
    final float SHAKE_THRESHOLD = 3.7f; // adjust for sensitivity
    long lastShakeTime = 0;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Setup bottom navigation
        setupNavBar();

        // SOS button
        btnSOS = findViewById(R.id.btnSOS);
        btnSOS.setOnClickListener(v -> {
            Toast.makeText(this, "SOS pressed! Attempting to get location...", Toast.LENGTH_SHORT).show();
            startSOS();
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;

        if (accelerometer != null) {
            Toast.makeText(this, "Device has an accelerometer!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No accelerometer detected.", Toast.LENGTH_LONG).show();
        }

    }

    // ---------------- Shake detection ----------------
    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime > 1000) { // 1 sec cooldown
                lastShakeTime = now;
                Toast.makeText(this, "Shake detected! Sending SOS...", Toast.LENGTH_SHORT).show();
                startSOS();
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void startSOS() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Toast.makeText(this, "Getting location...", Toast.LENGTH_SHORT).show();

        Handler timeoutHandler = new Handler();

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                lm.removeUpdates(this);
                timeoutHandler.removeCallbacksAndMessages(null);
                handleSOS(location); // ✅ ALWAYS fires now
            }
        };

        // Timeout (fallback)
        timeoutHandler.postDelayed(() -> {
            lm.removeUpdates(listener);
            handleSOS(null);
        }, 8000);

        try {
            if (net) {
                lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0,
                        0,
                        listener
                );
            }

            if (gps) {
                lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0,
                        0,
                        listener
                );
            }

        } catch (Exception e) {
            handleSOS(null);
        }
    }


    private void handleSOS(Location location) {
        String message;
        double lat = 0, lon = 0;

        if (location != null) {
            lat = location.getLatitude();
            lon = location.getLongitude();
            message = "🚨 SOS!\nMy location:\nhttps://maps.google.com/?q="
                    + lat + "," + lon;

        } else {
            message = "🚨 SOS!\nLocation unavailable!";
        }

        saveSOS(location);
        sendSMSIntent(message);
    }

    private void saveSOS(Location location) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference sosRef = FirebaseDatabase.getInstance().getReference("SOS").child(user.getUid());

        String time = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        if (location != null) {
            sosRef.child("latitude").setValue(location.getLatitude());
            sosRef.child("longitude").setValue(location.getLongitude());
        } else {
            sosRef.child("latitude").setValue(null);
            sosRef.child("longitude").setValue(null);
        }
        sosRef.child("time").setValue(time);
    }

    private void sendSMSIntent(String message) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 102);
            return;
        }

        DatabaseReference contactRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid())
                .child("EmergencyContacts");

        contactRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(DashboardActivity.this, "No emergency contacts found", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot s : snapshot.getChildren()) {
                    String phone = s.child("phone").getValue(String.class);
                    if (phone != null && !phone.isEmpty()) {
                        try {
                            SmsManager.getDefault().sendTextMessage(phone, null, message, null, null);
                        } catch (Exception e) {
                            Toast.makeText(DashboardActivity.this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                Toast.makeText(DashboardActivity.this, "SOS sent!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Failed to read contacts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSOS();
        }


        if (requestCode == 102 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission granted, press SOS again", Toast.LENGTH_SHORT).show();
        }
    }

}
