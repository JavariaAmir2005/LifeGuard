package com.example.lifeguard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    EditText etEmail, etPassword, etName;
    TextView LoginText;
    FirebaseAuth mAuth;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        LoginText = findViewById(R.id.LoginText);
        mAuth = FirebaseAuth.getInstance();

        final boolean[] isPasswordVisible = {false};
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >=
                        (etPassword.getRight()
                                - etPassword.getCompoundDrawables()[DRAWABLE_END]
                                .getBounds().width())) {

                    if (isPasswordVisible[0]) {
                        // Hide password
                        etPassword.setInputType(
                                InputType.TYPE_CLASS_TEXT |
                                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                        );
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                0, 0, R.drawable.ic_eye_close, 0
                        );
                        isPasswordVisible[0] = false;
                    } else {
                        // Show password
                        etPassword.setInputType(
                                InputType.TYPE_CLASS_TEXT |
                                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        );
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                0, 0, R.drawable.ic_eye_open, 0
                        );
                        isPasswordVisible[0] = true;
                    }

                    // Keep cursor at end
                    etPassword.setSelection(etPassword.getText().length());
                    return true;
                }
            }
            return false;
        });
        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Name is required");
                etName.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Password is required");
                etPassword.requestFocus();
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Password must be at least 6 characters");
                etPassword.requestFocus();
                return;
            }
            
            // CREATE USER
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            // ✅ Only now get UID
                            String uid = mAuth.getCurrentUser().getUid();

                            // ✅ Save name & email to Firebase
                            DatabaseReference ref = FirebaseDatabase.getInstance()
                                    .getReference("Users")
                                    .child(uid);
                            ref.child("name").setValue(name);
                            ref.child("email").setValue(email);

                            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();

                        } else {
                            String error = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Registration failed";

                            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        LoginText.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }
}
