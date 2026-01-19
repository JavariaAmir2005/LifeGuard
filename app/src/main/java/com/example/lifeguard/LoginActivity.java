package com.example.lifeguard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    TextView btnRegister;
    FirebaseAuth mAuth;

    @SuppressLint({"WrongViewCast", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnRegister = findViewById(R.id.LoginText);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        mAuth = FirebaseAuth.getInstance();

        /* ---------------- PASSWORD SHOW / HIDE LOGIC ---------------- */

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

        /* ---------------- LOGIN BUTTON ---------------- */

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

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

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        } else {
                            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        /* ---------------- REGISTER TEXT ---------------- */
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }
}
