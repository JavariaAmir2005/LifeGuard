package com.example.lifeguard;

import android.content.Intent;
import android.telephony.SmsManager;

import android.Manifest;
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

import android.speech.SpeechRecognizer;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LocationActivity extends AppCompatActivity {
    private static final int SMS_REQ = 200;
    FirebaseAuth auth;
    DatabaseReference contactRef;
    SpeechRecognizer speechRecognizer;
    Intent speechIntent;
    private static final int REQ = 100;

    TextView tvLat, tvLon, tvAddr;
    LocationManager lm;
    AlertDialog loadingDialog;
    Handler timeoutHandler = new Handler();

    DatabaseReference sosRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        tvLat = findViewById(R.id.tvLat);
        tvLon = findViewById(R.id.tvLon);
        tvAddr = findViewById(R.id.tvAddr);

        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        sosRef = FirebaseDatabase.getInstance()
                .getReference("SOS")
                .child(user.getUid()); // Replace with FirebaseAuth UID later

        findViewById(R.id.btnGet).setOnClickListener(v -> getLocation());


        if (user != null) {
            contactRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("emergencyContacts");
        }

    }

    private void showLoading() {
        if (loadingDialog == null) {
            ProgressBar pb = new ProgressBar(this);
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setView(pb);
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

        timeoutHandler.postDelayed(() -> {
            hideLoading();
            lm.removeUpdates(listener);
            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
        }, 8000);

        try {
            if (net)
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 10, listener);

            if (gps)
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, listener);

        } catch (Exception e) {
            hideLoading();
            Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show();
        }
    }

    private final LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            showLocation(location);
            triggerSOS(location);
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
        } catch (Exception e) {
            tvAddr.setText("Geocoder error");
        }
        sendSMS(
                "SOS! I need help.\n" +
                        tvAddr.getText().toString()
        );

    }

    private void triggerSOS(Location loc) {
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();
        String addressText = "Address not found";

        Geocoder g = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> list = g.getFromLocation(lat, lon, 1);
            if (list != null && !list.isEmpty()) {
                addressText = list.get(0).getAddressLine(0);
            }
        } catch (Exception ignored) {}

        String time = new SimpleDateFormat(
                "dd-MM-yyyy HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        if (sosRef == null) return;

        sosRef.child("latitude").setValue(lat);
        sosRef.child("longitude").setValue(lon);
        sosRef.child("address").setValue(addressText);
        sosRef.child("time").setValue(time);

        Toast.makeText(this, "🚨 SOS Sent Successfully!", Toast.LENGTH_LONG).show();
    }
    private boolean checkSmsPermission() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_REQ
            );
            return false;
        }
        return true;
    }
    private void sendSMS(String message) {

        if (!checkSmsPermission()) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();

        contactRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    Toast.makeText(LocationActivity.this,
                            "No emergency contacts found",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String phone = snap.child("phone").getValue(String.class);

                    if (phone != null && !phone.isEmpty()) {
                        smsManager.sendTextMessage(
                                phone,
                                null,
                                message,
                                null,
                                null
                        );
                    }
                }

                Toast.makeText(LocationActivity.this,
                        "SOS sent to emergency contacts",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LocationActivity.this,
                        "Failed to fetch contacts",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void startVoiceSOS() {

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> words =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                if (words != null) {
                    for (String word : words) {

                        word = word.toLowerCase();

                        if (word.contains("help") || word.contains("sos")) {

                            Toast.makeText(
                                    LocationActivity.this,
                                    "Voice SOS Activated",
                                    Toast.LENGTH_SHORT
                            ).show();

                            if (!checkSmsPermission()) {
                                Toast.makeText(LocationActivity.this, getString(R.string.sms_permission_needed_to_send_sos), Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Location lastLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (lastLoc == null) lastLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                            if (lastLoc != null) {
                                triggerSOS(lastLoc);
                            } else {
                                Toast.makeText(LocationActivity.this, "Location not available for voice SOS", Toast.LENGTH_SHORT).show();
                            }

                            break;
                        }
                    }
                }
            }

            public void onReadyForSpeech(Bundle params) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float rmsdB) {}
            public void onBufferReceived(byte[] buffer) {}
            public void onEndOfSpeech() {}
            public void onError(int error) {}
            public void onPartialResults(Bundle partialResults) {}
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(speechIntent);
    }


    @Override

    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == REQ &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        }

        if (requestCode == SMS_REQ &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this,
                    "SMS permission granted",
                    Toast.LENGTH_SHORT).show();
        }
    }

}
