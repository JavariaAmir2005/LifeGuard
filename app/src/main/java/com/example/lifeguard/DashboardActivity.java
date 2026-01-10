package com.example.lifeguard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;


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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    MaterialButton  btnSOS, btnHistory;
    SpeechRecognizer speechRecognizer;
    Intent speechIntent;
    boolean isListening = false;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initVoiceSOS();


        btnSOS = findViewById(R.id.btnSOS);
        btnHistory = findViewById(R.id.btnHistory);


        btnSOS.setOnClickListener(v ->
              startSOS());

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));


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
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private void initVoiceSOS() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    300
            );
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
        );

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override public void onReadyForSpeech(Bundle params) {}
                    @Override public void onBeginningOfSpeech() {}
                    @Override public void onRmsChanged(float rmsdB) {}
                    @Override public void onBufferReceived(byte[] buffer) {}
                    @Override public void onEndOfSpeech() {}

                    @Override
                    public void onError(int error) {
                        restartListening();
                    }

                    @Override
                    public void onResults(Bundle results) {
                        ArrayList<String> words =
                                results.getStringArrayList(
                                        SpeechRecognizer.RESULTS_RECOGNITION);

                        if (words != null) {
                            for (String word : words) {
                                word = word.toLowerCase();

                                if (word.contains("sos")
                                        || word.contains("help")
                                        || word.contains("emergency")) {

                                    Toast.makeText(
                                            DashboardActivity.this,
                                            "Voice SOS detected!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    startSOS(); // 🔥 SAME SOS METHOD
                                    break;
                                }
                            }
                        }
                        restartListening();
                    }

                    @Override public void onPartialResults(Bundle partialResults) {}
                    @Override public void onEvent(int eventType, Bundle params) {}
                }
        );

        startListening();
    }

    private void startSOS() {

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    101
            );
            return;
        }

        LocationManager lm =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        lm.requestSingleUpdate(
                LocationManager.GPS_PROVIDER,
                location -> handleSOS(location),
                null
        );
    }
    private String getTime() {

        return new SimpleDateFormat(
                "dd-MM-yyyy HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }
    private String getAddress(double lat, double lon) {

        try {
            Geocoder geocoder =
                    new Geocoder(this, Locale.getDefault());

            List<Address> addresses =
                    geocoder.getFromLocation(lat, lon, 1);

            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Address not found";
    }

    private void handleSOS(Location location) {

        double lat = location.getLatitude();
        double lon = location.getLongitude();

        String address = getAddress(lat, lon);
        String time = getTime();

        saveSOS(lat, lon, address, time);
        sendSMS("🚨 SOS!\n" + address);


        Toast.makeText(this,
                "SOS sent successfully",
                Toast.LENGTH_LONG).show();
    }
    private void saveSOS(double lat, double lon, String address, String time) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference sosRef =
                FirebaseDatabase.getInstance()
                        .getReference("SOS")
                        .child(user.getUid());


        sosRef.child("latitude").setValue(lat);
        sosRef.child("longitude").setValue(lon);
        sosRef.child("address").setValue(address);
        sosRef.child("time").setValue(time);


    }
    private void sendSMS(String message) {

        if (checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.SEND_SMS},
                    200
            );
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference contactRef =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(user.getUid())
                        .child("emergencyContacts");

        SmsManager smsManager = SmsManager.getDefault();

        contactRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            Toast.makeText(DashboardActivity.this,
                                    "No emergency contacts found",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (DataSnapshot s : snapshot.getChildren()) {
                            String phone =
                                    s.child("phone").getValue(String.class);

                            if (phone != null)
                                smsManager.sendTextMessage(
                                        phone,
                                        null,
                                        message,
                                        null,
                                        null
                                );
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                }
        );
    }

}
