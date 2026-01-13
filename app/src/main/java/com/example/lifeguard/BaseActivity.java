package com.example.lifeguard;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.lifeguard.DashboardActivity;
import com.example.lifeguard.ProfileActivity;
import com.example.lifeguard.ContactsActivity;
import com.example.lifeguard.LocationActivity;
public class BaseActivity extends AppCompatActivity {

    protected void setupNavBar() {
        BottomNavigationView navView = findViewById(R.id.bottomNavigation);
        if (navView != null) {
                // Highlight current activity
            if (this instanceof DashboardActivity)
                navView.setSelectedItemId(R.id.nav_home);
            else if (this instanceof ProfileActivity)
                navView.setSelectedItemId(R.id.nav_profile);
            else if (this instanceof ContactsActivity)
                navView.setSelectedItemId(R.id.nav_contacts);
            else if (this instanceof LocationActivity)
                navView.setSelectedItemId(R.id.nav_location);

            navView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home && !(this instanceof DashboardActivity)) {
                    startActivity(new Intent(this, DashboardActivity.class));
                } else if (id == R.id.nav_profile && !(this instanceof ProfileActivity)) {
                    startActivity(new Intent(this, ProfileActivity.class));
                } else if (id == R.id.nav_contacts && !(this instanceof ContactsActivity)) {
                    startActivity(new Intent(this, ContactsActivity.class));
                } else if (id == R.id.nav_location && !(this instanceof LocationActivity)) {
                    startActivity(new Intent(this, LocationActivity.class));
                }
                return true;
            });

        }
    }
}
