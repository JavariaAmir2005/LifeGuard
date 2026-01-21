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
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends BaseActivity implements SensorEventListener {
    MaterialButton btnSOS;
    SensorManager sensorManager;
    Sensor accelerometer;
    final float SHAKE_THRESHOLD = 2.7f;
    long lastShakeTime = 0;

    private ArrayList<String> emergencyNumbers = new ArrayList<>();
    private int currentCallIndex = 0;
    private boolean someoneAnswered = false;
    private boolean isSOSRunning = false;

    TelephonyManager telephonyManager;
    PhoneStateListener phoneStateListener;
    Handler callTimeoutHandler = new Handler();
    Runnable callTimeoutRunnable;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        setupNavBar();

        btnSOS = findViewById(R.id.btnSOS);
        btnSOS.setOnClickListener(v -> startSOS());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float gForce = (float) Math.sqrt(
                Math.pow(event.values[0] / SensorManager.GRAVITY_EARTH, 2) +
                        Math.pow(event.values[1] / SensorManager.GRAVITY_EARTH, 2) +
                        Math.pow(event.values[2] / SensorManager.GRAVITY_EARTH, 2));

        if (gForce > SHAKE_THRESHOLD && System.currentTimeMillis() - lastShakeTime > 1000) {
            lastShakeTime = System.currentTimeMillis();
            startSOS();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void startSOS() {
        // Reset previous SOS state
        callTimeoutHandler.removeCallbacksAndMessages(null);
        isSOSRunning = true;
        someoneAnswered = false;
        currentCallIndex = 0;

        // Request location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Handler timeoutHandler = new Handler();
        LocationListener listener = location -> {
            timeoutHandler.removeCallbacksAndMessages(null);
            handleSOS(location);
        };
        timeoutHandler.postDelayed(() -> handleSOS(null), 8000);

        try {
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
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
        sendSMSIntent(message);
        loadContactsAndCall();
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

    private void sendSMSIntent(String message) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid()).child("EmergencyContacts");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot s : snapshot.getChildren()) {
                    String phone = s.child("phone").getValue(String.class);
                    if (phone != null)
                        android.telephony.SmsManager.getDefault().sendTextMessage(phone, null, message, null, null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadContactsAndCall() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid()).child("EmergencyContacts");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                emergencyNumbers.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    String phone = s.child("phone").getValue(String.class);
                    if (phone != null) emergencyNumbers.add(phone);
                }

                if (!emergencyNumbers.isEmpty()) {
                    setupCallListener();
                    callNextContact();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void callNextContact() {
        if (!isSOSRunning || someoneAnswered || currentCallIndex >= emergencyNumbers.size())
            return;

        String number = emergencyNumbers.get(currentCallIndex);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 201);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);

        callTimeoutRunnable = () -> {
            if (!someoneAnswered) {
                currentCallIndex++;
                callNextContact();
            }
        };
        callTimeoutHandler.postDelayed(callTimeoutRunnable, 25000); // 25s timeout
    }

    private void setupCallListener() {
        if (phoneStateListener != null) return;

        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    someoneAnswered = true;
                    isSOSRunning = false;
                    callTimeoutHandler.removeCallbacks(callTimeoutRunnable);
                }

                if (state == TelephonyManager.CALL_STATE_IDLE && !someoneAnswered) {
                    callTimeoutHandler.removeCallbacks(callTimeoutRunnable);
                    currentCallIndex++;
                    callNextContact();
                }
            }
        };

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == 201 || requestCode == 101) && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callNextContact();
        }
    }
}
