package com.example.lifeguard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

public class DashboardActivity extends BaseActivity {

    MaterialButton btnSOS;
    SpeechRecognizer speechRecognizer;
    RecognitionListener recognitionListener;
    android.content.Intent speechIntent;
    boolean isListening = false;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Setup bottom navigation
        setupNavBar();

        // Initialize voice-activated SOS
        initVoiceSOS();

        // SOS button
        btnSOS = findViewById(R.id.btnSOS);
        btnSOS.setOnClickListener(v -> {
            Toast.makeText(this, "SOS pressed! Attempting to get location...", Toast.LENGTH_SHORT).show();
            startSOS();
        });
    }

    private void startListening() {
        if (!isListening) {
            isListening = true;
            speechRecognizer.startListening(speechIntent);
        }
    }

    private void restartListening() {
        isListening = false;
        startListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }

    private void initVoiceSOS() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 300);
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) { restartListening(); }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> words = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (words != null) {
                    for (String word : words) {
                        if (word.toLowerCase().contains("sos") ||
                                word.toLowerCase().contains("help") ||
                                word.toLowerCase().contains("emergency")) {
                            Toast.makeText(DashboardActivity.this, "Voice SOS detected!", Toast.LENGTH_SHORT).show();
                            startSOS();
                            break;
                        }
                    }
                }
                restartListening();
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        startListening();
    }

    private void startSOS() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            Location lastLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastLocation != null) {
                handleSOS(lastLocation); // Send immediately if available
            } else {
                Toast.makeText(this, "Fetching GPS location, will send SOS shortly...", Toast.LENGTH_SHORT).show();
                // Send SOS with null location after 5 seconds if GPS not available
                new android.os.Handler().postDelayed(() -> handleSOS(null), 5000);
            }

            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, location -> {
                if (location != null) {
                    handleSOS(location); // Override null if GPS found later
                }
            }, null);

        } catch (SecurityException e) {
            e.printStackTrace();
            handleSOS(null);
        }


    }

    private void handleSOS(Location location) {
        String message;
        double lat = 0, lon = 0;

        if (location != null) {
            lat = location.getLatitude();
            lon = location.getLongitude();
            message = "🚨 SOS!\nMy location:\nLat: " + lat + ", Lng: " + lon;
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

        if (requestCode == 300 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }

        if (requestCode == 102 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission granted, press SOS again", Toast.LENGTH_SHORT).show();
        }
    }
}
