package com.example.lifeguard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends BaseActivity {

    TextView tvName, tvEmail, tvPhone, tvBloodGroup, tvAllergies, tvMedicines, tvMedicalNotes;
    Button btnLogout, btnEditProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("My Profile");

        setupNavBar();

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvBloodGroup = findViewById(R.id.tvBloodGroup);
        tvAllergies = findViewById(R.id.tvAllergies);
        tvMedicines = findViewById(R.id.tvMedicines);
        tvMedicalNotes = findViewById(R.id.tvMedicalNotes);
        btnLogout = findViewById(R.id.btnLogout);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);// return to login screen
            startActivity(intent);
        });

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));

        fetchUserInfo();
    }

    private void fetchUserInfo() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ProfileActivity.this,
                            "User info not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                User userProfile = snapshot.getValue(User.class);
                if (userProfile != null) {
                    tvName.setText("Name: " + (userProfile.getName() != null ? userProfile.getName() : "--"));
                    tvEmail.setText("Email: " + (userProfile.getEmail() != null ? userProfile.getEmail() : "--"));
                    tvPhone.setText("Phone: " + (userProfile.getPhone() != null ? userProfile.getPhone() : "--"));
                    tvBloodGroup.setText("Blood group: " + (userProfile.getBloodGroup() != null ? userProfile.getBloodGroup() : "--"));
                    tvAllergies.setText("Allergies & Reactions: " + (userProfile.getAllergies() != null ? userProfile.getAllergies() : "None"));
                    tvMedicines.setText("Medicines: " + (userProfile.getMedicines() != null ? userProfile.getMedicines() : "None"));
                    tvMedicalNotes.setText("Medical Notes: " + (userProfile.getMedicalNotes() != null ? userProfile.getMedicalNotes() : "None"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this,
                        "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
