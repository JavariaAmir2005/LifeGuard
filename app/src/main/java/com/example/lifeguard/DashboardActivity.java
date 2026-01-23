package com.example.lifeguard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

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

public class DashboardActivity extends BaseActivity implements SensorEventListener {
    MaterialButton btnSOS;
    // Shake Detection
    SensorManager sensorManager;
    Sensor accelerometer;
    final float SHAKE_THRESHOLD = 2.7f;
    long lastShakeTime = 0;

    // Sequential Calling
    ArrayList<String> callNumbers = new ArrayList<>();
    int currentCallIndex = 0;
    TelephonyManager telephonyManager;
    PhoneStateListener phoneStateListener;

    boolean isCallAnswered = false;
    boolean hasListenerRegistered = false; // Fix for listener crash

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        setupNavBar();

        btnSOS = findViewById(R.id.btnSOS);
        btnSOS.setOnClickListener(v -> {
            Toast.makeText(this, "SOS pressed! Attempting to get location...", Toast.LENGTH_SHORT).show();
            startSOS();
        });

        // Shake
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;

        // Telephony
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    }

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
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0], y = event.values[1], z = event.values[2];
        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime > 1000) {
                lastShakeTime = now;
                Toast.makeText(this, "Shake detected! Sending SOS...", Toast.LENGTH_SHORT).show();
                startSOS();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ---------------- SOS ----------------
    private void startSOS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Handler timeoutHandler = new Handler();
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                timeoutHandler.removeCallbacksAndMessages(null);
                handleSOS(location);
                if (ActivityCompat.checkSelfPermission(DashboardActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    lm.removeUpdates(this);
            }
        };

        timeoutHandler.postDelayed(() -> {
            handleSOS(null);
            if (ActivityCompat.checkSelfPermission(DashboardActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                lm.removeUpdates(listener);
        }, 8000);

        try {
            if (net) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
            if (gps) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        } catch (Exception e) {
            handleSOS(null);
        }
    }

    private void handleSOS(Location location) {
        String message;
        if (location != null) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            message = "🚨 SOS!\nMy location:\nhttps://maps.google.com/?q=" + lat + "," + lon;
        } else {
            message = "🚨 SOS!\nLocation unavailable!";
        }

        saveSOS(location);
        sendSMS(message);
        startSequentialCalling();
    }

    private void saveSOS(Location location) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference sosRef = FirebaseDatabase.getInstance().getReference("SOS").child(user.getUid());
        String time = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        sosRef.child("latitude").setValue(location != null ? location.getLatitude() : null);
        sosRef.child("longitude").setValue(location != null ? location.getLongitude() : null);
        sosRef.child("time").setValue(time);
    }

    private void sendSMS(String message) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 102);
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid()).child("EmergencyContacts");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot s : snapshot.getChildren()) {
                    String phone = s.child("phone").getValue(String.class);
                    if (phone != null)
                        SmsManager.getDefault().sendTextMessage(phone, null, message, null, null);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ---------------- Sequential Calling ----------------
    private void startSequentialCalling() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid()).child("EmergencyContacts");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                callNumbers.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    String phone = s.child("phone").getValue(String.class);
                    if (phone != null) callNumbers.add(phone);
                }

                if (!callNumbers.isEmpty()) {
                    currentCallIndex = 0;
                    isCallAnswered = false;
                    registerCallListenerOnce();
                    callNextNumber();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void callNextNumber() {
        if (currentCallIndex >= callNumbers.size()) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 201);
            return;
        }

        String number = callNumbers.get(currentCallIndex);
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }

    private void registerCallListenerOnce() {
        if (hasListenerRegistered) return;

        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {

                if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    isCallAnswered = true;
                }

                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    if (!isCallAnswered) {
                        currentCallIndex++;
                        callNextNumber();
                    } else {
                        // SOS completed
                        currentCallIndex = callNumbers.size();
                        isCallAnswered = false;
                    }
                }
            }
        };

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        hasListenerRegistered = true;
    }

    // ---------------- Permissions ----------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            startSOS();
        if (requestCode == 102 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show();
        if (requestCode == 201 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            callNextNumber();
    }
}