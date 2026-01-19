package com.example.lifeguard;

import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class SyncedContactsActivity extends BaseActivity {

    RecyclerView recyclerView;
    ContactsAdapter adapter;
    List<Contact> contacts;

    DatabaseReference contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synced_contacts);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Synced Contacts");
        setupNavBar();

        recyclerView = findViewById(R.id.recyclerSyncedContacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        contacts = new ArrayList<>();
        adapter = new ContactsAdapter(this, contacts, null); // FirebaseRef not needed here
        recyclerView.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            contactsRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(user.getUid())
                    .child("EmergencyContacts");

            loadSyncedContacts();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }


        loadSyncedContacts();
    }

    private void loadSyncedContacts() {
        contactsRef.get().addOnSuccessListener(snapshot -> {
            contacts.clear();
            for (DataSnapshot ds : snapshot.getChildren()) {
                Contact c = ds.getValue(Contact.class);
                if (c != null) contacts.add(c);
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load synced contacts", Toast.LENGTH_SHORT).show());



    }
}
