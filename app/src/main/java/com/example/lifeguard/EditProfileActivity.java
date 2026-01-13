package com.example.lifeguard;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditProfileActivity extends BaseActivity {

    EditText etName, etEmail, etPhone, etBloodGroup, etAllergies, etMedicines, etMedicalNotes;
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Edit Profile");

        setupNavBar();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etBloodGroup = findViewById(R.id.etBloodGroup);
        etAllergies = findViewById(R.id.etAllergies);
        etMedicines = findViewById(R.id.etMedicines);
        etMedicalNotes = findViewById(R.id.etMedicalNotes);
        btnSave = findViewById(R.id.btnSave);

        loadProfile();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                User userProfile = snapshot.getValue(User.class);
                if (userProfile != null) {
                    etName.setText(userProfile.getName() != null ? userProfile.getName() : "");
                    etEmail.setText(userProfile.getEmail() != null ? userProfile.getEmail() : "");
                    etPhone.setText(userProfile.getPhone() != null ? userProfile.getPhone() : "");
                    etBloodGroup.setText(userProfile.getBloodGroup() != null ? userProfile.getBloodGroup() : "");
                    etAllergies.setText(userProfile.getAllergies() != null ? userProfile.getAllergies() : "");
                    etMedicines.setText(userProfile.getMedicines() != null ? userProfile.getMedicines() : "");
                    etMedicalNotes.setText(userProfile.getMedicalNotes() != null ? userProfile.getMedicalNotes() : "");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid());

        User updatedUser = new User(
                etName.getText().toString().trim(),
                etEmail.getText().toString().trim(),
                etPhone.getText().toString().trim(),
                etBloodGroup.getText().toString().trim(),
                etAllergies.getText().toString().trim(),
                etMedicines.getText().toString().trim(),
                etMedicalNotes.getText().toString().trim()
        );

        ref.setValue(updatedUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(EditProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
