package com.example.lifeguard;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Ensure Firebase is initialized
        FirebaseApp.initializeApp(this);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser(); // null if no user is signed in

        // Splash animation
        ImageView logo = findViewById(R.id.logo);
        TextView title = findViewById(R.id.title);
        Animation fade = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(fade);
        title.startAnimation(fade);


        // Delay navigation by 3 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (user != null) {
                intent = new Intent(SplashActivity.this, DashboardActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 3000);
    }
}

