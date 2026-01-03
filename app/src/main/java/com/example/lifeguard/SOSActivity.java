package com.example.lifeguard;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SOSActivity extends AppCompatActivity {

    Button btnSOS;
    DatabaseHelper dbHelper;
    DatabaseReference historyRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sosactivity);

        btnSOS = findViewById(R.id.btnSOS);
        dbHelper = new DatabaseHelper(this);
        historyRef = FirebaseDatabase.getInstance().getReference("EmergencyHistory");

        btnSOS.setOnClickListener(v -> {
            long id = dbHelper.addHistory("SOS Triggered",
                    String.valueOf(System.currentTimeMillis()));

            History h = new History((int) id, "SOS Triggered",
                    String.valueOf(System.currentTimeMillis()));

            // Firebase sync
            historyRef.child(String.valueOf(id)).setValue(h);

            Toast.makeText(this, "SOS Triggered & Saved", Toast.LENGTH_SHORT).show();
        });
    }
}
